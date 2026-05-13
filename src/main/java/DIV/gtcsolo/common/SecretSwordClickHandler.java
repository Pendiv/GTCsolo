package DIV.gtcsolo.common;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.item.SecretSwordItem;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.network.SecretSwordActionPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * SecretSword 用の client side 左クリック ハンドラ。
 *
 *   - shift+左クリック (どこでも): mode を 1 進める (= SecretSwordActionPacket(shiftCycle=true))
 *   - 通常 左クリック on air/block (Mode 1/4/6/7/8): mode action 発火 (= shiftCycle=false)
 *   - 通常 左クリック on entity (Mode 1/4/6/7/8): vanilla attack をキャンセルして mode action 発火
 *   - Mode 2/3/5 の通常左クリック: vanilla の挙動 (Mode 3 は hurtEnemy で knockup 注入)
 */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SecretSwordClickHandler {

    private static boolean isHolding(Player player) {
        return player.getMainHandItem().getItem() instanceof SecretSwordItem;
    }

    /** mode action を持たない (= 左クリックで何もしない / vanilla 挙動を維持する) mode 群 */
    private static boolean modeHasNoLeftClickAction(int mode) {
        return mode == 2 || mode == 3 || mode == 5;
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (!isHolding(player)) return;
        if (player.isShiftKeyDown()) {
            ModNetwork.CHANNEL.sendToServer(new SecretSwordActionPacket(true));
            return;
        }
        int mode = SecretSwordItem.getMode(player.getMainHandItem());
        if (modeHasNoLeftClickAction(mode)) return;
        ModNetwork.CHANNEL.sendToServer(new SecretSwordActionPacket(false));
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;
        if (!isHolding(player)) return;
        if (player.isShiftKeyDown()) {
            ModNetwork.CHANNEL.sendToServer(new SecretSwordActionPacket(true));
            event.setCanceled(true);
            return;
        }
        int mode = SecretSwordItem.getMode(player.getMainHandItem());
        if (modeHasNoLeftClickAction(mode)) return; // ブロック攻撃は vanilla
        ModNetwork.CHANNEL.sendToServer(new SecretSwordActionPacket(false));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;
        if (!isHolding(player)) return;
        if (player.isShiftKeyDown()) {
            ModNetwork.CHANNEL.sendToServer(new SecretSwordActionPacket(true));
            event.setCanceled(true);
            return;
        }
        ItemStack stack = player.getMainHandItem();
        int mode = SecretSwordItem.getMode(stack);
        if (modeHasNoLeftClickAction(mode)) return; // Mode 3 は ここで vanilla attack 透過させて hurtEnemy で knockup
        ModNetwork.CHANNEL.sendToServer(new SecretSwordActionPacket(false));
        event.setCanceled(true);
    }
}
