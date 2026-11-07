package DIV.gtcsolo.render.halo;

import DIV.gtcsolo.Gtcsolo;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only registration for gtcsolo's Re-Avaritia-compatible geometry loaders.
 */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HaloGeometryLoaders {

    private HaloGeometryLoaders() {}

    @SubscribeEvent
    public static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        if (ModList.get().isLoaded("avaritia")) {
            event.register("rainbow_halo", RainbowHaloModelLoader.INSTANCE);
        }
    }
}
