package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.machine.WENMainStorageMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/** クライアント→サーバー: モニターからネットワークID設定 */
public class WENSetIdPacket {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockPos monitorPos;
    private final String networkId;

    public WENSetIdPacket(BlockPos monitorPos, String networkId) {
        this.monitorPos = monitorPos;
        this.networkId = networkId;
    }

    public static void encode(WENSetIdPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.monitorPos);
        buf.writeUtf(pkt.networkId, 64);
    }

    public static WENSetIdPacket decode(FriendlyByteBuf buf) {
        return new WENSetIdPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(WENSetIdPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!(player.level().getBlockEntity(pkt.monitorPos)
                    instanceof WENDataMonitorBlockEntity monitor)) {
                LOGGER.warn("[WEN] SetId: no monitor at {}", pkt.monitorPos);
                return;
            }

            WENMainStorageMachine wen = monitor.getController();
            if (wen == null) {
                LOGGER.warn("[WEN] SetId: monitor not linked to controller");
                player.sendSystemMessage(Component.translatable("gui.gtcsolo.wen_monitor.not_linked"));
                return;
            }

            boolean ok = wen.setNetworkId(pkt.networkId);
            if (ok) {
                player.sendSystemMessage(Component.translatable(
                        "gui.gtcsolo.wen_monitor.id_set", pkt.networkId));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "gui.gtcsolo.wen_monitor.id_failed", pkt.networkId));
            }

            // 即座にUIを更新
            if (player.level().getBlockEntity(pkt.monitorPos)
                    instanceof WENDataMonitorBlockEntity monitorBE) {
                monitorBE.forceSyncNow();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}