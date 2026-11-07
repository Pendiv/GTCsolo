package DIV.gtcsolo.buildcopy;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.copy.PasteBlock;
import DIV.gtcsolo.block.copy.PasteBlockEntity;
import DIV.gtcsolo.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Server-thread-only bounded work: states first, BE contents second, neighbor notifications last. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID)
public final class PasteJobs {
    private static final Map<ServerLevel, Job> JOBS = new HashMap<>();
    private static final int BLOCKS_PER_TICK = 128;
    private PasteJobs() {}

    public static void start(ServerPlayer player, PasteBlockEntity block, CopyLayout layout) {
        var level = player.serverLevel();
        if (JOBS.containsKey(level)) {
            player.sendSystemMessage(Component.translatable("gtcsolo.copy.busy")); return;
        }
        var origin = block.getBlockPos().immutable();
        var rotation = layout.rotationTo(block.getBlockState().getValue(PasteBlock.FACING));
        for (int i = 0; i < layout.volume(); i++) {
            var target = layout.target(i, origin, rotation);
            if (!level.getWorldBorder().isWithinBounds(target) || level.isOutsideBuildHeight(target)) {
                player.sendSystemMessage(Component.translatable("gtcsolo.copy.outside_world")); return;
            }
            if (!level.hasChunkAt(target)) {
                player.sendSystemMessage(Component.translatable("gtcsolo.copy.unloaded")); return;
            }
        }
        JOBS.put(level, new Job(player, layout, origin, rotation, block.copyId()));
        player.sendSystemMessage(Component.translatable("gtcsolo.copy.started", layout.name, layout.volume()));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var iterator = JOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try { if (entry.getValue().advance(entry.getKey())) iterator.remove(); }
            catch (RuntimeException ex) {
                Gtcsolo.LOGGER.error("Copy paste stopped; some blocks may already be placed", ex);
                entry.getValue().message(entry.getKey().getServer(), "gtcsolo.copy.interrupted");
                iterator.remove();
            }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { JOBS.clear(); }

    private static final class Job {
        final UUID player, copy;
        final CopyLayout layout;
        final BlockPos origin;
        final Rotation rotation;
        final int anchorIndex;
        int phase, cursor;
        Job(ServerPlayer player, CopyLayout layout, BlockPos origin, Rotation rotation, UUID copy) {
            this.player = player.getUUID(); this.copy = copy; this.layout = layout;
            this.origin = origin; this.rotation = rotation; anchorIndex = layout.index(layout.anchor);
        }
        void message(MinecraftServer server, String key) {
            var recipient = server.getPlayerList().getPlayer(player);
            if (recipient != null) recipient.sendSystemMessage(Component.translatable(key, layout.name));
        }
        // Keep the controller until the end of each phase; a corner source block may replace it.
        int orderedIndex(int step) { return step == layout.volume() - 1 ? anchorIndex : step >= anchorIndex ? step + 1 : step; }
        boolean advance(ServerLevel level) {
            var actor = level.getServer().getPlayerList().getPlayer(player);
            if (actor == null || !actor.hasPermissions(2) || !actor.isCreative()) {
                message(level.getServer(), "gtcsolo.copy.interrupted"); return true;
            }
            for (int work = 0; work < BLOCKS_PER_TICK; work++) {
                int index = orderedIndex(cursor);
                var target = layout.target(index, origin, rotation);
                if (!level.hasChunkAt(target)) { message(level.getServer(), "gtcsolo.copy.interrupted"); return true; }
                var state = layout.rotatedState(index, rotation);
                boolean preserveController = index == anchorIndex && state.is(ModBlocks.BLOCK10.get());
                if (phase == 0) {
                    // Abort if someone replaces/removes the builder before it finishes writing states.
                    if (!(level.getBlockEntity(origin) instanceof PasteBlockEntity builder) || !copy.equals(builder.copyId())) {
                        message(level.getServer(), "gtcsolo.copy.interrupted"); return true;
                    }
                    if (!preserveController) {
                        Clearable.tryClear(level.getBlockEntity(target));
                        // Vanilla structure placement uses a barrier to reset same-state block entities.
                        if (level.getBlockEntity(target) != null) level.setBlock(target, Blocks.BARRIER.defaultBlockState(), 2 | 16);
                        if (!level.setBlock(target, state, 2 | 16 | 32) && level.getBlockState(target) != state)
                            throw new IllegalStateException("Could not place block at " + target);
                    }
                } else if (phase == 1 && !preserveController) {
                    var data = layout.entityData(index);
                    if (data != null && level.getBlockState(target) != state)
                        throw new IllegalStateException("Block changed before copied data could be restored at " + target);
                    var entity = level.getBlockEntity(target);
                    if (data != null && entity == null)
                        throw new IllegalStateException("Missing copied block entity at " + target);
                    if (data != null && entity != null) {
                        // Resolve all copied positions before restoring contents/configuration.
                        data.putInt("x", target.getX()); data.putInt("y", target.getY()); data.putInt("z", target.getZ());
                        entity.load(data);
                        entity.setChanged();
                        level.sendBlockUpdated(target, state, state, Block.UPDATE_CLIENTS);
                    }
                } else if (phase == 2) {
                    var actual = level.getBlockState(target);
                    actual.updateNeighbourShapes(level, target, Block.UPDATE_CLIENTS);
                    level.updateNeighborsAt(target, actual.getBlock());
                }
                if (++cursor == layout.volume()) {
                    cursor = 0;
                    if (++phase == 3) {
                        message(level.getServer(), "gtcsolo.copy.finished"); return true;
                    }
                }
            }
            return false;
        }
    }
}
