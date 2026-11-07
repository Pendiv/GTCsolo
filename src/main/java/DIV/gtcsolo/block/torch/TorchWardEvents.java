package DIV.gtcsolo.block.torch;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TorchWardEvents {
    private TorchWardEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (blocks(event, event.getSpawnType())) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // PhantomSpawner bypasses PositionCheck. Cancel the actual insertion, not just finalization.
        if (blocks(event, event.getSpawnType())) event.setSpawnCancelled(true);
    }

    private static boolean blocks(MobSpawnEvent event, MobSpawnType reason) {
        // This deliberately leaves spawners, summons, raids/patrols, reinforcements, conversions,
        // and initial terrain-generation populations alone. No entity is removed after it spawns.
        if (reason != MobSpawnType.NATURAL) return false;
        var mob = event.getEntity();
        if (mob.getType().getCategory() != MobCategory.MONSTER && !(mob instanceof Enemy)) return false;
        ServerLevel level = event.getLevel().getLevel();
        return TorchWardData.get(level).blocksSpawn(level, event.getX(), event.getY(), event.getZ());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        // Chunk load callbacks can originate during async preparation. SavedData belongs to the server thread.
        level.getServer().execute(() -> {
            if (level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) == chunk) {
                TorchWardData.get(level).reconcile(chunk);
            }
        });
    }
}
