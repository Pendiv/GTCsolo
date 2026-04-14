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

    /** block4 — テクスチャ: cryostat_glass.png */
    public static final RegistryObject<Block> BLOCK4  = simpleBlock("block4");
    /** block6 — テクスチャ: nb3sn_coil.png */
    public static final RegistryObject<Block> BLOCK6  = simpleBlock("block6");
    /** WENデータモニター（機能外装・BlockEntity付き） — テクスチャ: cryogenic_monitor.png */
    public static final RegistryObject<Block> WEN_DATA_MONITOR =
            BLOCKS.register("wen_data_monitor", WENDataMonitorBlock::new);
    /** block8 — テクスチャ: tungsten_wall.png */
    public static final RegistryObject<Block> BLOCK8  = simpleBlock("block8");
    /** block9 — テクスチャ: ore_smelter_controller.png */
    public static final RegistryObject<Block> BLOCK9  = simpleBlock("block9");
    /** block10 — テクスチャ: refined_netherstar_block.png */
    public static final RegistryObject<Block> BLOCK10 = simpleBlock("block10");
}