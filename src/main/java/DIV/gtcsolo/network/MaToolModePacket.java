package DIV.gtcsolo.network;

import DIV.gtcsolo.registry.tool.fullcustom.MaCrystalToolItem;
import DIV.gtcsolo.registry.tool.fullcustom.MaToolMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Supplier;

/** Only changes the requested mode on the sender's currently held MA tool. */
public record MaToolModePacket(int slot, ResourceLocation item, int mode) {
    public static void encode(MaToolModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.slot);
        buffer.writeResourceLocation(packet.item);
        buffer.writeByte(packet.mode);
    }
    public static MaToolModePacket decode(FriendlyByteBuf buffer) {
        return new MaToolModePacket(buffer.readUnsignedByte(), buffer.readResourceLocation(), buffer.readUnsignedByte());
    }
    public static void handle(MaToolModePacket packet, Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && apply(player, packet))
                player.displayClientMessage(Component.translatable("gtcsolo.ma_tool.mode", MaToolMode.values()[packet.mode].label()), true);
        });
        ctx.setPacketHandled(true);
    }
    public static boolean apply(ServerPlayer player, MaToolModePacket packet) {
        if (!player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu
                || packet.slot < 0 || packet.slot > 8 || player.getInventory().selected != packet.slot
                || packet.mode < 0 || packet.mode >= MaToolMode.values().length) return false;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MaCrystalToolItem)
                || !packet.item.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) return false;
        // Shift is a client gesture, not a permission; requiring the crouch packet here introduces a race.
        MaToolMode.set(stack, MaToolMode.values()[packet.mode]);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }
}
