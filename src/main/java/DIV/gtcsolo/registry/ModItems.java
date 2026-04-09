package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Gtcsolo.MODID);

    public static final RegistryObject<Item> XCRYSTAL =
            ITEMS.register("xcrystal",
                    () -> new Item(new Item.Properties())
            );

    public static final RegistryObject<Item> FGEAR =
            ITEMS.register("fgear",
                    () -> new Item(new Item.Properties())
            );

    public static final RegistryObject<Item> FCORE =
            ITEMS.register("fcore",
                    () -> new Item(new Item.Properties())
            );

    public static final RegistryObject<Item> HIGH_STRENGTH_STEEL_CASING =
            ITEMS.register("high_strength_steel_casing",
                    () -> new BlockItem(ModBlocks.HIGH_STRENGTH_STEEL_CASING.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> EXTEND_ENERGY_CUBE =
            ITEMS.register("extend_energy_cube",
                    () -> new BlockItem(ModBlocks.EXTEND_ENERGY_CUBE.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> PRIEST_AXE =
            ITEMS.register("priest_axe",
                    () -> new PriestAxeItem(
                            PriestAxeItem.CUSTOM_TIER,
                            new Item.Properties()
                    )
            );
    // 別タブに入れたい場合は以下のように書く（ANOTHER_TABは ModCreativeTabs に定義）:
    // static { ModCreativeTabs.assignTab(PRIEST_AXE, ModCreativeTabs.ANOTHER_TAB); }
}