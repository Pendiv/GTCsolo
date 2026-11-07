package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.client.performance.SpriteVisibility;
import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Small render-thread batches: no one-shot tessellation, worker races, or monolithic upload. */
final class MegaPreviewMesh implements AutoCloseable {
    private static final int BLOCKS_PER_UPLOAD = 1024;
    private final TrackedDummyWorld world;
    private final Collection<BlockPos> positions;
    private final List<RenderType> layers = RenderType.chunkBufferLayers();
    private final List<List<VertexBuffer>> meshes = new ArrayList<>();
    private final Set<TextureAtlasSprite> sprites = Collections.newSetFromMap(new IdentityHashMap<>());
    private final PoseStack pose = new PoseStack();
    private final RandomSource random = RandomSource.create();
    private BufferBuilder builder;
    private Iterator<BlockPos> iterator;
    private int layerIndex, batchBlocks, vertices;
    private long processed;

    MegaPreviewMesh(TrackedDummyWorld world, Collection<BlockPos> positions) {
        this.world = world; this.positions = positions;
        for (var ignored : layers) meshes.add(new ArrayList<>());
        iterator = positions.iterator();
    }

    void step(long deadline) {
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        int frameBlocks = 0;
        try (var capture = SpriteVisibility.capture(sprites)) {
            while (!done() && frameBlocks < 4096 && System.nanoTime() < deadline) {
                RenderType layer = layers.get(layerIndex);
                WorldSceneRenderer.setDefaultRenderLayerState(layer);
                if (builder == null) builder = new BufferBuilder(65536);
                if (batchBlocks == 0) builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                if (!iterator.hasNext()) {
                    upload();
                    layerIndex++;
                    if (!done()) iterator = positions.iterator();
                    continue;
                }
                BlockPos pos = iterator.next();
                var state = world.getBlockState(pos);
                if (!state.isAir() && WorldSceneRenderer.canRenderInLayer(dispatcher, state, pos, world, layer, random)) {
                    pose.pushPose();
                    try {
                        pose.translate(pos.getX(), pos.getY(), pos.getZ());
                        WorldSceneRenderer.renderBlocksForge(dispatcher, state, pos, world, pose, builder, random, layer);
                        vertices++;
                    } finally { pose.popPose(); }
                }
                processed++; batchBlocks++; frameBlocks++;
                if (batchBlocks >= BLOCKS_PER_UPLOAD) upload();
            }
        } finally { WorldSceneRenderer.setDefaultRenderLayerState(null); }
    }

    private void upload() {
        var rendered = builder.end();
        if (vertices > 0 && !rendered.isEmpty()) {
            var vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            try {
                vbo.bind(); vbo.upload(rendered);
                meshes.get(layerIndex).add(vbo);
            } catch (Throwable failure) { vbo.close(); throw failure; }
            finally { VertexBuffer.unbind(); }
        } else rendered.release();
        batchBlocks = 0; vertices = 0;
    }

    void draw() {
        SpriteVisibility.markVisible(sprites);
        for (int i = 0; i < layers.size(); i++) {
            var layer = layers.get(i);
            if (meshes.get(i).isEmpty()) continue;
            layer.setupRenderState();
            try {
                for (var mesh : meshes.get(i)) {
                    mesh.bind();
                    mesh.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                }
            } finally { VertexBuffer.unbind(); layer.clearRenderState(); }
        }
    }

    boolean done() { return layerIndex == layers.size(); }
    int percent() { return positions.isEmpty() ? 100 : (int) Math.min(100, processed * 100 / ((long) positions.size() * layers.size())); }
    @Override public void close() {
        for (var layer : meshes) { for (var mesh : layer) mesh.close(); layer.clear(); }
        sprites.clear();
        if (builder != null && batchBlocks > 0) builder.discard();
        builder = null; iterator = null;
    }
}
