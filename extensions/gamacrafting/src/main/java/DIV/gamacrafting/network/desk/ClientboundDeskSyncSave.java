package DIV.gamacrafting.network.desk;

import DIV.gamacrafting.content.crafting.desk.DeskHandler;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundDeskSyncSave(ResourceKey<Level> dimension, CompoundTag tag) {
    public static void encode(ClientboundDeskSyncSave message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.dimension.location());
        buf.writeNbt(message.tag);
    }

    public static ClientboundDeskSyncSave decode(FriendlyByteBuf buf) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        CompoundTag tag = buf.readNbt();
        if (tag == null) {
            throw new DecoderException("Missing Gamacrafting desk save data");
        }
        return new ClientboundDeskSyncSave(dimension, tag);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DeskHandler.Client.syncSave(this.dimension, this.tag));
    }
}
