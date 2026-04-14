package DIV.gtcsolo.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnergyCubeUpdatePacket {

    private final BlockPos pos;
    private final long stored;
    private final boolean emitMode;
    private final boolean euMode;

    public EnergyCubeUpdatePacket(BlockPos pos, long stored, boolean emitMode, boolean euMode) {
        this.pos = pos;
        this.stored = stored;
        this.emitMode = emitMode;
        this.euMode = euMode;
    }

    public static void encode(EnergyCubeUpdatePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeLong(packet.stored);
        buf.writeBoolean(packet.emitMode);
        buf.writeBoolean(packet.euMode);
    }

    public static EnergyCubeUpdatePacket decode(FriendlyByteBuf buf) {
        return new EnergyCubeUpdatePacket(
                buf.readBlockPos(),
                buf.readLong(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void handle(EnergyCubeUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(EnergyCubeUpdatePacket packet) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof DIV.gtcsolo.block.ExtendEnergyCubeScreen screen
                && screen.getMenu().getBlockPos().equals(packet.pos)) {
            screen.getMenu().updateData(packet.stored, packet.emitMode, packet.euMode);
        }
    }
}