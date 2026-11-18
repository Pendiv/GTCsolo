package DIV.gamacrafting.network.desk;

import DIV.gamacrafting.content.crafting.desk.DeskMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundDeskDoneAnimation(int id, boolean done) {
    public static void encode(ServerboundDeskDoneAnimation message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.id);
        buf.writeBoolean(message.done);
    }

    public static ServerboundDeskDoneAnimation decode(FriendlyByteBuf buf) {
        return new ServerboundDeskDoneAnimation(buf.readVarInt(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.containerMenu instanceof DeskMenu menu && menu.containerId == this.id
                && menu.stillValid(player)) {
            menu.canCraft = this.done;
        }
    }
}
