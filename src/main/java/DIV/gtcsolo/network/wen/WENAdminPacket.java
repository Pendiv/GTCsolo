package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.machine.wen.WENNetworkData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/** クライアント→サーバー: 管理者操作 */
public class WENAdminPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Action { DELETE_NETWORK, SET_ENERGY, SET_CAPACITY }

    private final BlockPos monitorPos;
    private final Action action;
    private final String targetId;
    private final long value;

    public WENAdminPacket(BlockPos monitorPos, Action action, String targetId, long value) {
        this.monitorPos = monitorPos;
        this.action = action;
        this.targetId = targetId;
        this.value = value;
    }

    public static void encode(WENAdminPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.monitorPos);
        buf.writeEnum(pkt.action);
        buf.writeUtf(pkt.targetId, 64);
        buf.writeLong(pkt.value);
    }

    public static WENAdminPacket decode(FriendlyByteBuf buf) {
        return new WENAdminPacket(buf.readBlockPos(), buf.readEnum(Action.class),
                buf.readUtf(64), buf.readLong());
    }

    public static void handle(WENAdminPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // OP権限チェック
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("§cPermission denied"));
                return;
            }

            WENNetworkData data = WENNetworkData.get(player.server.overworld());

            switch (pkt.action) {
                case DELETE_NETWORK -> {
                    data.removeNetwork(pkt.targetId);
                    player.sendSystemMessage(Component.literal("§aDeleted network: " + pkt.targetId));
                    LOGGER.info("[WEN Admin] {} deleted network '{}'", player.getName().getString(), pkt.targetId);
                }
                case SET_ENERGY -> {
                    data.setEnergy(pkt.targetId, pkt.value);
                    player.sendSystemMessage(Component.literal("§aSet energy: " + pkt.targetId + " = " + pkt.value + "EU"));
                    LOGGER.info("[WEN Admin] {} set energy '{}' to {}EU", player.getName().getString(), pkt.targetId, pkt.value);
                }
                case SET_CAPACITY -> {
                    data.setCapacity(pkt.targetId, pkt.value);
                    player.sendSystemMessage(Component.literal("§aSet capacity: " + pkt.targetId + " = " + pkt.value + "EU"));
                    LOGGER.info("[WEN Admin] {} set capacity '{}' to {}EU", player.getName().getString(), pkt.targetId, pkt.value);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}