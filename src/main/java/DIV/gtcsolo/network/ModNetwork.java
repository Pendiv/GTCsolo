package DIV.gtcsolo.network;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Gtcsolo.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, EnergyCubeUpdatePacket.class,
                EnergyCubeUpdatePacket::encode,
                EnergyCubeUpdatePacket::decode,
                EnergyCubeUpdatePacket::handle);
    }
}