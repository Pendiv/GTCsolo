package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENDataMonitorMenu;
import DIV.gtcsolo.machine.wen.WENMonitorData;
import DIV.gtcsolo.machine.wen.WENNetworkData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.function.Supplier;

public record WENMonitorUpdatePacket(int menuId, BlockPos pos, WENMonitorData data) {
    public static final int MAX_NUMBER_DIGITS = 256;

    public static void writeNumber(FriendlyByteBuf buffer, BigInteger value) {
        buffer.writeUtf(value.toString(), MAX_NUMBER_DIGITS);
    }

    public static BigInteger readNumber(FriendlyByteBuf buffer) {
        String value = buffer.readUtf(MAX_NUMBER_DIGITS);
        if (!value.matches("[0-9]+"))
            throw new IllegalArgumentException("Invalid WEN energy value");
        return new BigInteger(value);
    }

    private static int readCount(FriendlyByteBuf buffer, int maximum) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum)
            throw new IllegalArgumentException("Invalid WEN collection length");
        return count;
    }

    public static void writeData(FriendlyByteBuf buffer, WENMonitorData data) {
        buffer.writeUtf(data.networkId(), 64);
        writeNumber(buffer, data.stored());
        writeNumber(buffer, data.capacity());
        buffer.writeBoolean(data.formed());
        buffer.writeLong(data.inputPerSecond());
        buffer.writeLong(data.outputPerSecond());
        buffer.writeBoolean(data.operator());
        buffer.writeBoolean(data.crossDimension());
        buffer.writeVarInt(data.storageLevel());
        buffer.writeVarInt(data.nextUpgradeCost());
        buffer.writeVarInt(data.page());
        buffer.writeVarInt(data.networkOffset());
        buffer.writeVarInt(data.networkCount());
        buffer.writeVarInt(data.history().size());
        for (var value : data.history()) writeNumber(buffer, value);
        buffer.writeVarInt(data.networks().size());
        for (var network : data.networks()) {
            buffer.writeUtf(network.id(), 64);
            buffer.writeBoolean(network.formed());
            writeNumber(buffer, network.stored());
            writeNumber(buffer, network.capacity());
            buffer.writeUtf(network.dimension(), 128);
        }
    }

    public static WENMonitorData readData(FriendlyByteBuf buffer) {
        String id = buffer.readUtf(64);
        BigInteger stored = readNumber(buffer), capacity = readNumber(buffer);
        boolean formed = buffer.readBoolean();
        long input = buffer.readLong(), output = buffer.readLong();
        boolean operator = buffer.readBoolean(), crossDimension = buffer.readBoolean();
        int storageLevel = buffer.readVarInt(),
                nextCost = buffer.readVarInt(),
                page = buffer.readVarInt();
        int offset = buffer.readVarInt(), count = buffer.readVarInt();
        int historyCount = readCount(buffer, WENNetworkData.HISTORY_SIZE);
        var history = new ArrayList<BigInteger>(historyCount);
        for (int i = 0; i < historyCount; i++) history.add(readNumber(buffer));
        int networkCount = readCount(buffer, WENMonitorData.NETWORK_ROWS);
        var networks = new ArrayList<WENMonitorData.NetworkInfo>(networkCount);
        for (int i = 0; i < networkCount; i++)
            networks.add(
                    new WENMonitorData.NetworkInfo(
                            buffer.readUtf(64),
                            buffer.readBoolean(),
                            readNumber(buffer),
                            readNumber(buffer),
                            buffer.readUtf(128)));
        return new WENMonitorData(
                id,
                stored,
                capacity,
                formed,
                input,
                output,
                history,
                networks,
                offset,
                count,
                operator,
                crossDimension,
                storageLevel,
                nextCost,
                page);
    }

    public static void encode(WENMonitorUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.menuId());
        buffer.writeBlockPos(packet.pos());
        writeData(buffer, packet.data());
    }

    public static WENMonitorUpdatePacket decode(FriendlyByteBuf buffer) {
        return new WENMonitorUpdatePacket(
                buffer.readVarInt(), buffer.readBlockPos(), readData(buffer));
    }

    public static void handle(
            WENMonitorUpdatePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get()
                .enqueueWork(
                        () ->
                                DistExecutor.unsafeRunWhenOn(
                                        Dist.CLIENT, () -> () -> applyClient(packet)));
        context.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyClient(WENMonitorUpdatePacket packet) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null
                && player.containerMenu instanceof WENDataMonitorMenu menu
                && menu.containerId == packet.menuId()
                && menu.getMonitorPos().equals(packet.pos())) menu.updateData(packet.data());
    }
}
