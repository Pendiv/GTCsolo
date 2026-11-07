package DIV.gtcsolo.client;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.VoltageClocks;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VoltageClockClientEvents {
    private VoltageClockClientEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation time = new ResourceLocation("minecraft", "time");
            ItemPropertyFunction vanillaTime = Objects.requireNonNull(ItemProperties.getProperty(Items.CLOCK, time),
                    "Vanilla clock time property is missing");
            // Vanilla already shares one wobble state across its clocks. Reusing that same
            // property preserves hand/frame/world lookup and random spinning in non-natural dimensions.
            VoltageClocks.ENTRIES.forEach(entry -> ItemProperties.register(entry.item().get(), time, vanillaTime));
        });
    }
}
