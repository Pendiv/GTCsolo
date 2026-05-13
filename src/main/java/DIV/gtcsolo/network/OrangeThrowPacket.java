package DIV.gtcsolo.network;

import DIV.gtcsolo.entity.OrangeProjectile;
import DIV.gtcsolo.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: 左クリックでミカンを投擲したい通知。
 * サーバー側で OrangeProjectile を spawn し、 手に持っているミカンを 1 個消費する。
 */
public class OrangeThrowPacket {

    public OrangeThrowPacket() {}

    public static void encode(OrangeThrowPacket packet, FriendlyByteBuf buf) {
        // ペイロード無し (= プレイヤー方向は server 側で取得する)
    }

    public static OrangeThrowPacket decode(FriendlyByteBuf buf) {
        return new OrangeThrowPacket();
    }

    public static void handle(OrangeThrowPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // メインハンドの mikan を優先、 無ければオフハンドをチェック
            ItemStack stack = player.getMainHandItem();
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (!stack.is(ModItems.ORANGE.get())) {
                stack = player.getOffhandItem();
                hand = InteractionHand.OFF_HAND;
                if (!stack.is(ModItems.ORANGE.get())) return;
            }

            OrangeProjectile orange = new OrangeProjectile(player.level(), player);
            orange.setItem(stack.copyWithCount(1));
            // 視線方向に発射、 速度 1.5 (= 雪玉と同等)
            orange.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 1.0f);
            player.level().addFreshEntity(orange);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
