package DIV.gtcsolo.common;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.network.OrangeThrowPacket;
import DIV.gtcsolo.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ミカンを左クリックで投げるためのクライアント側 Forge イベントハンドラ。
 * 左クリックは vanilla の attack 系統に乗ってる (LeftClickEmpty / LeftClickBlock / AttackEntityEvent) ので
 * 3 系統すべてを捕捉してパケットを送り、 元の動作はキャンセルする。
 */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OrangeClickHandler {

    private static boolean holdingOrange(Player player) {
        return player.getMainHandItem().is(ModItems.ORANGE.get())
                || player.getOffhandItem().is(ModItems.ORANGE.get());
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (!holdingOrange(player)) return;
        ModNetwork.CHANNEL.sendToServer(new OrangeThrowPacket());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;
        if (!holdingOrange(player)) return;
        ModNetwork.CHANNEL.sendToServer(new OrangeThrowPacket());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;
        if (!holdingOrange(player)) return;
        ModNetwork.CHANNEL.sendToServer(new OrangeThrowPacket());
        event.setCanceled(true);
    }
}
