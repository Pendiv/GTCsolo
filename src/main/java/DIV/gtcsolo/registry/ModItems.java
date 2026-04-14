package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.world.item.ArmorItem;
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

    public static final RegistryObject<Item> BEDROCKIUM_HE_CASING =
            ITEMS.register("bedrockium_he_casing",
                    () -> new BlockItem(ModBlocks.BEDROCKIUM_HE_CASING.get(), new Item.Properties())
            );

    public static final RegistryObject<Item> PRIEST_AXE =
            ITEMS.register("priest_axe",
                    () -> new PriestAxeItem(
                            PriestAxeItem.CUSTOM_TIER,
                            new Item.Properties()
                    )
            );
    // =========================================================================
    //  新規アイテム (仮名: item1~item5)
    // =========================================================================

    /** item1 — テクスチャ: ingot.png */
    public static final RegistryObject<Item> ITEM1 =
            ITEMS.register("item1", () -> new Item(new Item.Properties()));

    /** 永夜の結晶 — テクスチャ: moonnight_rune.png — WENアップグレード用 */
    public static final RegistryObject<Item> EVERNIGHT_CRYSTAL =
            ITEMS.register("evernight_crystal", () -> new Item(new Item.Properties()));

    /** item3 — テクスチャ: refined_netherstar_ingot.png */
    public static final RegistryObject<Item> ITEM3 =
            ITEMS.register("item3", () -> new Item(new Item.Properties()));

    /** item4 — テクスチャ: refined_netherstar_sword.png */
    public static final RegistryObject<Item> ITEM4 =
            ITEMS.register("item4", () -> new Item(new Item.Properties()));

    /** item5 — テクスチャ: refined_netherstar_pickaxe.png */
    public static final RegistryObject<Item> ITEM5 =
            ITEMS.register("item5", () -> new Item(new Item.Properties()));

    // =========================================================================
    //  WEN (Wireless Energy Network) ブロックアイテム
    // =========================================================================

    public static final RegistryObject<Item> WEN_MAINSTORAGE_CASING_ITEM =
            ITEMS.register("wen_mainstorage_casing",
                    () -> new BlockItem(ModBlocks.WEN_MAINSTORAGE_CASING.get(), new Item.Properties()));

    public static final RegistryObject<Item> WEN_MAINSTORAGE_OUTPUT_PORT_ITEM =
            ITEMS.register("wen_mainstorage_output_port",
                    () -> new BlockItem(ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get(), new Item.Properties()));

    public static final RegistryObject<Item> WEN_MAINSTORAGE_INPUT_PORT_ITEM =
            ITEMS.register("wen_mainstorage_input_port",
                    () -> new BlockItem(ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get(), new Item.Properties()));

    public static final RegistryObject<Item> WEN_BASIC_ENERGY_CELL_ITEM =
            ITEMS.register("wen_basic_energy_cell",
                    () -> new BlockItem(ModBlocks.WEN_BASIC_ENERGY_CELL.get(), new Item.Properties()));

    // =========================================================================
    //  その他ブロックアイテム (仮名)
    // =========================================================================

    /** block4 — テクスチャ: cryostat_glass.png */
    public static final RegistryObject<Item> BLOCK4_ITEM =
            ITEMS.register("block4", () -> new BlockItem(ModBlocks.BLOCK4.get(), new Item.Properties()));

    /** block6 — テクスチャ: nb3sn_coil.png */
    public static final RegistryObject<Item> BLOCK6_ITEM =
            ITEMS.register("block6", () -> new BlockItem(ModBlocks.BLOCK6.get(), new Item.Properties()));

    public static final RegistryObject<Item> WEN_DATA_MONITOR_ITEM =
            ITEMS.register("wen_data_monitor",
                    () -> new BlockItem(ModBlocks.WEN_DATA_MONITOR.get(), new Item.Properties()));

    /** block8 — テクスチャ: tungsten_wall.png */
    public static final RegistryObject<Item> BLOCK8_ITEM =
            ITEMS.register("block8", () -> new BlockItem(ModBlocks.BLOCK8.get(), new Item.Properties()));

    /** block9 — テクスチャ: ore_smelter_controller.png */
    public static final RegistryObject<Item> BLOCK9_ITEM =
            ITEMS.register("block9", () -> new BlockItem(ModBlocks.BLOCK9.get(), new Item.Properties()));

    /** block10 — テクスチャ: refined_netherstar_block.png */
    public static final RegistryObject<Item> BLOCK10_ITEM =
            ITEMS.register("block10", () -> new BlockItem(ModBlocks.BLOCK10.get(), new Item.Properties()));

    // =========================================================================
    //  新規防具 (仮名: armor1 helmet/chestplate/leggings/boots)
    //  テクスチャ: refined_netherstar_*.png, layer_1/layer_2
    // =========================================================================

    public static final RegistryObject<Item> ARMOR1_HELMET =
            ITEMS.register("armor1_helmet", () -> new ArmorItem(
                    ModArmorMaterial.PLACEHOLDER, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> ARMOR1_CHESTPLATE =
            ITEMS.register("armor1_chestplate", () -> new ArmorItem(
                    ModArmorMaterial.PLACEHOLDER, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> ARMOR1_LEGGINGS =
            ITEMS.register("armor1_leggings", () -> new ArmorItem(
                    ModArmorMaterial.PLACEHOLDER, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> ARMOR1_BOOTS =
            ITEMS.register("armor1_boots", () -> new ArmorItem(
                    ModArmorMaterial.PLACEHOLDER, ArmorItem.Type.BOOTS, new Item.Properties()));
}