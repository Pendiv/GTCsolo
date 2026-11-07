package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.torch.VoltageTorchTier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Voltage-themed clocks. Their time display is supplied by vanilla's client property. */
public final class VoltageClocks {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Gtcsolo.MODID);

    public record Entry(VoltageTorchTier tier, RegistryObject<Item> item) {}

    public static final List<Entry> ENTRIES = registerTiers();

    private VoltageClocks() {}

    private static List<Entry> registerTiers() {
        List<Entry> entries = new ArrayList<>();
        for (VoltageTorchTier tier : VoltageTorchTier.values()) {
            RegistryObject<Item> item = ITEMS.register("voltage_clock_" + tier.id(),
                    () -> new Item(new Item.Properties()));
            entries.add(new Entry(tier, item));
        }
        return List.copyOf(entries);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static void addToCreativeTab(CreativeModeTab.Output output) {
        ENTRIES.forEach(entry -> output.accept(entry.item().get()));
    }
}
