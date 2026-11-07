package DIV.gtcsolo.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Server-to-client only. Carries the layout, never inventories or other block-entity contents. */
public record CopyPreviewPacket(BlockPos origin, Direction facing, CompoundTag layout) {
    public static void encode(CopyPreviewPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.origin); buffer.writeEnum(packet.facing); buffer.writeNbt(packet.layout);
    }
    public static CopyPreviewPacket decode(FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        var facing = buffer.readEnum(Direction.class);
        var data = buffer.readNbt();
        if (data == null || facing.getAxis() == Direction.Axis.Y) throw new IllegalArgumentException("Invalid copy preview");
        return new CopyPreviewPacket(pos, facing, data);
    }
    public static void handle(CopyPreviewPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DIV.gtcsolo.client.CopyPreview.show(packet)));
        context.get().setPacketHandled(true);
    }
}
