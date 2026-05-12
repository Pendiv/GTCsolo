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

    /** R-HICC ガラス — テクスチャ: cryostat_glass.png */
    public static final RegistryObject<Item> R_HICC_GLASS_ITEM =
            ITEMS.register("r_hicc_glass", () -> new BlockItem(ModBlocks.R_HICC_GLASS.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                            net.minecraft.world.level.Level level,
                                            java.util.List<net.minecraft.network.chat.Component> tooltip,
                                            net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("block.gtcsolo.r_hicc_glass.tooltip.1")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                    super.appendHoverText(stack, level, tooltip, flag);
                }
            });

    /** block6 — テクスチャ: nb3sn_coil.png */
    public static final RegistryObject<Item> BLOCK6_ITEM =
            ITEMS.register("block6", () -> new BlockItem(ModBlocks.BLOCK6.get(), new Item.Properties()));

    public static final RegistryObject<Item> WEN_DATA_MONITOR_ITEM =
            ITEMS.register("wen_data_monitor",
                    () -> new BlockItem(ModBlocks.WEN_DATA_MONITOR.get(), new Item.Properties()));

    /** 壊滅コアブロック — StarForge controller ベース。テクスチャ: tungsten_wall.png */
    public static final RegistryObject<Item> DEVASTATION_CORE_BLOCK_ITEM =
            ITEMS.register("devastation_core_block", () -> new BlockItem(ModBlocks.DEVASTATION_CORE_BLOCK.get(), new Item.Properties()));

    /** block9 — テクスチャ: ore_smelter_controller.png */
    public static final RegistryObject<Item> RESONANCE_CONTROL_CORE_BLOCK_ITEM =
            ITEMS.register("resonance_control_core_block", () -> new BlockItem(ModBlocks.RESONANCE_CONTROL_CORE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> AMEIJIA_MAKER_ITEM =
            ITEMS.register("ameijia_maker", () -> new BlockItem(ModBlocks.AMEIJIA_MAKER.get(), new Item.Properties()));

    /** block10 — テクスチャ: refined_netherstar_block.png */
    public static final RegistryObject<Item> BLOCK10_ITEM =
            ITEMS.register("block10", () -> new BlockItem(ModBlocks.BLOCK10.get(), new Item.Properties()));

    public static final RegistryObject<Item> SIMULATION_CASING_ITEM =
            ITEMS.register("simulation_casing", () -> new BlockItem(ModBlocks.SIMULATION_CASING.get(), new Item.Properties()));

    // casingblock_n — 仮追加ケーシング群
    public static final RegistryObject<Item> AURORALIUM_RESONANCE_CASING_ITEM =
            ITEMS.register("auroralium_resonance_casing", () -> new BlockItem(ModBlocks.AURORALIUM_RESONANCE_CASING.get(), new Item.Properties()));
    public static final RegistryObject<Item> BEDROCKIUM_NOCTURNIUM_FUSION_CASING_ITEM =
            ITEMS.register("bedrockium_nocturnium_fusion_casing", () -> new BlockItem(ModBlocks.BEDROCKIUM_NOCTURNIUM_FUSION_CASING.get(), new Item.Properties()));
    public static final RegistryObject<Item> CASINGBLOCK_3_ITEM =
            ITEMS.register("casingblock_3", () -> new BlockItem(ModBlocks.CASINGBLOCK_3.get(), new Item.Properties()));
    public static final RegistryObject<Item> AURORALIUM_STARFORGE_CASING_ITEM =
            ITEMS.register("auroralium_starforge_casing", () -> new BlockItem(ModBlocks.AURORALIUM_STARFORGE_CASING.get(), new Item.Properties()));
    public static final RegistryObject<Item> WEN_FUNCTIONAL_ASSEMBLER_MACHINE_CASING_ITEM =
            ITEMS.register("wen_functional_assembler_machine_casing", () -> new BlockItem(ModBlocks.WEN_FUNCTIONAL_ASSEMBLER_MACHINE_CASING.get(), new Item.Properties()));
    public static final RegistryObject<Item> REFINED_OBSIDIAN_CASING_ITEM =
            ITEMS.register("refined_obsidian_casing", () -> new BlockItem(ModBlocks.REFINED_OBSIDIAN_CASING.get(), new Item.Properties()));
    public static final RegistryObject<Item> ETERNAL_CASING_ITEM =
            ITEMS.register("eternal_casing", () -> new BlockItem(ModBlocks.ETERNAL_CASING.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHEMICAL_RESISTANT_CASING_ITEM =
            ITEMS.register("chemical_resistant_casing", () -> new BlockItem(ModBlocks.CHEMICAL_RESISTANT_CASING.get(), new Item.Properties()));

    public static final RegistryObject<Item> HTFF_CASING_ITEM =
            ITEMS.register("htff_casing", () -> new BlockItem(ModBlocks.HTFF_CASING.get(), new Item.Properties()));

    /** タイムアップグレード — UpgradeHatch内に格納するとレシピ稼働時間を短縮 (stacksTo=1)
     *  テクスチャ: item/upgrade/time_upgrade.png */
    public static final RegistryObject<Item> TIME_UPGRADE =
            ITEMS.register("time_upgrade",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    /** パラレルアップグレード — UpgradeHatch内に格納するとマルチブロックの並列数を増加 (stacksTo=1, ロジック未実装)
     *  テクスチャ: item/upgrade/parallel_upgrade.png */
    public static final RegistryObject<Item> PARALLEL_UPGRADE =
            ITEMS.register("parallel_upgrade",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    /** pc — アニメーションテクスチャ (pc.png + pc.png.mcmeta, 10 frame ping-pong) */
    public static final RegistryObject<Item> PC =
            ITEMS.register("pc", () -> new Item(new Item.Properties()));

    // =========================================================================
    //  AE2統合用アイテム・ブロックアイテム (仮登録・未統合)
    // =========================================================================

    /** AE2版 WEN入力ポート (BlockItem) */
    public static final RegistryObject<Item> WEN_AE_INPUT_PORT_ITEM =
            ITEMS.register("wen_ae_inputport",
                    () -> new BlockItem(ModBlocks.WEN_AE_INPUT_PORT.get(), new Item.Properties()));

    /** AE2版 WEN出力ポート (BlockItem) */
    public static final RegistryObject<Item> WEN_AE_OUTPUT_PORT_ITEM =
            ITEMS.register("wen_ae_outputport",
                    () -> new BlockItem(ModBlocks.WEN_AE_OUTPUT_PORT.get(), new Item.Properties()));

    /** FE版 WEN入力ポート (BlockItem) */
    public static final RegistryObject<Item> WEN_FE_INPUT_PORT_ITEM =
            ITEMS.register("wen_fe_inputport",
                    () -> new BlockItem(ModBlocks.WEN_FE_INPUT_PORT.get(), new Item.Properties()));

    /** FE版 WEN出力ポート (BlockItem) */
    public static final RegistryObject<Item> WEN_FE_OUTPUT_PORT_ITEM =
            ITEMS.register("wen_fe_outputport",
                    () -> new BlockItem(ModBlocks.WEN_FE_OUTPUT_PORT.get(), new Item.Properties()));

    /** WEN ワイヤレスエネルギーカード — テクスチャ: item/ae2/wen_wireless_energycard.png
     *  AE2アップグレードスロットに挿入するとそのAE2網全体がWENから無制限給電される */
    public static final RegistryObject<Item> WEN_WIRELESS_ENERGYCARD =
            ITEMS.register("wen_wireless_energycard",
                    () -> new DIV.gtcsolo.integration.ae2.WENEnergyCardItem(
                            new Item.Properties().stacksTo(16)));

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

    // =========================================================================
    //  exclamation/ 系仮素材
    // =========================================================================

    /** old_glass ブロックアイテム — テクスチャ: exclamation/old_glass.png */
    public static final RegistryObject<Item> OLD_GLASS_ITEM =
            ITEMS.register("old_glass",
                    () -> new BlockItem(ModBlocks.OLD_GLASS.get(), new Item.Properties()));

    /** unknown ブロックアイテム — テクスチャ: exclamation/unknown.png */
    public static final RegistryObject<Item> UNKNOWN_ITEM =
            ITEMS.register("unknown",
                    () -> new BlockItem(ModBlocks.UNKNOWN.get(), new Item.Properties()));

    /** water — 設置すると水源になるアイテム (snowball 系の挙動)。stack 64。 */
    public static final RegistryObject<Item> WATER =
            ITEMS.register("water",
                    () -> new DIV.gtcsolo.item.FluidPlacingItem(
                            net.minecraft.world.level.material.Fluids.WATER,
                            new Item.Properties()));

    /** magma — 設置すると溶岩源になるアイテム。stack 64。 */
    public static final RegistryObject<Item> MAGMA =
            ITEMS.register("magma",
                    () -> new DIV.gtcsolo.item.FluidPlacingItem(
                            net.minecraft.world.level.material.Fluids.LAVA,
                            new Item.Properties()));

    // =========================================================================
    //  StarForge / 星の軌跡
    // =========================================================================

    /** 星の軌跡 — テクスチャ: star_locus.png
     *  NBT "Trace" で 8 種類の軌跡を識別。空状態 (NBT 未設定) は観測機構で書き込まれる。 */
    public static final RegistryObject<Item> STAR_LOCUS =
            ITEMS.register("star_locus",
                    () -> new DIV.gtcsolo.item.StarLocusItem(
                            new Item.Properties().stacksTo(1)));

    /** 朽ち果てた星の軌跡 — テクスチャ: decaying_star_locus.png (未配置、追加予定)
     *  StarForge の崩壊フェイズで出力される副産物。locus_simulation_builder で次 tier 軌跡を作る触媒
     *  (消費されない、stack 64 で持ち歩き可)。NBT "Trace" は star_locus と同仕様。 */
    public static final RegistryObject<Item> DECAYING_STAR_LOCUS =
            ITEMS.register("decaying_star_locus",
                    () -> new DIV.gtcsolo.item.DecayingStarLocusItem(
                            new Item.Properties()));

}