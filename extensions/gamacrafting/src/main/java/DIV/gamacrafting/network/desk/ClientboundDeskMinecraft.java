package DIV.gamacrafting.network.desk;

import DIV.gamacrafting.content.crafting.desk.DeskHandler;
import DIV.gamacrafting.content.crafting.desk.DeskSavedData;
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

public record ClientboundDeskMinecraft(ResourceKey<Level> dimension, DeskSavedData.Entry entry) {
    public static void encode(ClientboundDeskMinecraft message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.dimension.location());
        buf.writeNbt(message.entry.save());
    }

    public static ClientboundDeskMinecraft decode(FriendlyByteBuf buf) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        CompoundTag tag = buf.readNbt();
        if (tag == null) {
            throw new DecoderException("Missing Gamacrafting desk animation data");
        }
        return new ClientboundDeskMinecraft(dimension, DeskSavedData.Entry.load(tag));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DeskHandler.Client.addAnimation(this.dimension, this.entry));
    }
}
