package DIV.gtcsolo.network;

import DIV.gtcsolo.item.SecretSwordItem;
import DIV.gtcsolo.item.SecretSwordModeHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: secret_sword の左クリック動作通知。
 *
 *   - shiftCycle=true:  mode を 1 進める (= shift+左クリック)
 *   - shiftCycle=false: 現在 mode の左クリック動作を実行 (= 通常左クリック)
 */
public class SecretSwordActionPacket {

    private final boolean shiftCycle;

    public SecretSwordActionPacket(boolean shiftCycle) {
        this.shiftCycle = shiftCycle;
    }

    public static void encode(SecretSwordActionPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.shiftCycle);
    }

    public static SecretSwordActionPacket decode(FriendlyByteBuf buf) {
        return new SecretSwordActionPacket(buf.readBoolean());
    }

    public static void handle(SecretSwordActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof SecretSwordItem)) return;

            if (packet.shiftCycle) {
                int newMode = SecretSwordItem.cycleMode(stack);
                player.displayClientMessage(
                        Component.translatable("item.gtcsolo.secret_sword.switched", newMode),
                        true);
                return;
            }
            SecretSwordModeHandlers.execute(player, stack, SecretSwordItem.getMode(stack));
        });
        ctx.get().setPacketHandled(true);
    }
}
