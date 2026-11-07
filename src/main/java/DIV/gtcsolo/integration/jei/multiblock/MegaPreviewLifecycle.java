package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** JEI retains wrappers after closing a page; release worlds/GL buffers independently. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class MegaPreviewLifecycle {
    private static final PreviewFrameLease<MegaPreviewWidget> LEASES = new PreviewFrameLease<>();
    private MegaPreviewLifecycle() {}
    static void touch(MegaPreviewWidget widget) { LEASES.touch(widget); }
    public static void clear() {
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(MegaPreviewLifecycle::clear);
            return;
        }
        LEASES.clear(MegaPreviewWidget::releasePreview);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) clear();
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) LEASES.beginFrame();
        else LEASES.expire(MegaPreviewWidget::releasePreview);
    }
}
