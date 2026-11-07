package DIV.gtcsolo.client.performance;

import DIV.gtcsolo.mixin.performance.MegaWorldPreviewAccess;
import DIV.gtcsolo.mixin.performance.OwnedBufferBuilderAccess;
import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded native tessellation/uploads; native in-world shaders, scale, filters and BERs stay in charge. */
public final class NativeWorldPreviewBatches implements AutoCloseable {
    private static final int POSITIONS_PER_STEP = 4096;
    private static final int POSITIONS_PER_NATIVE_CALL = 32;
    private static final int POSITIONS_PER_UPLOAD = 512;
    private final List<RenderType> layers = RenderType.chunkBufferLayers();
    private final List<List<VertexBuffer>> meshes = new ArrayList<>();
    private final List<Set<TextureAtlasSprite>> sprites = new ArrayList<>();
    private final VertexBuffer[] representatives = new VertexBuffer[layers.size()];
    private final Map<VertexBuffer, Integer> representativeLayers = new IdentityHashMap<>();
    private final Set<BlockPos> blockEntities = new HashSet<>();
    private final Set<BlockPos> readOnlyBlockEntities = Collections.unmodifiableSet(blockEntities);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final long totalPositions;
    private final PoseStack pose = new PoseStack();
    private final List<BlockPos> nextPositions = new ArrayList<>(POSITIONS_PER_NATIVE_CALL);
    private TrackedDummyWorld world;
    private Collection<BlockPos> positions;
    private Iterator<BlockPos> iterator;
    private BufferBuilder builder;
    private WorldSceneRenderer.VertexConsumerWrapper consumer;
    private boolean building;
    private int layerIndex, positionsInUpload;
    private long processedPositions;

    public NativeWorldPreviewBatches(TrackedDummyWorld world, Collection<BlockPos> positions) {
        RenderSystem.assertOnRenderThread();
        this.world = Objects.requireNonNull(world);
        this.positions = Objects.requireNonNull(positions);
        totalPositions = (long) positions.size() * layers.size();
        for (int i = 0; i < layers.size(); i++) {
            meshes.add(new ArrayList<>());
            sprites.add(Collections.newSetFromMap(new IdentityHashMap<>()));
        }
        iterator = positions.iterator();
    }

    /** Counts positions across render layers; checks the clock between calls of at most 32 positions. */
    public void step(long deadlineNanos) {
        if (closed.get() || done() || System.nanoTime() >= deadlineNanos) return;
        RenderSystem.assertOnRenderThread();
        TrackedDummyWorld activeWorld = world;
        var minecraft = Minecraft.getInstance();
        var dispatcher = minecraft.getBlockRenderer();
        int visited = 0;
        ModelBlockRenderer.enableCaching();
        try {
            while (!closed.get() && !done() && visited < POSITIONS_PER_STEP
                    && System.nanoTime() < deadlineNanos) {
                beginBuffer();
                if (!iterator.hasNext()) {
                    finishLayer();
                    continue;
                }
                nextPositions.clear();
                int quota = Math.min(POSITIONS_PER_NATIVE_CALL,
                        Math.min(POSITIONS_PER_STEP - visited, POSITIONS_PER_UPLOAD - positionsInUpload));
                while (nextPositions.size() < quota && iterator.hasNext()) {
                    nextPositions.add(iterator.next().immutable());
                }
                try (var capture = SpriteVisibility.capture(sprites.get(layerIndex))) {
                    MegaWorldPreviewAccess.gtcsolo$megaRenderBlocks(world, pose, dispatcher,
                            layers.get(layerIndex), consumer, nextPositions);
                }
                // Native prepares this set in a separate full scan. Piggyback only on the first layer.
                if (layerIndex == 0) {
                    for (BlockPos position : nextPositions) {
                        var entity = world.getBlockEntity(position);
                        if (entity != null && minecraft.getBlockEntityRenderDispatcher().getRenderer(entity) != null) {
                            blockEntities.add(position);
                        }
                    }
                }
                visited += nextPositions.size();
                processedPositions += nextPositions.size();
                positionsInUpload += nextPositions.size();
                nextPositions.clear();
                if (positionsInUpload == POSITIONS_PER_UPLOAD) upload(false);
            }
        } catch (RuntimeException | Error failure) {
            try { close(); }
            catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        } finally {
            try { activeWorld.setRenderFilter(position -> true); }
            finally { ModelBlockRenderer.clearCache(); }
        }
    }

    private void beginBuffer() {
        if (builder == null) {
            builder = new BufferBuilder(65_536);
            consumer = new WorldSceneRenderer.VertexConsumerWrapper(builder);
        }
        if (!building) {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            building = true;
        }
    }

    private void finishLayer() {
        // Even an empty translucent layer needs a valid BLOCK-format VBO, or native skips its BER pass.
        upload(meshes.get(layerIndex).isEmpty());
        layerIndex++;
        if (!done()) iterator = positions.iterator();
    }

    private void upload(boolean keepEmpty) {
        var rendered = builder.end();
        building = false;
        positionsInUpload = 0;
        if (rendered.isEmpty() && !keepEmpty) {
            rendered.release();
            return;
        }
        VertexBuffer buffer = null;
        boolean uploadOwnsRendered = false;
        try {
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            if (buffer.isInvalid()) throw new IllegalStateException("Cannot allocate native preview vertex buffer");
            buffer.bind();
            uploadOwnsRendered = true;
            // A valid native VertexBuffer releases RenderedBuffer in its own finally block.
            buffer.upload(rendered);
            meshes.get(layerIndex).add(buffer);
            if (representatives[layerIndex] == null) {
                representatives[layerIndex] = buffer;
                representativeLayers.put(buffer, layerIndex);
            }
        } catch (RuntimeException | Error failure) {
            if (buffer != null) {
                try { buffer.close(); }
                catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        } finally {
            try { if (!uploadOwnsRendered) rendered.release(); }
            finally { VertexBuffer.unbind(); }
        }
    }

    public boolean done() { return !closed.get() && layerIndex == layers.size(); }

    public int percent() {
        if (closed.get()) return 0;
        if (done()) return 100;
        return totalPositions == 0 ? 0 : (int) Math.min(99, processedPositions * 100 / totalPositions);
    }

    /** Borrowed, read-only array in native chunk-layer order; valid only after completion until close. */
    public VertexBuffer[] buffers() {
        if (!done()) throw new IllegalStateException("Native world preview is not ready");
        return representatives;
    }

    public Set<BlockPos> blockEntities() {
        if (!done()) throw new IllegalStateException("Native world preview is not ready");
        return readOnlyBlockEntities;
    }

    /** Native already applied this layer's shader/uniforms. False means the receiver belongs to native. */
    public boolean draw(VertexBuffer receiver) {
        Integer index = representativeLayers.get(receiver);
        if (index == null) return false;
        if (closed.get()) return true;
        RenderSystem.assertOnRenderThread();
        SpriteVisibility.markVisible(sprites.get(index));
        var layerMeshes = meshes.get(index);
        for (int i = 0; i < layerMeshes.size(); i++) {
            var buffer = layerMeshes.get(i);
            buffer.bind();
            buffer.draw();
        }
        return true;
    }

    /** Resolve the private native enum once; caller publishes LEVEL/BER/duration before this release write. */
    @SuppressWarnings("unchecked")
    public static void publishCompiledState() {
        AtomicReference<Object> state = (AtomicReference<Object>) MegaWorldPreviewAccess.gtcsolo$megaCacheState();
        state.set(NativeCompiledState.VALUE);
    }

    private static final class NativeCompiledState {
        static final Object VALUE = resolve();
        private static Object resolve() {
            Object current = MegaWorldPreviewAccess.gtcsolo$megaCacheState().get();
            if (current instanceof Enum<?> state) {
                for (Object value : state.getDeclaringClass().getEnumConstants()) {
                    if (((Enum<?>) value).name().equals("COMPILED")) return value;
                }
            }
            throw new IllegalStateException("Native preview CacheState.COMPILED is unavailable");
        }
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        if (RenderSystem.isOnRenderThread()) releaseRenderResources();
        else RenderSystem.recordRenderCall(this::releaseRenderResources);
    }

    private void releaseRenderResources() {
        RenderSystem.assertOnRenderThread();
        Throwable failure = null;
        for (var layer : meshes) {
            for (var buffer : layer) {
                try { buffer.close(); }
                catch (RuntimeException | Error cleanup) { failure = appendFailure(failure, cleanup); }
            }
            layer.clear();
        }
        for (var layerSprites : sprites) layerSprites.clear();
        blockEntities.clear();
        nextPositions.clear();
        if (builder != null && building) {
            try { builder.discard(); }
            catch (RuntimeException | Error cleanup) { failure = appendFailure(failure, cleanup); }
        }
        if (builder != null) {
            try {
                var storage = (OwnedBufferBuilderAccess) (Object) builder;
                var memory = storage.gtcsolo$getOwnedStorage();
                if (memory != null) {
                    // ModernFix's finalizer also frees this field if non-null. Detach first so it
                    // cannot free our owned allocation twice; discard/release only reset counters.
                    storage.gtcsolo$setOwnedStorage(null);
                    long address = MemoryUtil.memAddress0(memory);
                    if (address != 0) MemoryUtil.getAllocator(false).free(address);
                }
            } catch (RuntimeException | Error cleanup) { failure = appendFailure(failure, cleanup); }
        }
        builder = null;
        consumer = null;
        iterator = null;
        positions = null;
        world = null;
        building = false;
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

    private static Throwable appendFailure(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }
}
