package DIV.gtcsolo.network;

import DIV.gtcsolo.block.ExtendEnergyCubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: UIスイッチ切り替え通知
 */
public class EnergyCubeSwitchPacket {

    /** 0 = emitMode切替, 1 = euMode切替 */
    private final int switchId;
    private final boolean value;
    private final BlockPos pos;

    public EnergyCubeSwitchPacket(BlockPos pos, int switchId, boolean value) {
        this.pos = pos;
        this.switchId = switchId;
        this.value = value;
    }

    public static void encode(EnergyCubeSwitchPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.switchId);
        buf.writeBoolean(packet.value);
    }

    public static EnergyCubeSwitchPacket decode(FriendlyByteBuf buf) {
        return new EnergyCubeSwitchPacket(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(EnergyCubeSwitchPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(packet.pos)
                    instanceof ExtendEnergyCubeBlockEntity be)) return;

            switch (packet.switchId) {
                case 0 -> be.setEmitMode(packet.value);
                case 1 -> be.setEuMode(packet.value);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}