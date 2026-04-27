package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.ExtendEnergyCubeBlock;
import DIV.gtcsolo.block.wen.WENDataMonitorBlock;
import DIV.gtcsolo.block.wen.WENPortBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Gtcsolo.MODID);

    public static final RegistryObject<Block> HIGH_STRENGTH_STEEL_CASING =
            BLOCKS.register("high_strength_steel_casing",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .strength(5.0f, 12.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                    )
            );

    public static final RegistryObject<Block> EXTEND_ENERGY_CUBE =
            BLOCKS.register("extend_energy_cube", ExtendEnergyCubeBlock::new);

    public static final RegistryObject<Block> BEDROCKIUM_HE_CASING =
            BLOCKS.register("bedrockium_he_casing",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .strength(5.0f, 12.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                    )
            );

    // =========================================================================
    //  WEN (Wireless Energy Network) ブロック群
    // =========================================================================

    private static RegistryObject<Block> simpleBlock(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .strength(5.0f, 12.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()));
    }

    /** WEN主要外装ケーシング — テクスチャ: cryostat_casing.png */
    public static final RegistryObject<Block> WEN_MAINSTORAGE_CASING = simpleBlock("wen_mainstorage_casing");
    /** WEN出力ポート（機能外装・FE Capability付き） — テクスチャ: cryogenic_port_output.png */
    public static final RegistryObject<Block> WEN_MAINSTORAGE_OUTPUT_PORT =
            BLOCKS.register("wen_mainstorage_output_port", WENPortBlock::new);
    /** WEN入力ポート（機能外装・FE Capability付き） — テクスチャ: cryogenic_port_input.png */
    public static final RegistryObject<Block> WEN_MAINSTORAGE_INPUT_PORT =
            BLOCKS.register("wen_mainstorage_input_port", WENPortBlock::new);
    /** WEN基本蓄電セル（内部機能ブロック） — テクスチャ: neutron_sheild_block.png */
    public static final RegistryObject<Block> WEN_BASIC_ENERGY_CELL = simpleBlock("wen_basic_energy_cell");

    // =========================================================================
    //  その他ブロック (仮名)
    // =========================================================================

    /** R-HICC ガラス (耐熱耐衝撃耐冷耐化学薬品) — テクスチャ: cryostat_glass.png */
    public static final RegistryObject<Block> R_HICC_GLASS = BLOCKS.register("r_hicc_glass",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 12.0f)
                    .sound(SoundType.GLASS)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false)
                    .isValidSpawn((s, l, p, t) -> false)
                    .isViewBlocking((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)));
    /** block6 — テクスチャ: nb3sn_coil.png */
    public static final RegistryObject<Block> BLOCK6  = simpleBlock("block6");
    /** WENデータモニター（機能外装・BlockEntity付き） — テクスチャ: cryogenic_monitor.png */
    public static final RegistryObject<Block> WEN_DATA_MONITOR =
            BLOCKS.register("wen_data_monitor", WENDataMonitorBlock::new);
    /** block8 — テクスチャ: tungsten_wall.png */
    public static final RegistryObject<Block> BLOCK8  = simpleBlock("block8");
    /** 共振制御コアブロック */
    public static final RegistryObject<Block> RESONANCE_CONTROL_CORE_BLOCK = simpleBlock("resonance_control_core_block");

    /** Ameijia 次元マーカー (鉱脈図での次元表示用) — テクスチャ: maker/ameijia_top.png (6面共通) */
    public static final RegistryObject<Block> AMEIJIA_MAKER = simpleBlock("ameijia_maker");
    /** block10 — テクスチャ: refined_netherstar_block.png */
    public static final RegistryObject<Block> BLOCK10 = simpleBlock("block10");

    // =========================================================================
    //  casingblock_n — 仮追加ケーシング群
    // =========================================================================

    public static final RegistryObject<Block> SIMULATION_CASING = simpleBlock("simulation_casing");
    public static final RegistryObject<Block> AURORALIUM_RESONANCE_CASING = simpleBlock("auroralium_resonance_casing");
    public static final RegistryObject<Block> BEDROCKIUM_NOCTURNIUM_FUSION_CASING = simpleBlock("bedrockium_nocturnium_fusion_casing");
    public static final RegistryObject<Block> CASINGBLOCK_3 = simpleBlock("casingblock_3");
    public static final RegistryObject<Block> CASINGBLOCK_4 = simpleBlock("casingblock_4");
    public static final RegistryObject<Block> CASINGBLOCK_5 = simpleBlock("casingblock_5");
    public static final RegistryObject<Block> REFINED_OBSIDIAN_CASING = simpleBlock("refined_obsidian_casing");
    public static final RegistryObject<Block> CASINGBLOCK_7 = simpleBlock("casingblock_7");

    // =========================================================================
    //  Chemical Combustion Generator 用ケーシング
    // =========================================================================

    public static final RegistryObject<Block> CHEMICAL_RESISTANT_CASING = simpleBlock("chemical_resistant_casing");

    // =========================================================================
    //  High-Pressure Alloy Blast Furnace 用ケーシング
    // =========================================================================

    /** HTFFケーシング — High-Pressure Alloy Blast Furnaceの内部ケーシング */
    public static final RegistryObject<Block> HTFF_CASING = simpleBlock("htff_casing");

    // =========================================================================
    //  AE2版 WEN ポート (仮登録・未統合)
    // =========================================================================

    /** AE2版 WEN入力ポート (AE2→WEN 変換、1 EU = 2 AE) */
    public static final RegistryObject<Block> WEN_AE_INPUT_PORT =
            BLOCKS.register("wen_ae_inputport", DIV.gtcsolo.block.wen.WENAePortBlock::new);
    /** AE2版 WEN出力ポート (WEN→AE2 変換、1 EU = 2 AE) */
    public static final RegistryObject<Block> WEN_AE_OUTPUT_PORT =
            BLOCKS.register("wen_ae_outputport", DIV.gtcsolo.block.wen.WENAePortBlock::new);

    /** FEワイヤレス入力ポート (FE→WEN 変換、1 EU = 4 FE) */
    public static final RegistryObject<Block> WEN_FE_INPUT_PORT =
            BLOCKS.register("wen_fe_inputport", DIV.gtcsolo.block.wen.WENFePortBlock::new);
    /** FEワイヤレス出力ポート (WEN→FE 変換、1 EU = 4 FE) */
    public static final RegistryObject<Block> WEN_FE_OUTPUT_PORT =
            BLOCKS.register("wen_fe_outputport", DIV.gtcsolo.block.wen.WENFePortBlock::new);
}