package DIV.gtcsolo.client.performance;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.integration.jei.multiblock.MegaPreviewLifecycle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cached preview UVs and sprite references belong to a single resource generation. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PerformanceModelReloads {
    private PerformanceModelReloads() {}

    @SubscribeEvent
    public static void modelsBaked(ModelEvent.BakingCompleted event) {
        MegaPreviewLifecycle.clear();
    }
}
