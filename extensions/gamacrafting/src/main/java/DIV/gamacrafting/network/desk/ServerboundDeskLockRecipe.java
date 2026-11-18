package DIV.gamacrafting.network.desk;

import DIV.gamacrafting.Gamacrafting;
import DIV.gamacrafting.content.crafting.desk.DeskMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundDeskLockRecipe(int id, boolean lock) {
    public static void encode(ServerboundDeskLockRecipe message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.id);
        buf.writeBoolean(message.lock);
    }

    public static ServerboundDeskLockRecipe decode(FriendlyByteBuf buf) {
        return new ServerboundDeskLockRecipe(buf.readVarInt(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.containerMenu instanceof DeskMenu menu && menu.containerId == this.id
                && menu.stillValid(player)) {
            menu.access.execute((level, pos) -> level.getBlockEntity(pos, Gamacrafting.DESK_BLOCK_ENTITY.get()).ifPresent(desk -> {
                if (this.lock) {
                    desk.lockCurrentRecipe();
                } else {
                    desk.clearLock();
                }
            }));
        }
    }
}
