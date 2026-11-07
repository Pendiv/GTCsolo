package DIV.gtcsolo.client;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.network.MaToolModePacket;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.registry.tool.fullcustom.MaCrystalToolItem;
import DIV.gtcsolo.registry.tool.fullcustom.MaToolMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class MaToolClientEvents {
    private static double wheelRemainder;
    private static int previousSlot = -1;
    private MaToolClientEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || minecraft.gameMode == null || player.isSpectator()
                || !Screen.hasShiftDown() || !(player.getMainHandItem().getItem() instanceof MaCrystalToolItem)) {
            wheelRemainder = 0;
            previousSlot = -1;
            return;
        }
        double delta = event.getScrollDelta();
        if (!Double.isFinite(delta) || delta == 0) return;
        event.setCanceled(true);
        int slot = player.getInventory().selected;
        if (slot != previousSlot) wheelRemainder = 0;
        previousSlot = slot;
        if (Math.signum(wheelRemainder) != Math.signum(delta)) wheelRemainder = 0;
        wheelRemainder += delta;
        int steps = (int) wheelRemainder;
        if (steps == 0) return;
        wheelRemainder -= steps;
        var stack = player.getMainHandItem();
        MaToolMode current = MaToolMode.get(stack);
        MaToolMode mode = current.cycle(steps);
        if (mode == current) return;
        minecraft.gameMode.stopDestroyBlock();
        MaToolMode.set(stack, mode);
        ModNetwork.CHANNEL.sendToServer(new MaToolModePacket(slot, ForgeRegistries.ITEMS.getKey(stack.getItem()), mode.ordinal()));
    }
}
