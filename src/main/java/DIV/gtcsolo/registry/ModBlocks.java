package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.ExtendEnergyCubeBlock;
import DIV.gtcsolo.block.datachest.DataChestBlock;
import DIV.gtcsolo.block.wen.WENDataMonitorBlock;
import DIV.gtcsolo.block.wen.WENPortBlock;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import DIV.gtcsolo.block.DraconicCoilTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Gtcsolo.MODID);

    public static final RegistryObject<Block> MITHRILITE_MACHINE_CASING = BLOCKS.register("mithrilite_machine_casing",
            () -> new Block(metalCasingProperties()));

    public static final RegistryObject<Block> DRACONIC_STEEL_GEARBOX = BLOCKS.register("draconic_steel_gearbox",
            () -> new Block(BlockBehaviour.Properties.of().strength(5.0f, 12.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DRACONIC_STEEL_PIPE_CASING = BLOCKS.register("draconic_steel_pipe_casing",
            () -> new Block(BlockBehaviour.Properties.of().strength(5.0f, 12.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DRACONIC_STEEL_CASING = BLOCKS.register("draconic_steel_casing",
            () -> new Block(BlockBehaviour.Properties.of().strength(5.0f, 12.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // Same block classes/properties as the corresponding GT1.6.4 tungstensteel casings.
    public static final RegistryObject<Block> MITHRILITE_ROBUST_MACHINE_CASING = BLOCKS.register("mithrilite_robust_machine_casing",
            () -> new Block(metalCasingProperties()));
    public static final RegistryObject<Block> MITHRILITE_PIPE_CASING = BLOCKS.register("mithrilite_pipe_casing",
            () -> new Block(metalCasingProperties()));
    public static final RegistryObject<Block> STAINLESS_STEEL_PIPE_CASING = BLOCKS.register("stainless_steel_pipe_casing",
            () -> new Block(metalCasingProperties()));
    public static final RegistryObject<ActiveBlock> STAINLESS_STEEL_ENGINE_INTAKE_CASING = BLOCKS.register("stainless_steel_engine_intake_casing",
            () -> new ActiveBlock(metalCasingProperties()));
    public static final RegistryObject<ActiveBlock> MITHRILITE_FIREBOX_CASING = BLOCKS.register("mithrilite_firebox_casing",
            () -> new ActiveBlock(metalCasingProperties()));
    public static final RegistryObject<ActiveBlock> STAINLESS_STEEL_FIREBOX_CASING = BLOCKS.register("stainless_steel_firebox_casing",
            () -> new ActiveBlock(metalCasingProperties()));
    public static final RegistryObject<ActiveBlock> MITHRILITE_EXTREME_ENGINE_INTAKE_CASING = BLOCKS.register("mithrilite_extreme_engine_intake_casing",
            () -> new ActiveBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<CoilBlock> DRACONIUM_COIL_BLOCK = BLOCKS.register("draconium_coil_block",
            () -> new CoilBlock(metalCasingProperties(), DraconicCoilTypes.DRACONIUM));
    public static final RegistryObject<CoilBlock> AWAKENED_DRACONIUM_COIL_BLOCK = BLOCKS.register("draconium_awakened_coil_block",
            () -> new CoilBlock(metalCasingProperties(), DraconicCoilTypes.AWAKENED_DRACONIUM));

    private static BlockBehaviour.Properties metalCasingProperties() {
        return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .isValidSpawn((state, level, pos, entityType) -> false);
    }

    public static final RegistryObject<com.gregtechceu.gtceu.common.block.FusionCasingBlock> FUSION_CASING_MK4 = BLOCKS.register("fusion_casing_mk4",
            () -> new com.gregtechceu.gtceu.common.block.FusionCasingBlock(
                    BlockBehaviour.Properties.of().strength(5.0f, 10.0f).sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(), DIV.gtcsolo.block.FusionMk4CasingType.INSTANCE));

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

    public static final RegistryObject<Block> DATACHEST =
            BLOCKS.register("datachest", DataChestBlock::new);

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
                .strength(1.5f, 6.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()));
    }

    /** WEN主要外装ケーシング — テクスチャ: cryostat_casing.png */
    public static final RegistryObject<Block> WEN_MAINSTORAGE_CASING = simpleBlock("wen_mainstorage_casing");
    /** WEN出力ポート（機能外装・隣接FE機器とAE2網に接続） — テクスチャ: cryogenic_port_output.png */
    public static final RegistryObject<Block> WEN_MAINSTORAGE_OUTPUT_PORT =
            BLOCKS.register("wen_mainstorage_output_port", WENPortBlock::new);
    /** WEN入力ポート（機能外装・隣接FE機器とAE2網に接続） — テクスチャ: cryogenic_port_input.png */
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
    /** 壊滅コアブロック (Devastation Core Block) — StarForge controller のベース。テクスチャ: tungsten_wall.png */
    public static final RegistryObject<Block> DEVASTATION_CORE_BLOCK = simpleBlock("devastation_core_block");
    /** 共振制御コアブロック */
    public static final RegistryObject<Block> RESONANCE_CONTROL_CORE_BLOCK = simpleBlock("resonance_control_core_block");

    /** Ameijia 次元マーカー (鉱脈図での次元表示用) — テクスチャ: maker/ameijia_top.png (6面共通) */
    public static final RegistryObject<Block> AMEIJIA_MAKER = simpleBlock("ameijia_maker");
    /** Paste controller; preserve the existing block10 ID and refined_netherstar artwork. */
    public static final RegistryObject<Block> BLOCK10 = BLOCKS.register("block10", DIV.gtcsolo.block.copy.PasteBlock::new);

    // =========================================================================
    //  casingblock_n — 仮追加ケーシング群
    // =========================================================================

    public static final RegistryObject<Block> SIMULATION_CASING = simpleBlock("simulation_casing");
    public static final RegistryObject<Block> AURORALIUM_RESONANCE_CASING = simpleBlock("auroralium_resonance_casing");
    public static final RegistryObject<Block> BEDROCKIUM_NOCTURNIUM_FUSION_CASING = simpleBlock("bedrockium_nocturnium_fusion_casing");
    public static final RegistryObject<Block> CASINGBLOCK_3 = simpleBlock("casingblock_3");
    /** オーロラリウム製 StarForge ケーシング — StarForge 構造の C 位置に使用 */
    public static final RegistryObject<Block> AURORALIUM_STARFORGE_CASING = simpleBlock("auroralium_starforge_casing");
    public static final RegistryObject<Block> WEN_FUNCTIONAL_ASSEMBLER_MACHINE_CASING = simpleBlock("wen_functional_assembler_machine_casing");
    public static final RegistryObject<Block> REFINED_OBSIDIAN_CASING = simpleBlock("refined_obsidian_casing");
    public static final RegistryObject<Block> ETERNAL_CASING = simpleBlock("eternal_casing");

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
    //  AE2 / FE ワイヤレスポート
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

    // =========================================================================
    //  exclamation/ 系仮素材ブロック (透過)
    // =========================================================================

    /** 透過ブロック共通プロパティ。視認貫通・湧き禁止・窒息なし・redstone非伝導。 */
    private static RegistryObject<Block> transparentBlock(String name, float hardness, SoundType sound) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .strength(hardness, hardness * 3.0f)
                .sound(sound)
                .noOcclusion()
                .isRedstoneConductor((s, l, p) -> false)
                .isValidSpawn((s, l, p, t) -> false)
                .isViewBlocking((s, l, p) -> false)
                .isSuffocating((s, l, p) -> false)));
    }

    /** old_glass — テクスチャ: exclamation/old_glass.png */
    public static final RegistryObject<Block> OLD_GLASS = transparentBlock("old_glass", 0.3f, SoundType.GLASS);
    /**
     * unknown — テクスチャ: exclamation/unknown.png。 SINGULARITY_MAKER の構造材に使用。
     * 罠 (媒介野) 機能は battle 側 `allthegregtechbattle:unknown` に分離済み — こちらは無害な通常ブロック。
     */
    public static final RegistryObject<Block> UNKNOWN = BLOCKS.register("unknown",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(1.5f, 4.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false)
                    .isValidSpawn((s, l, p, t) -> false)
                    .isViewBlocking((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)));
}
