package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.block.wen.WENDataMonitorMenu;
import DIV.gtcsolo.machine.wen.WENMonitorData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Selects a bounded monitor view without sending the entire network registry to every viewer. */
public record WENMonitorViewPacket(int menuId, BlockPos pos, int page, int offset) {
    public static void encode(WENMonitorViewPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.menuId());
        buffer.writeBlockPos(packet.pos());
        buffer.writeVarInt(packet.page());
        buffer.writeVarInt(packet.offset());
    }

    public static WENMonitorViewPacket decode(FriendlyByteBuf buffer) {
        return new WENMonitorViewPacket(
                buffer.readVarInt(),
                buffer.readBlockPos(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(WENMonitorViewPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            var player = context.get().getSender();
                            if (player == null
                                    || packet.page() < 0
                                    || packet.page() >= WENMonitorData.PAGE_COUNT
                                    || packet.offset() < 0
                                    || !(player.containerMenu instanceof WENDataMonitorMenu menu)
                                    || menu.containerId != packet.menuId()
                                    || !menu.getMonitorPos().equals(packet.pos())
                                    || !menu.stillValid(player)) return;
                            menu.setView(packet.page(), packet.offset());
                            if (player.level().getBlockEntity(packet.pos())
                                    instanceof WENDataMonitorBlockEntity monitor)
                                monitor.syncTo(player);
                        });
        context.get().setPacketHandled(true);
    }
}
