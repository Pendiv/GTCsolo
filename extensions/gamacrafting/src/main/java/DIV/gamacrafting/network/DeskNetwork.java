package DIV.gamacrafting.network;

import DIV.gamacrafting.Gamacrafting;
import DIV.gamacrafting.network.desk.ClientboundDeskMinecraft;
import DIV.gamacrafting.network.desk.ClientboundDeskSyncSave;
import DIV.gamacrafting.network.desk.ServerboundDeskDoneAnimation;
import DIV.gamacrafting.network.desk.ServerboundDeskLockRecipe;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DeskNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Gamacrafting.id("desk"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private DeskNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ClientboundDeskMinecraft.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundDeskMinecraft::encode)
                .decoder(ClientboundDeskMinecraft::decode)
                .consumerMainThread(ClientboundDeskMinecraft::handle)
                .add();
        CHANNEL.messageBuilder(ClientboundDeskSyncSave.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundDeskSyncSave::encode)
                .decoder(ClientboundDeskSyncSave::decode)
                .consumerMainThread(ClientboundDeskSyncSave::handle)
                .add();
        CHANNEL.messageBuilder(ServerboundDeskDoneAnimation.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundDeskDoneAnimation::encode)
                .decoder(ServerboundDeskDoneAnimation::decode)
                .consumerMainThread(ServerboundDeskDoneAnimation::handle)
                .add();
        CHANNEL.messageBuilder(ServerboundDeskLockRecipe.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundDeskLockRecipe::encode)
                .decoder(ServerboundDeskLockRecipe::decode)
                .consumerMainThread(ServerboundDeskLockRecipe::handle)
                .add();
    }
}
