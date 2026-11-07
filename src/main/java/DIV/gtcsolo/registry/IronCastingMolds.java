package DIV.gtcsolo.registry;

import DIV.gtcsolo.item.IronCastingMoldItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IronCastingMolds {
    public static final String[] SHAPES = {"plate", "gear", "credit", "bottle", "ingot", "ball", "block",
            "nugget", "cylinder", "anvil", "name", "small_gear", "rotor", "pill"};
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gtcsolo");
    public static final RegistryObject<Item> EMPTY = ITEMS.register("iron_empty_mold", () -> new Item(new Item.Properties()));
    public static final Map<String, RegistryObject<IronCastingMoldItem>> MOLDS = new LinkedHashMap<>();
    static {
        for (String shape : SHAPES) MOLDS.put(shape, ITEMS.register("iron_" + shape + "_casting_mold",
                () -> new IronCastingMoldItem(new Item.Properties())));
    }
    private IronCastingMolds() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
    public static Item original(String shape) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", shape + "_casting_mold"));
    }
    public static String shapeOf(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !id.getNamespace().equals("gtceu")) return null;
        for (String shape : SHAPES)
            if (id.getPath().equals(shape + "_casting_mold") && MOLDS.get(shape).isPresent()) return shape;
        return null;
    }
    public static void addToCreativeTab(CreativeModeTab.Output output) {
        output.accept(EMPTY.get());
        MOLDS.values().forEach(entry -> output.accept(entry.get()));
    }
}
