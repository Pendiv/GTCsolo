package DIV.gtcsolo.render.dynamic;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * AddPackFindersEvent (CLIENT_RESOURCES) を LOWEST priority で購読 = GTCEu の NORMAL priority
 * registerPackFinders の **後** に走る = clearClient() で wipe された DATA に再 inject。
 *
 * 競合の経緯:
 *  - GTCEu CommonProxy.registerPackFinders (@SubscribeEvent priority=NORMAL) が
 *    CLIENT_RESOURCES の addPackFinders で GTDynamicResourcePack.clearClient() (= DATA.clear()) する。
 *  - mod constructor で inject しても、 この clear で消える → atlas stitch 時に DATA に存在せず白化。
 *  - LOWEST で再 inject すれば clear の後で再注入される → atlas stitch / reinitModels に間に合う。
 */
@Mod.EventBusSubscriber(modid = "gtcsolo", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaskedTextureClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MaskedTextureClient() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        LOGGER.debug("[MaskedTex] AddPackFindersEvent CLIENT_RESOURCES @LOWEST — re-injecting after GTCEu clearClient()");
        MaskedTextureProvider.generateAll();
    }
}
