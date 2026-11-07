package DIV.gtcsolo.client.performance;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.machine.mega.MegaBlastPattern;
import DIV.gtcsolo.machine.mega.MegaMixerPattern;
import DIV.gtcsolo.machine.mega.MegaPatternResource;
import DIV.gtcsolo.machine.orefactory.OreFactoryPattern;
import DIV.gtcsolo.mixin.performance.MegaWorldPreviewAccess;
import DIV.gtcsolo.mixin.performance.PatternWorldPreviewAccess;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Three exported mega layouts; standard controllers and copied layouts retain the native entry point. */
public final class MegaWorldPreview {
    private static final long FRAME_BUDGET = 8_000_000L;
    private static volatile Active active;
    private static volatile String lastFailure = "";
    private static volatile long requests;

    private MegaWorldPreview() {}

    /** Read on the client thread when a native worker's deferred completion is considered. */
    public static boolean isActive() { return active != null; }

    private enum Layout {
        MIXER("mega_mixer", 'T', false), BLAST("mega_alloy_blast_furnace", '%', true),
        ORE("ore_processing_factory", '7', false), FUSION("mega_fusion", '\0', false);
        final String id;
        final char port;
        final boolean coils;
        final MegaPatternResource resource;
        Layout(String id, char port, boolean coils) {
            this.id = id; this.port = port; this.coils = coils;
            resource = new MegaPatternResource(MegaWorldPreview.class, path(), id);
        }
        String path() { return "/data/gtcsolo/multiblocks/" + id + ".json.gz"; }
        TraceabilityPredicate ports() {
            return switch (this) {
                case MIXER -> MegaMixerPattern.previewPorts();
                case BLAST -> MegaBlastPattern.previewPorts();
                case ORE -> OreFactoryPattern.previewPorts();
                case FUSION -> com.gregtechceu.gtceu.api.pattern.Predicates.any();
            };
        }
        static Layout find(ResourceLocation id) {
            if (!id.getNamespace().equals(Gtcsolo.MODID)) return null;
            for (Layout layout : values()) if (layout.id.equals(id.getPath())) return layout;
            return null;
        }
    }

    /** True means the known giant entry was handled, including a controlled loading failure. */
    public static boolean tryShow(BlockPos origin, MultiblockControllerMachine controller, int duration) {
        if (!controller.getDefinition().isRenderWorldPreview()) return false;
        Layout layout = Layout.find(controller.getDefinition().getId());
        if (layout == null) return false;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return true;
        requests++;
        Active previous = active;
        int layer = previous != null && previous.origin.equals(origin) && previous.layout == layout
                && previous.sourceLevel == minecraft.level ? previous.layer + 1 : -1;
        Reuse reuse = null;
        try {
            var header = layout.resource.header();
            if (layer >= header.height()) layer = -1;
            reuse = previous == null || previous.layout != layout ? null : previous.detachGeometry();
            // Native clean also closes the previous owned world and all its VBOs.
            Thread worker = MegaWorldPreviewAccess.gtcsolo$megaGetThread();
            if (worker != null) worker.interrupt();
            MegaWorldPreviewAccess.gtcsolo$megaSetThread(null);
            MultiblockInWorldPreviewRenderer.cleanPreview();
            active = new Active(layout, origin.immutable(), controller.getFrontFacing(), controller.getUpwardsFacing(),
                    duration, layer, minecraft.level, controller.getDefinition().getBlock().defaultBlockState(), reuse);
            lastFailure = "";
        } catch (RuntimeException | LinkageError failure) {
            if (reuse != null) reuse.close();
            fail(failure);
        }
        return true;
    }

    /** Called before native rendering, even while its cache is still UNUSED. */
    public static boolean beforeRender() {
        Active current = active;
        if (current == null) return true;
        if (Minecraft.getInstance().level != current.sourceLevel) { clear(); return false; }
        try {
            current.step(System.nanoTime() + FRAME_BUDGET);
            return active == current && current.ready;
        } catch (RuntimeException | LinkageError failure) {
            fail(failure);
            return false;
        }
    }

    public static VertexBuffer[] buffers() {
        Active current = active;
        return current != null && current.ready ? current.batches.buffers() : null;
    }

    public static boolean draw(VertexBuffer receiver) {
        Active current = active;
        return current != null && current.ready && current.batches.draw(receiver);
    }

    public static void tick() {
        Active current = active;
        if (current != null && Minecraft.getInstance().level != current.sourceLevel) clear();
    }

    public static void remove(BlockPos position) {
        Active current = active;
        if (current != null && current.origin.equals(position)) clear();
    }

    public static void clear() {
        if (!Minecraft.getInstance().isSameThread()) { Minecraft.getInstance().execute(MegaWorldPreview::clear); return; }
        if (active != null) MultiblockInWorldPreviewRenderer.cleanPreview();
    }

    /** Native timeout, copied previews, and ordinary previews all close the same owned lease. */
    public static void onNativeClear() {
        Active previous = active;
        active = null;
        if (previous != null) {
            try { previous.close(); }
            catch (RuntimeException failure) { Gtcsolo.LOGGER.warn("Could not finish closing a mega preview", failure); }
        }
    }

    private static void fail(Throwable failure) {
        lastFailure = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
        Gtcsolo.LOGGER.error("Could not prepare a sparse mega in-world preview", failure);
        clear();
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable("gtcsolo.mega_preview.world_failed"), true);
    }

    public static Map<String, Object> snapshot() {
        Active current = active;
        var result = new LinkedHashMap<String, Object>();
        result.put("requests", requests);
        result.put("active", current != null);
        result.put("last_failure", lastFailure);
        if (current != null) {
            result.put("resource", current.layout.id);
            result.put("layer", current.layer);
            result.put("stage", current.stage);
            result.put("visited_cells", current.visited);
            result.put("world_blocks", current.worldBlocks);
            result.put("ready", current.ready);
        }
        return result;
    }

    /** Exactly the coordinate operations used by native showPreview; export axes are RIGHT/UP/BACK. */
    public static BlockPos worldPosition(BlockPos local, MegaPatternResource.Header header, BlockPos origin,
                                         Direction front, Direction up) {
        BlockPos offset = local.offset(-header.anchorX(), -header.anchorY(), -header.anchorZ());
        offset = offset.rotate(switch (front) {
            case NORTH, UP, DOWN -> Rotation.NONE;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_90;
        });
        Rotation rotation = up == Direction.EAST ? Rotation.CLOCKWISE_90 : up == Direction.SOUTH
                ? Rotation.CLOCKWISE_180 : up == Direction.WEST ? Rotation.COUNTERCLOCKWISE_90 : Rotation.NONE;
        return origin.offset(MegaWorldPreviewAccess.gtcsolo$megaRotateByFrontAxis(offset, front, rotation));
    }

    public static BlockState machineFacing(BlockState state, Direction front) {
        if (!(state.getBlock() instanceof MetaMachineBlock machine)) return state;
        var rotation = machine.getRotationState();
        if (rotation == RotationState.NONE) return state;
        Direction face = state.getValue(rotation.property);
        if (face.getAxis() != Direction.Axis.Y) face = switch (front) {
            case NORTH, UP, DOWN -> front;
            case SOUTH -> face.getOpposite();
            case WEST -> face.getCounterClockWise();
            case EAST -> face.getClockWise();
        };
        return rotation.test(face) ? state.setValue(rotation.property, face) : state;
    }

    private static BlockState property(BlockState state, String name, String text) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) throw new IllegalStateException("Unknown mega preview property " + name);
        return propertyValue(state, property, text);
    }
    private static <T extends Comparable<T>> BlockState propertyValue(BlockState state, Property<T> property, String text) {
        return state.setValue(property, property.getValue(text)
                .orElseThrow(() -> new IllegalStateException("Invalid mega preview property value " + text)));
    }

    private record Reuse(MegaWorldPreviewData data, Map<Character, BlockState> palette,
                         MegaWorldPreviewTemplate template, Long2ObjectOpenHashMap<BlockState> ports) {
        void close() { try { data.close(); } catch (Exception failure) { Gtcsolo.LOGGER.warn("Could not close mega preview reader", failure); } }
    }

    private static final class Active implements AutoCloseable {
        final Layout layout;
        final BlockPos origin;
        final Direction front, up;
        final int duration, layer;
        final Level sourceLevel;
        final BlockState core;
        final TrackedDummyWorld world = new TrackedDummyWorld();
        final List<BlockPos> positions = new ArrayList<>();
        MegaWorldPreviewData data;
        Map<Character, BlockState> palette;
        MegaWorldPreviewTemplate template;
        Long2ObjectOpenHashMap<BlockState> ports;
        LongIterator iterator;
        NativeWorldPreviewBatches batches;
        volatile String stage = "reading";
        volatile long visited;
        volatile int worldBlocks;
        volatile boolean ready;
        long lastStatus;

        Active(Layout layout, BlockPos origin, Direction front, Direction up, int duration, int layer,
               Level sourceLevel, BlockState core, Reuse reuse) {
            this.layout = layout; this.origin = origin; this.front = front; this.up = up;
            this.duration = duration; this.layer = layer; this.sourceLevel = sourceLevel; this.core = core;
            if (reuse != null) {
                data = reuse.data(); palette = reuse.palette(); template = reuse.template(); ports = reuse.ports();
                iterator = data.positions(); stage = "placing"; visited = data.visited();
                return;
            }
            try {
                var stream = MegaWorldPreview.class.getResourceAsStream(layout.path());
                if (stream == null) throw new IllegalStateException("Missing mega layout " + layout.id);
                try {
                    data = new MegaWorldPreviewData(new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8),
                            layout.resource.header(), layout.port);
                } catch (Exception failure) { stream.close(); throw failure; }
                palette = new HashMap<>();
                for (var entry : data.palette().entrySet()) {
                    var specification = entry.getValue().getAsJsonObject();
                    ResourceLocation id = new ResourceLocation(specification.get("block").getAsString());
                    if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalStateException("Missing mega preview block " + id);
                    Block block = ForgeRegistries.BLOCKS.getValue(id);
                    BlockState state = block.defaultBlockState();
                    for (var item : specification.getAsJsonObject("state").entrySet())
                        state = property(state, item.getKey(), item.getValue().getAsString());
                    palette.put(entry.getKey().charAt(0), state);
                }
            } catch (Exception failure) {
                close();
                throw new IllegalStateException("Could not open mega layout " + layout.id, failure);
            }
        }

        void step(long deadline) {
            if (ready) return;
            if (!data.done()) {
                try { data.step(16_384, deadline); }
                catch (Exception failure) { throw new IllegalStateException("Could not read mega layout " + layout.id, failure); }
                visited = data.visited();
                if (!data.done()) { status(false); return; }
                var corePredicate=Predicates.controller(Predicates.blocks(core.getBlock()));
                template = layout==Layout.FUSION ? MegaWorldPreviewTemplate.fixed(corePredicate)
                        : MegaWorldPreviewTemplate.create(corePredicate,layout.ports(),
                                layout.coils ? Predicates.heatingCoils() : null,data.portCount());
                ports = new Long2ObjectOpenHashMap<>();
                for (int index = 0; index < data.portCount(); index++)
                    ports.put(data.portPosition(index), template.ports().get(index));
                iterator = data.positions(); stage = "placing";
            }
            if (iterator != null) {
                int count = 0;
                while (iterator.hasNext() && count++ < 4096 && System.nanoTime() < deadline) {
                    long packed = iterator.nextLong();
                    BlockPos local = new BlockPos(MegaWorldPreviewData.x(packed), MegaWorldPreviewData.y(packed), MegaWorldPreviewData.z(packed));
                    if (layer >= 0 && local.getY() != layer) continue;
                    BlockState raw = stateAt(packed);
                    BlockState faced = raw;
                    if (raw.hasProperty(BlockStateProperties.FACING) || raw.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        BlockInfo information = BlockInfo.fromBlockState(raw);
                        ((PatternWorldPreviewAccess) (Object) template.facingHelper()).gtcsolo$worldPreviewResetFacing(
                                local, raw, null, (position, face) -> facingAllowed(position, face, raw), information::setBlockState);
                        faced = information.getBlockState();
                    }
                    BlockPos target = worldPosition(local, data.header(), origin, front, up);
                    if (data.symbol(packed) == 'K') {
                        // Native showPreview creates its inner controller from the preview state before world rotation.
                        if (BlockInfo.fromBlockState(faced).getBlockEntity(target) instanceof IMachineBlockEntity holder
                                && holder.getMetaMachine() instanceof IMultiController) {
                            holder.getSelf().setLevel(world);
                            world.setInnerBlockEntity(holder.getSelf());
                        }
                    } else if (!faced.isAir()) {
                        world.addBlock(target, BlockInfo.fromBlockState(machineFacing(faced, front)));
                        positions.add(target);
                    }
                }
                worldBlocks = positions.size();
                if (iterator.hasNext()) { status(true); return; }
                iterator = null; stage = "baking";
                batches = new NativeWorldPreviewBatches(world, positions);
            }
            batches.step(deadline);
            if (!batches.done()) { status(true); return; }
            MegaWorldPreviewAccess.gtcsolo$megaSetLevel(world);
            MegaWorldPreviewAccess.gtcsolo$megaSetBlockEntities(batches.blockEntities());
            MegaWorldPreviewAccess.gtcsolo$megaLeftTick().set(duration);
            NativeWorldPreviewBatches.publishCompiledState();
            ready = true; stage = "ready";
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable("gtcsolo.mega_preview.ready",
                    data.header().width(), data.header().height(), data.header().depth(), data.nonAirCount()), true);
        }

        private boolean facingAllowed(BlockPos position, Direction face, BlockState state) {
            BlockPos neighbor = position.relative(face);
            if (stateAt(MegaWorldPreviewData.pack(neighbor.getX(), neighbor.getY(), neighbor.getZ())).getBlock() != Blocks.AIR) return false;
            if (state.getBlock() instanceof MetaMachineBlock machine
                    && machine.newBlockEntity(BlockPos.ZERO, machine.defaultBlockState()) instanceof IMachineBlockEntity holder) {
                var candidate = holder.getMetaMachine();
                return !(candidate instanceof IMultiController) && candidate.isFacingValid(face);
            }
            return true;
        }

        private BlockState stateAt(long position) {
            char symbol = data.symbol(position);
            if (symbol == 0) return Blocks.AIR.defaultBlockState();
            if (symbol == 'K') return core;
            if (symbol == layout.port) return ports.get(position);
            if (symbol == 'E' && layout.coils) return template.coil();
            BlockState state = palette.get(symbol);
            if (state == null) throw new IllegalStateException("Unknown mega preview symbol " + symbol);
            return state;
        }

        private void status(boolean building) {
            long now = System.nanoTime();
            if (now - lastStatus < 1_000_000_000L) return;
            lastStatus = now;
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable(
                    building ? "gtcsolo.mega_preview.mesh" : "gtcsolo.mega_preview.loading",
                    building ? batches == null ? 0 : batches.percent() : data.visited() * 100 / data.volume()), true);
        }

        Reuse detachGeometry() {
            if (data == null || !data.done() || template == null || ports == null) return null;
            Reuse result = new Reuse(data, palette, template, ports);
            data = null; palette = null; template = null; ports = null;
            return result;
        }

        @Override public void close() {
            try {
                if (batches != null) batches.close();
            } finally {
                batches = null;
                world.clear(); positions.clear(); iterator = null;
                if (data != null) { try { data.close(); } catch (Exception failure) { Gtcsolo.LOGGER.warn("Could not close mega layout", failure); } data = null; }
                if (palette != null) palette.clear();
                if (ports != null) ports.clear();
                palette = null; ports = null; template = null;
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> clear());
        }
    }
}
