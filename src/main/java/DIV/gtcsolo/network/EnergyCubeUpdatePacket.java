package DIV.gtcsolo.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;
import java.util.function.Supplier;

public class EnergyCubeUpdatePacket {

    private final BlockPos pos;
    private final BigInteger stored;
    private final BigInteger outputRate;

    public EnergyCubeUpdatePacket(BlockPos pos, BigInteger stored, BigInteger outputRate) {
        this.pos = pos;
        this.stored = stored;
        this.outputRate = outputRate;
    }

    public static void encode(EnergyCubeUpdatePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.stored.toString());
        buf.writeUtf(packet.outputRate.toString());
    }

    public static EnergyCubeUpdatePacket decode(FriendlyByteBuf buf) {
        return new EnergyCubeUpdatePacket(
                buf.readBlockPos(),
                new BigInteger(buf.readUtf(512)),
                new BigInteger(buf.readUtf(512))
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
            screen.getMenu().updateData(packet.stored, packet.outputRate);
        }
    }
}