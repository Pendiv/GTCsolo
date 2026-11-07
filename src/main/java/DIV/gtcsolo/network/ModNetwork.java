package DIV.gtcsolo.network;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.network.wen.WENAdminPacket;
import DIV.gtcsolo.network.wen.WENMonitorUpdatePacket;
import DIV.gtcsolo.network.wen.WENSetIdPacket;
import DIV.gtcsolo.network.wen.WENSelectIdPacket;
import DIV.gtcsolo.network.wen.WENUpgradePacket;
import DIV.gtcsolo.network.wen.WENMonitorViewPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL = "7";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Gtcsolo.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, EnergyCubeUpdatePacket.class,
                EnergyCubeUpdatePacket::encode,
                EnergyCubeUpdatePacket::decode,
                EnergyCubeUpdatePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, EnergyCubeSwitchPacket.class,
                EnergyCubeSwitchPacket::encode,
                EnergyCubeSwitchPacket::decode,
                EnergyCubeSwitchPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, WENSetIdPacket.class,
                WENSetIdPacket::encode,
                WENSetIdPacket::decode,
                WENSetIdPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, WENMonitorUpdatePacket.class,
                WENMonitorUpdatePacket::encode,
                WENMonitorUpdatePacket::decode,
                WENMonitorUpdatePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, WENAdminPacket.class,
                WENAdminPacket::encode,
                WENAdminPacket::decode,
                WENAdminPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, WENUpgradePacket.class,
                WENUpgradePacket::encode,
                WENUpgradePacket::decode,
                WENUpgradePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, WENSelectIdPacket.class,
                WENSelectIdPacket::encode,
                WENSelectIdPacket::decode,
                WENSelectIdPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CopyPreviewPacket.class,
                CopyPreviewPacket::encode, CopyPreviewPacket::decode, CopyPreviewPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, MaToolModePacket.class,
                MaToolModePacket::encode, MaToolModePacket::decode, MaToolModePacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, WENMonitorViewPacket.class,
                WENMonitorViewPacket::encode, WENMonitorViewPacket::decode, WENMonitorViewPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
    }
}
