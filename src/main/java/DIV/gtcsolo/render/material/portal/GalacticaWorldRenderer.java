package DIV.gtcsolo.render.material.portal;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Positions are indexed on chunk load and updates; only the actual portal pass runs per frame. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class GalacticaWorldRenderer {
    private static final ResourceLocation MATERIAL = new ResourceLocation("gtcsolo", "galactica");
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(16384));
    private static volatile Map<BlockState, GalacticaBlockModel> models = Map.of();
    private static volatile GalacticaBlockModel frameModel;
    private static final Map<Long, Set<Long>> blocks = new ConcurrentHashMap<>();
    private static final Map<Long, Set<Long>> pipes = new ConcurrentHashMap<>();
    private static final Set<Long> pendingPipes = ConcurrentHashMap.newKeySet();
    private static ClientLevel indexedLevel;

    private GalacticaWorldRenderer() {}

    static void installModels(Map<BlockState, GalacticaBlockModel> next) {
        models = Map.copyOf(next);
        frameModel = next.values().stream().filter(GalacticaBlockModel::isFrame).findFirst().orElse(null);
    }

    public static boolean isGalactica(Material material) {
        return material != null && MATERIAL.equals(material.getResourceLocation());
    }

    static boolean matches(BlockState state) {
        return state.getBlock() instanceof MaterialBlock form && MATERIAL.equals(form.material.getResourceLocation())
                && (form.tagPrefix == TagPrefix.block || form.tagPrefix == TagPrefix.frameGt);
    }

    private static boolean matchesPipe(ClientLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof IPipeNode<?, ?> pipe)) return false;
        var material = pipe.getFrameMaterial();
        return isGalactica(material);
    }

    private static long sectionKey(BlockPos pos) { return SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4); }
    private static void update(Map<Long, Set<Long>> index, BlockPos pos, boolean present) {
        long section = sectionKey(pos), key = pos.asLong();
        if (present) index.computeIfAbsent(section, ignored -> ConcurrentHashMap.newKeySet()).add(key);
        else {
            var positions = index.get(section);
            if (positions != null) {
                positions.remove(key);
                if (positions.isEmpty()) index.remove(section, positions);
            }
        }
    }

    /** Called from ClientLevel.sendBlockUpdated after updates, including block-entity rerenders. */
    public static void blockChanged(ClientLevel level, BlockPos pos, BlockState state) {
        if (indexedLevel != level) return;
        update(blocks, pos, matches(state));
        if (pipes.containsKey(sectionKey(pos)) || state.hasBlockEntity()) update(pipes, pos, matchesPipe(level, pos));
    }

    /** May be called on a chunk build thread; positions are verified on the next client tick. */
    public static void observePipe(BlockPos pos) { pendingPipes.add(pos.asLong()); }

    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        if (indexedLevel != level) reset(level);
        clearChunk(chunk);
        var sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.hasOnlyAir() || !section.getStates().maybeHas(GalacticaWorldRenderer::matches)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (matches(section.getBlockState(x, y, z)))
                    update(blocks, new BlockPos(baseX + x, baseY + y, baseZ + z), true);
            }
        }
        for (BlockPos pos : chunk.getBlockEntities().keySet()) if (matchesPipe(level, pos)) update(pipes, pos, true);
    }

    @SubscribeEvent public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() != indexedLevel) return;
        clearChunk(event.getChunk());
    }

    private static void clearChunk(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            long key = SectionPos.asLong(chunk.getPos().x, chunk.getSectionYFromSectionIndex(i), chunk.getPos().z);
            blocks.remove(key);
            pipes.remove(key);
        }
    }

    @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (indexedLevel != level) reset(level);
        if (level == null) return;
        for (Long key : pendingPipes) {
            if (!pendingPipes.remove(key)) continue;
            BlockPos pos = BlockPos.of(key);
            if (level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) update(pipes, pos, matchesPipe(level, pos));
        }
    }

    private static void reset(ClientLevel level) {
        blocks.clear(); pipes.clear(); pendingPipes.clear(); indexedLevel = level;
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level != indexedLevel || models.isEmpty()) return;
        var camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        VertexConsumer out = BUFFER.getBuffer(RenderType.endPortal());
        drawPositions(blocks, false, level, event, pose, out, camera.x, camera.y, camera.z);
        drawPositions(pipes, true, level, event, pose, out, camera.x, camera.y, camera.z);
        BUFFER.endBatch(RenderType.endPortal());
    }

    private static void drawPositions(Map<Long, Set<Long>> index, boolean pipe, ClientLevel level,
                                      RenderLevelStageEvent event, PoseStack pose, VertexConsumer out,
                                      double cameraX, double cameraY, double cameraZ) {
        for (var section : index.entrySet()) {
            var sectionPos = SectionPos.of(section.getKey());
            int x = sectionPos.minBlockX(), y = sectionPos.minBlockY(), z = sectionPos.minBlockZ();
            if (!event.getFrustum().isVisible(new AABB(x, y, z, x + 16, y + 16, z + 16))) continue;
            for (long key : section.getValue()) {
                BlockPos pos = BlockPos.of(key);
                if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                BlockState state = level.getBlockState(pos);
                GalacticaBlockModel model;
                IPipeNode<?, ?> node = null;
                if (pipe) {
                    if (!(level.getBlockEntity(pos) instanceof IPipeNode<?, ?> value) || !matchesPipe(level, pos)) {
                        section.getValue().remove(key);
                        continue;
                    }
                    node = value;
                    model = frameModel;
                } else model = models.get(state);
                if (model == null) continue;
                pose.pushPose();
                try {
                    pose.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                    Matrix4f matrix = pose.last().pose();
                    for (Direction side : Direction.values()) {
                        // A frame's far side is visible through its holes.
                        if (!model.isFrame() && behind(side, pos, cameraX, cameraY, cameraZ)) continue;
                        if (node != null && (node.getConnections() & (1 << (12 + side.get3DDataValue()))) != 0) continue;
                        if (node == null && !Block.shouldRenderFace(state, level, pos, side, pos.relative(side))) continue;
                        emit(out, matrix, model.portalFaces(side), model.isFrame() && reverseFacing(side, pos, cameraX, cameraY, cameraZ));
                    }
                    emit(out, matrix, model.portalFaces(null));
                } finally {
                    pose.popPose();
                }
            }
            if (section.getValue().isEmpty()) index.remove(section.getKey(), section.getValue());
        }
    }

    private static boolean behind(Direction side, BlockPos pos, double x, double y, double z) {
        return switch (side) {
            case WEST -> x > pos.getX() + 1;
            case EAST -> x < pos.getX();
            case DOWN -> y > pos.getY() + 1;
            case UP -> y < pos.getY();
            case NORTH -> z > pos.getZ() + 1;
            case SOUTH -> z < pos.getZ();
        };
    }

    private static boolean reverseFacing(Direction side, BlockPos pos, double x, double y, double z) {
        return switch (side) {
            case WEST -> x > pos.getX();
            case EAST -> x < pos.getX() + 1;
            case DOWN -> y > pos.getY();
            case UP -> y < pos.getY() + 1;
            case NORTH -> z > pos.getZ();
            case SOUTH -> z < pos.getZ() + 1;
        };
    }

    static void emit(VertexConsumer out, Matrix4f matrix, java.util.List<BakedQuad> quads) {
        emit(out, matrix, quads, false);
    }

    private static void emit(VertexConsumer out, Matrix4f matrix, java.util.List<BakedQuad> quads, boolean reverse) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int i = 0; i < 4; i++) {
                int at = (reverse ? 3 - i : i) * stride;
                out.vertex(matrix, Float.intBitsToFloat(data[at]), Float.intBitsToFloat(data[at + 1]),
                        Float.intBitsToFloat(data[at + 2])).endVertex();
            }
        }
    }
}
