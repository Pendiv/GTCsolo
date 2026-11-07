package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.block.wen.WENDataMonitorMenu;
import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.math.BigInteger;
import java.util.function.Supplier;

public record WENAdminPacket(
        BlockPos monitorPos, Action action, String targetId, BigInteger value) {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public enum Action {
        DELETE_NETWORK,
        SET_ENERGY,
        SET_CAPACITY
    }

    public static void encode(WENAdminPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.monitorPos());
        buffer.writeEnum(packet.action());
        buffer.writeUtf(packet.targetId(), 64);
        WENMonitorUpdatePacket.writeNumber(buffer, packet.value());
    }

    public static WENAdminPacket decode(FriendlyByteBuf buffer) {
        return new WENAdminPacket(
                buffer.readBlockPos(),
                buffer.readEnum(Action.class),
                buffer.readUtf(64),
                WENMonitorUpdatePacket.readNumber(buffer));
    }

    public static void handle(WENAdminPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            var player = context.get().getSender();
                            if (player == null
                                    || !(player.containerMenu instanceof WENDataMonitorMenu menu)
                                    || !menu.getMonitorPos().equals(packet.monitorPos())
                                    || !menu.stillValid(player)) return;
                            if (!player.hasPermissions(2)) {
                                player.sendSystemMessage(
                                        Component.translatable("gui.gtcsolo.wen.admin_only")
                                                .withStyle(ChatFormatting.RED));
                                return;
                            }
                            WENNetworkData data = WENNetworkData.get(player.server.overworld());
                            if (!data.hasNetwork(packet.targetId())) {
                                player.sendSystemMessage(
                                        Component.translatable(
                                                        "gui.gtcsolo.wen.network_missing",
                                                        packet.targetId())
                                                .withStyle(ChatFormatting.RED));
                                return;
                            }
                            switch (packet.action()) {
                                case DELETE_NETWORK -> data.removeNetwork(packet.targetId());
                                case SET_ENERGY ->
                                        data.setEnergy(packet.targetId(), packet.value());
                                case SET_CAPACITY ->
                                        data.setCapacity(packet.targetId(), packet.value());
                            }
                            LOGGER.info(
                                    "[WEN Admin] {} {} '{}' {}",
                                    player.getGameProfile().getName(),
                                    packet.action(),
                                    packet.targetId(),
                                    packet.value());
                            player.sendSystemMessage(
                                    Component.translatable(
                                                    "gui.gtcsolo.wen.admin."
                                                            + packet.action()
                                                                    .name()
                                                                    .toLowerCase(
                                                                            java.util.Locale.ROOT),
                                                    packet.targetId(),
                                                    EnergyFormat.format(packet.value()))
                                            .withStyle(ChatFormatting.GREEN));
                            if (player.level().getBlockEntity(packet.monitorPos())
                                    instanceof WENDataMonitorBlockEntity monitor)
                                monitor.forceSyncNow();
                        });
        context.get().setPacketHandled(true);
    }
}
