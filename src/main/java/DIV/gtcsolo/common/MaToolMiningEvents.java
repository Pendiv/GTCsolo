package DIV.gtcsolo.common;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.tool.fullcustom.MaToolMode;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID)
public final class MaToolMiningEvents {
    private MaToolMiningEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled()
                || !MaToolMode.isPrecision(event.getEntity().getMainHandItem())
                || event.getPosition().isEmpty()) return;
        if (!MaToolMode.isSuitableTarget(event.getEntity().getMainHandItem(), event.getState())) {
            event.setNewSpeed(0);
            return;
        }
        float hardness =
                event.getState()
                        .getDestroySpeed(event.getEntity().level(), event.getPosition().get());
        if (hardness <= 0 || !Float.isFinite(hardness)) return;
        int divisor =
                ForgeHooks.isCorrectToolForDrops(event.getState(), event.getEntity()) ? 30 : 100;
        // Forge fires this after water, airborne, effects and efficiency modifiers.
        // BlockBehaviour then divides this value by hardness and the same harvest divisor.
        double speed = (double) hardness * divisor / MaToolMode.PRECISION_TICKS;
        event.setNewSpeed((float) Math.min(speed, Float.MAX_VALUE));
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        var stack = event.getPlayer().getMainHandItem();
        if (MaToolMode.isPrecision(stack)
                && !MaToolMode.isSuitableTarget(stack, event.getState())) {
            event.setCanceled(true);
        }
    }
}
