package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENAePortBlockEntity;
import DIV.gtcsolo.block.wen.WENFePortBlockEntity;
import DIV.gtcsolo.machine.wen.WENEnergyHatchMachine;
import DIV.gtcsolo.machine.wen.WENEnergyOutputHatchMachine;
import DIV.gtcsolo.machine.wen.WENWirelessInputMachine;
import DIV.gtcsolo.machine.wen.WENWirelessOutputMachine;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/** クライアント→サーバー: ID選択UIからのID設定（汎用） */
public class WENSelectIdPacket {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockPos machinePos;
    private final String networkId;

    public WENSelectIdPacket(BlockPos machinePos, String networkId) {
        this.machinePos = machinePos;
        this.networkId = networkId;
    }

    public static void encode(WENSelectIdPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.machinePos);
        buf.writeUtf(pkt.networkId, 64);
    }

    public static WENSelectIdPacket decode(FriendlyByteBuf buf) {
        return new WENSelectIdPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(WENSelectIdPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            var be = player.level().getBlockEntity(pkt.machinePos);
            if (be instanceof MetaMachineBlockEntity mmbe) {
                var machine = mmbe.getMetaMachine();
                if (machine instanceof WENEnergyHatchMachine hatch) {
                    hatch.setLinkedNetworkId(pkt.networkId);
                } else if (machine instanceof WENWirelessInputMachine input) {
                    input.setLinkedNetworkId(pkt.networkId);
                } else if (machine instanceof WENWirelessOutputMachine output) {
                    output.setLinkedNetworkId(pkt.networkId);
                } else if (machine instanceof WENEnergyOutputHatchMachine outputHatch) {
                    outputHatch.setLinkedNetworkId(pkt.networkId);
                } else {
                    return;
                }
            } else if (be instanceof WENAePortBlockEntity aePort) {
                aePort.setLinkedNetworkId(pkt.networkId);
            } else if (be instanceof WENFePortBlockEntity fePort) {
                fePort.setLinkedNetworkId(pkt.networkId);
            } else {
                return;
            }

            player.sendSystemMessage(Component.literal("§aLinked to: " + pkt.networkId));
            LOGGER.info("[WEN] {} selected ID '{}' at {}",
                    player.getName().getString(), pkt.networkId, pkt.machinePos);
        });
        ctx.get().setPacketHandled(true);
    }
}