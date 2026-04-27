package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.CCMachine;
import DIV.gtcsolo.machine.ChemicalCombustionGeneratorMachine;
import DIV.gtcsolo.machine.FECMachine;
import DIV.gtcsolo.machine.HPABFMachine;
import DIV.gtcsolo.machine.SpaceforgeEnergyHatchMachine;
import DIV.gtcsolo.machine.SpaceforgeMachine;
import DIV.gtcsolo.machine.WENMainStorageMachine;
import DIV.gtcsolo.machine.WMFMachine;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GCYMRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

public class ModMachines {

    public static final GTRegistrate REGISTRATE = GTRegistrate.create("gtcsolo");

    public static MultiblockMachineDefinition FEC;
    public static MultiblockMachineDefinition EEBF;
    public static MultiblockMachineDefinition CC;
    public static MultiblockMachineDefinition WEN_MAIN_STORAGE;
    public static MultiblockMachineDefinition SPACEFORGE;
    public static MultiblockMachineDefinition CHEMICAL_COMBUSTION_GENERATOR;
    public static MultiblockMachineDefinition FANTASIA_FORGE;
    public static MultiblockMachineDefinition HIGHPRESSURE_ALLOY_BLAST_FURNACE;
    public static MultiblockMachineDefinition WIRE_MANUFACTURING_FACTORY;
    public static MultiblockMachineDefinition MATERIAL_PRESS_FACTORY;
    public static MultiblockMachineDefinition MEKANISM_INFUSER;
    public static MultiblockMachineDefinition MICRO_PLANET_MINER;

    // SpaceForge Energy Hatch (SEHatch) — UV ～ MAX × 16/64/256/2048 A
    public static final PartAbility SPACEFORGE_MAIN_ENERGY = new PartAbility("spaceforge_main_energy");
    public static final java.util.Map<Integer, java.util.Map<Integer, MachineDefinition>> SPACEFORGE_ENERGY_HATCH = new java.util.HashMap<>();
    private static final int[] SEHATCH_AMPERAGES = {16, 64, 256, 2048};

    // Upgrade Hatch — 機能拡張用アイテムハッチ (4 tier: LV/EV/LuV/UHV を tier_1..tier_4 として登録)
    public static final PartAbility UPGRADE_HATCH = new PartAbility("upgrade_hatch");
    public static final java.util.Map<Integer, MachineDefinition> UPGRADE_HATCHES = new java.util.HashMap<>();
    /** {voltageTier, tierIndex} */
    private static final int[][] UPGRADE_HATCH_TIERS = {
            {GTValues.LV, 1},
            {GTValues.EV, 2},
            {GTValues.LuV, 3},
            {GTValues.UHV, 4}
    };

    public static void init() {
        FEC = REGISTRATE.multiblock("fec", FECMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ModRecipeTypes.FEC)
                .recipeModifiers(FECMachine::fecOverclock, GTRecipeModifiers.PARALLEL_HATCH)
                .tooltips(
                        Component.translatable("gtcsolo.machine.fec.desc.1"),
                        Component.translatable("gtcsolo.machine.fec.desc.2"),
                        Component.translatable("gtcsolo.machine.fec.desc.3"),
                        Component.translatable("gtcsolo.machine.fec.desc.4"),
                        Component.translatable("gtcsolo.machine.fec.desc.5"),
                Component.translatable("gtcsolo.machine.fec.desc.6"))
                .appearanceBlock(GTBlocks.CASING_HSSE_STURDY)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("  AAAAA  ", "    B    ", "    F    ", "    F    ", "    F    ", "    B    ", "  AAAAA  ")
                        .aisle(" A ABABA ", " D BXBXD ", " D FXFXD ", " D FXFXD ", " D FXFXD ", " D BXBXD ", " A ABABA ")
                        .aisle("A ABBBABA", "  B   B  ", "  F   F  ", "  F   F  ", "  F   F  ", "  B   B  ", "A ABBBABA")
                        .aisle("AABBBBBAA", " B  E  B ", " F  E  F ", " F  E  F ", " F  E  F ", " B  E  B ", "AABBBBBAA")
                        .aisle("ABBBBBBBA", "B  E E  B", "F  E E  F", "F  E E  F", "F  E E  F", "B  E E  B", "ABBBBBBBA")
                        .aisle("AABBBBBAA", " B  E  B ", " F  E  F ", " F  E  F ", " F  E  F ", " B  E  B ", "AABBBBBAA")
                        .aisle("A ABBBABA", "  B   B  ", "  F   F  ", "  F   F  ", "  F   F  ", "  B   B  ", "A ABBBABA")
                        .aisle(" A ABABA ", " D BXBXD ", " D FXFXD ", " D FXFXD ", " D FXFXD ", " D BXBXD ", " A ABABA ")
                        .aisle("  AAYAA  ", "    B    ", "    F    ", "    F    ", "    F    ", "    B    ", "  AAAAA  ")

                        .where('F', blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                        .where('B', blocks(GTBlocks.CASING_STAINLESS_CLEAN.get()))
                        .where('D', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.HSLASteel)))
                        .where('E', heatingCoils())
                        .where('X', air())
                        .where(' ', any())
                        .where('A', blocks(ModBlocks.HIGH_STRENGTH_STEEL_CASING.get())
                                .or(autoAbilities(definition.getRecipeTypes()))
                                .or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(2))
                                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('Y', controller(blocks(definition.getBlock())))
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtcsolo", "block/high_strength_steel_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/assembly_line"))
                .additionalDisplay((controller, components) -> {
                    if (controller instanceof FECMachine fec && controller.isFormed()) {
                        int types = fec.getPlasmaTypeCount();
                        components.add(Component.literal("Plasma Types: " + types)
                                .setStyle(Style.EMPTY.withColor(
                                        types > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
                    }
                })
                .register();

        EEBF = REGISTRATE.multiblock("extended_electric_blast_furnace", CoilWorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.BLAST_RECIPES)
                .recipeModifiers(GTRecipeModifiers::ebfOverclock)
                .tooltips(
                        Component.translatable("gtcsolo.machine.eebf.desc.1"),
                        Component.translatable("gtcsolo.machine.eebf.desc.2"),
                        Component.translatable("gtcsolo.machine.eebf.desc.3"))
                .appearanceBlock(GCYMBlocks.CASING_ATOMIC)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("OOOAOOO", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "OOOAOOO")
                        .aisle("OBBBBBO", "XEBBBEX", "XFFFFFX", "XFFFFFX", "XFFFFFX", "XEBBBEX", "OBBBBBO")
                        .aisle("OBBBBBO", "XBXXXBX", "XFXXXFX", "XFXXXFX", "XFXXXFX", "XBXXXBX", "OBBBBBO")
                        .aisle("ABBBBBO", "DBXDXBD", "DFXDXFD", "DFXDXFD", "DFXDXFD", "DBXDXBD", "ABBCBBO")
                        .aisle("OBBBBBO", "XBXXXBX", "XFXXXFX", "XFXXXFX", "XFXXXFX", "XBXXXBX", "OBBBBBO")
                        .aisle("OBBBBBO", "XEBBBEX", "XFFFFFX", "XFFFFFX", "XFFFFFX", "XEBBBEX", "OBBBBBO")
                        .aisle("OOOYOOO", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "OOOAOOO")

                        .where('Y', controller(blocks(definition.getBlock())))
                        .where('O', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                                .or(autoAbilities(definition.getRecipeTypes()))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                                // M5 migration: Mek gas 入力を受けるための INPUT_GAS hatch
                                .or(abilities(DIV.gtcsolo.integration.mekanism.capability
                                        .ChemicalPartAbilities.INPUT_GAS).setMinGlobalLimited(1)))
                        .where('A', blocks(GTBlocks.CASING_EXTREME_ENGINE_INTAKE.get()))
                        .where('B', blocks(GCYMBlocks.CASING_ATOMIC.get()))
                        .where('C', abilities(PartAbility.MUFFLER).setExactLimit(1))
                        .where('D', blocks(GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get()))
                        .where('E', blocks(GTBlocks.FIREBOX_TUNGSTENSTEEL.get()))
                        .where('F', heatingCoils())
                        .where('X', any())
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtceu", "block/casings/gcym/atomic_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/electric_blast_furnace"))
                .additionalDisplay((controller, components) -> {
                    if (controller instanceof CoilWorkableElectricMultiblockMachine coilMachine && controller.isFormed()) {
                        components.add(Component.translatable("gtceu.multiblock.blast_furnace.max_temperature",
                                Component.translatable(
                                        FormattingUtil.formatNumbers(coilMachine.getCoilType().getCoilTemperature() +
                                                100L * Math.max(0, coilMachine.getTier() - GTValues.MV)) + "K")
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))));
                    }
                })
                .register();

        CC = REGISTRATE.multiblock("collapsed_chamber", CCMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.MACERATOR_RECIPES)
                .recipeModifiers(CCMachine::ccParallel)
                .tooltips(
                        Component.translatable("gtcsolo.machine.cc.desc.1"),
                        Component.translatable("gtcsolo.machine.cc.desc.2"),
                        Component.translatable("gtcsolo.machine.cc.desc.3"),
                        Component.translatable("gtcsolo.machine.cc.desc.4"))
                .appearanceBlock(GTBlocks.CASING_HSSE_STURDY)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("XXXXX", "XAZAX", "XXXXX", "XXXXX")
                        .aisle("XXXXX", "ACCCA", "XYYYX", "XXXXX")
                        .aisle("XXXXX", "ZCCCZ", "XYYYX", "XXXXX")
                        .aisle("XXXXX", "ACCCA", "XYYYX", "XXXXX")
                        .aisle("XX#XX", "XAZAX", "XXXXX", "XXXXX")

                        .where('#', controller(blocks(definition.getBlock())))
                        .where('A', blocks(GCYMBlocks.CASING_ATOMIC.get()))
                        .where('C', blocks(GCYMBlocks.CRUSHING_WHEELS.get()))
                        .where('Z', blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where('Y', air())
                        .where('X', blocks(ModBlocks.BEDROCKIUM_HE_CASING.get())
                                .or(autoAbilities(definition.getRecipeTypes()))
                                .or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setMaxGlobalLimited(17))
                                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)))
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtcsolo", "block/bedrockium_he_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/assembler"))
                .additionalDisplay((controller, components) -> {
                    if (controller instanceof CCMachine cc && controller.isFormed()) {
                        components.add(Component.literal("Parallels: " + cc.getCurrentParallels())
                                .setStyle(Style.EMPTY.withColor(
                                        cc.getCurrentParallels() > 1 ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
                    }
                })
                .register();

        // =========================================================================
        //  WEN Main Storage — 可変サイズ蓄電マルチブロック
        // =========================================================================
        WEN_MAIN_STORAGE = REGISTRATE.multiblock("wen_main_storage", WENMainStorageMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ModRecipeTypes.WEN_STORAGE)
                .tooltips(
                        Component.translatable("gtcsolo.machine.wen.desc.1"),
                        Component.translatable("gtcsolo.machine.wen.desc.2"),
                        Component.translatable("gtcsolo.machine.wen.desc.3"))
                .appearanceBlock(ModBlocks.WEN_MAINSTORAGE_CASING)
                .pattern(definition -> FactoryBlockPattern.start(
                        com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.RIGHT,
                        com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.UP,
                        com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.BACK)
                        // JEI用最小構成 3x3x3: コントローラーは前面最下段
                        .aisle("WCW", "WWW", "WWW")  // front face: Cは最下行中央
                        .aisle("WWW", "WSW", "WWW")  // middle
                        .aisle("WWW", "WWW", "WWW")  // back face
                        .where('C', controller(blocks(definition.getBlock())))
                        .where('W', blocks(ModBlocks.WEN_MAINSTORAGE_CASING.get())
                                .or(blocks(ModBlocks.WEN_DATA_MONITOR.get())))
                        .where('S', blocks(ModBlocks.WEN_BASIC_ENERGY_CELL.get()))
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtcsolo", "block/cryostat_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/data_bank"))
                .register();

        // =========================================================================
        //  Chemical Combustion Generator — 液体2+1種燃焼発電マルチブロック
        // =========================================================================
        CHEMICAL_COMBUSTION_GENERATOR = REGISTRATE.multiblock("chemical_combustion_generator",
                        ChemicalCombustionGeneratorMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .generator(true)
                .recipeType(ModRecipeTypes.CHEMICAL_COMBUSTION_GENERATOR)
                .tooltips(
                        Component.translatable("gtcsolo.machine.ccg.desc.1"),
                        Component.translatable("gtcsolo.machine.ccg.desc.2"))
                .appearanceBlock(ModBlocks.REFINED_OBSIDIAN_CASING)
                .additionalDisplay((controller, components) -> {
                    if (controller instanceof ChemicalCombustionGeneratorMachine ccg && controller.isFormed()) {
                        if (ccg.isBoosted()) {
                            components.add(Component.translatable("gtcsolo.machine.ccg.boosted")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
                        } else {
                            components.add(Component.translatable("gtcsolo.machine.ccg.not_boosted")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
                        }
                    }
                })
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("AAAAA", "AAAAA", "AAMAA", "AAAAA", "AAAAA")
                        .aisle("AAZAA", "ACDCA", "ZDYDZ", "ACDCA", "AAZAA")
                        .aisle("AAZAA", "ACDCA", "ZDYDZ", "ACDCA", "AAZAA")
                        .aisle("AAZAA", "ACDCA", "ZDYDZ", "ACDCA", "AAZAA")
                        .aisle("AAZAA", "ACDCA", "ZDYDZ", "ACDCA", "AAZAA")
                        .aisle("AAZAA", "ACDCA", "ZDYDZ", "ACDCA", "AAZAA")
                        .aisle("AAAAA", "ABBBA", "ABXBA", "ABBBA", "AAAAA")
                        .where('X', controller(blocks(definition.getBlock())))
                        .where('A', blocks(ModBlocks.REFINED_OBSIDIAN_CASING.get())
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('B', blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where('C', blocks(ModBlocks.HIGH_STRENGTH_STEEL_CASING.get()))
                        .where('D', blocks(ModBlocks.CHEMICAL_RESISTANT_CASING.get()))
                        .where('M', abilities(PartAbility.MUFFLER).setExactLimit(1))
                        .where('Y', air())
                        .where('Z', blocks(ModBlocks.REFINED_OBSIDIAN_CASING.get())
                                .or(abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1))
                                .or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(2))
                                .or(abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1)))
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtcsolo", "block/refined_obsidian_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/generator/large_combustion_engine"))
                .register();

        // Space Forge — 発展型核融合マルチブロック
        SPACEFORGE = REGISTRATE.multiblock("spaceforge", SpaceforgeMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ModRecipeTypes.SPACEFORGE)
                .recipeModifiers(SpaceforgeMachine::spaceforgeOverclock)
                .tier(GTValues.MAX)  // OC上限: SEHatch tierを超える部分は不完全OC
                .tooltips(
                        Component.translatable("gtcsolo.machine.spaceforge.desc.1"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.2"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.3"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.4"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.5"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.6"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.7"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.8"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.9"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.10"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.11"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.12"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.13"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.14"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.15"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.16"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.17"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.18"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.19"),
                        Component.translatable("gtcsolo.machine.spaceforge.desc.20"))
                .appearanceBlock(ModBlocks.AURORALIUM_RESONANCE_CASING)
                .pattern(definition -> FactoryBlockPattern.start()
                        // リング構造1 (5 aisle)
                        .aisle("#########################","##A#########H#########A##","#AGA#######AGA#######AGA#","#########################","#########################","#########################","#########################","#AAA#######AAA#######AAA#","##A#########A#########A##","#########################")
                        .aisle("#########################","#AAA#######AAA#######AAA#","AAAAA#####AAAAA#####AAAAA","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","AAAAA#####AAAAA#####AAAAA","#AAA#######AAA#######AAA#","#########################")
                        .aisle("##A#########A#########A##","AAAAA#####AAAAA#####AAAAA","GAAAAAAAAAAAAAAAAAAAAAAAG","#CBC#######CBC#######CBC#","#CBC#######CBC#######CBC#","#CBC#######CBC#######CBC#","#CBC#######CBC#######CBC#","AAAAAAAAAAAAAAAAAAAAAAAAA","AAAAA#####AAAAA#####AAAAA","##A#########A#########A##")
                        .aisle("#########################","#AAA#######AAA#######AAA#","AAAAA#####AAAAA#####AAAAA","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","AAAAA#####AAAAA#####AAAAA","#AAA#######AAA#######AAA#","#########################")
                        .aisle("#########################","##A#########A#########A##","#AAA#######AAA#######AAA#","#########################","#########################","#########################","#########################","#AAA#######AAA#######AAA#","##A#########A#########A##","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","#########################","#########################","#########################","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","#########################","###########DDD###########","#########################","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","###########DDD###########","#########DD#E#DD#########","###########DDD###########","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","#########DD###DD#########","########F#EDDDE#F########","#########DD###DD#########","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","########D#######D########","#######F#DD###DD#F#######","########D#######D########","##A###################A##","#########################","#########################")
                        .aisle("##A###################A##","##A###################A##","#AAA#################AAA#","#########################","########D#######D########","#######DED#####DED#######","########D#######D########","#AAA#################AAA#","##A###################A##","#########################")
                        .aisle("#########################","#AAA#################AAA#","AAAAA###############AAAAA","#CCC#################CCC#","#CCC###D#########D###CCC#","#CCC##D#D#######D#D##CCC#","#CCC###D#########D###CCC#","AAAAA###############AAAAA","#AAA#################AAA#","#########################")
                        .aisle("#########################","AAAAA###############AAAAA","GAAAA###############AAAAG","#CBC#################CBC#","#CBC###D#########D###CBC#","#CBC##DED#######DED##CBC#","#CBC###D#########D###CBC#","AAAAA###############AAAAA","AAAAA###############AAAAA","##A###################A##")
                        .aisle("#########################","#AAA#################AAA#","AAAAA###############AAAAA","#CCC#################CCC#","#CCC###D#########D###CCC#","#CCC##D#D#######D#D##CCC#","#CCC###D#########D###CCC#","AAAAA###############AAAAA","#AAA#################AAA#","#########################")
                        .aisle("##A###################A##","##A###################A##","#AA##################AAA#","#########################","########D#######D########","#######DED#####DED#######","########D#######D########","#AAA#################AAA#","##A###################A##","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","########D#######D########","#######F#DD###DD#F#######","########D#######D########","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","#########DD###DD#########","########F#EDDDE#F########","#########DD###DD#########","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","###########DDD###########","#########DD#E#DD#########","###########DDD###########","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","#########################","###########DMD###########","#########################","##A###################A##","#########################","#########################")
                        .aisle("#########################","#########################","##A###################A##","#########################","#########################","#########################","#########################","##A###################A##","#########################","#########################")
                        .aisle("#########################","##A#########A#########A##","#AAA#######AAA#######AAA#","#########################","#########################","#########################","#########################","#AAA#######AAA#######AAA#","##A#########A#########A##","#########################")
                        .aisle("#########################","#AAA#######AAA#######AAA#","AAAAA#####AAAAA#####AAAAA","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","AAAAA#####AAAAA#####AAAAA","#AAA#######AAA#######AAA#","#########################")
                        .aisle("##A#########A#########A##","AAAAA#####AAAAA#####AAAAA","AAAAAAAAAAAAAAAAAAAAAAAAA","#CBC#######CBC#######CBC#","#CBC#######CCC#######CBC#","#CBC#######CBC#######CBC#","#CBC#######CBC#######CBC#","AAAAAAAAAAAAAAAAAAAAAAAAA","AAAAA#####AAAAA#####AAAAA","##A#########A#########A##")
                        .aisle("#########################","#AAA#######AAA#######AAA#","AAAAA#####AAAAA#####AAAAA","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","#CCC#######CCC#######CCC#","AAAAA#####AAAAA#####AAAAA","#AAA#######AAA#######AAA#","#########################")
                        .aisle("#########################","##A#########A#########A##","#AGA#######AXA#######AGA#","#########################","#########################","#########################","#########################","#AAA#######AAA#######AAA#","##A#########A#########A##","#########################")
                        .where('#', any())
                        .where('C', heatingCoils())
                        .where('B', blocks(ModBlocks.RESONANCE_CONTROL_CORE_BLOCK.get()))
                        .where('D', blocks(ModBlocks.BEDROCKIUM_NOCTURNIUM_FUSION_CASING.get()))
                        .where('E', blocks(GTBlocks.FUSION_COIL.get()))
                        .where('A', blocks(ModBlocks.AURORALIUM_RESONANCE_CASING.get()))
                        .where('G', blocks(ModBlocks.AURORALIUM_RESONANCE_CASING.get())
                                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)))
                        .where('H', abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .where('F', blocks(ModBlocks.BEDROCKIUM_NOCTURNIUM_FUSION_CASING.get())
                                .or(abilities(PartAbility.IMPORT_FLUIDS))
                                .or(abilities(PartAbility.IMPORT_ITEMS))
                                .or(abilities(PartAbility.EXPORT_FLUIDS))
                                .or(abilities(PartAbility.EXPORT_ITEMS)))
                        .where('M', abilities(SPACEFORGE_MAIN_ENERGY).setExactLimit(1))
                        .where('X', controller(blocks(definition.getBlock())))
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtcsolo", "block/auroralium_resonance_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/fusion_reactor"))
                .register();

        // SpaceForge Energy Hatch (SEHatch) — UV ～ MAX × 各アンペア
        int[] seTiers = { GTValues.UV, GTValues.UHV, GTValues.UEV,
                GTValues.UIV, GTValues.UXV, GTValues.OpV, GTValues.MAX };
        for (int tier : seTiers) {
            SPACEFORGE_ENERGY_HATCH.put(tier, new java.util.HashMap<>());
            for (int amp : SEHATCH_AMPERAGES) {
                registerSpaceforgeEnergyHatch(tier, amp);
            }
        }

        // =========================================================================
        //  Fantasia Forge — 9x9x9 幻想鍛造マルチブロック
        //  構造パターンは run/cmdex/export/fantasiaforge.txt 由来
        //  A=幻想強化ケーシング(casingblock_3), B=エクスポロニウムフレーム,
        //  C=積層ガラス, D=ヒートベント, E=キュプロニッケルコイル,
        //  F=高強度鋼ケーシング, G=鉄ブロック, H=タングステン鋼ギアボックス
        // =========================================================================
        FANTASIA_FORGE = REGISTRATE.multiblock("fantasia_forge", WorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ModRecipeTypes.FANTASIA_FORGE)
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH)
                .tooltips(
                        Component.translatable("gtcsolo.machine.fantasia_forge.desc.1"),
                        Component.translatable("gtcsolo.machine.fantasia_forge.desc.2"))
                .appearanceBlock(ModBlocks.CASINGBLOCK_3)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("AAAAAAAAA", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCDDDDDCB", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "A#EEEEE#C", "A#######C", "A#######C", "A#######C", "A#######C", "A#######C", "A#EEEEE#C", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "CEEFFFEEC", "C#######C", "C#######C", "C#######C", "C#######C", "C#######C", "CEEGGGEEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "CEFHHHFEC", "C##FFF##C", "C#######C", "C#######C", "C#######C", "C##FFF##C", "CEGGGGGEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "CEFHHHFEC", "C##FFF##C", "C#######C", "C#######C", "C#######C", "C##FFF##C", "CEGGGGGEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "CEFHHHFEC", "C##FFF##C", "C#######C", "C#######C", "C#######C", "C##FFF##C", "CEGGGGGEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "CEEFFFEEC", "C#######C", "C#######C", "C#######C", "C#######C", "C#######C", "CEEGGGEEC", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "C#EEEEE#C", "C#######C", "C#######C", "C#######C", "C#######C", "C#######C", "C#EEEEE#C", "AAAAAAAAA")
                        .aisle("AAAAYAAAA", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCCCCCCCB", "BCDDDDDCB", "AAAAAAAAA")
                        .where('Y', controller(blocks(definition.getBlock())))
                        .where('A', blocks(ModBlocks.CASINGBLOCK_3.get())
                                .or(autoAbilities(definition.getRecipeTypes()))
                                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('B', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, ModMaterials.EXPORONIUM)))
                        .where('C', blocks(GTBlocks.CASING_LAMINATED_GLASS.get()))
                        .where('D', blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where('E', blocks(GTBlocks.COIL_CUPRONICKEL.get()))
                        .where('F', blocks(ModBlocks.HIGH_STRENGTH_STEEL_CASING.get()))
                        .where('G', blocks(Blocks.IRON_BLOCK))
                        .where('H', blocks(GTBlocks.CASING_TUNGSTENSTEEL_GEARBOX.get()))
                        .where('#', any())
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtcsolo", "block/casingblock_3"),
                        new ResourceLocation("gtceu", "block/multiblock/assembly_line"))
                .register();

        // =========================================================================
        //  High-Pressure Alloy Blast Furnace — 9x9x9 高圧合金高炉
        //  構造パターンは run/cmdex/export/HPABF.txt 由来
        //  A=高温精錬ケーシング(GCYM), B=タングステンフレーム, C=ヒートベント,
        //  D=加熱コイル, E=HTFFケーシング(gtcsolo)
        //  レシピは合金高炉(ALLOY_BLAST_RECIPES)、ロジックはEBFと同じコイル温度OC
        // =========================================================================
        HIGHPRESSURE_ALLOY_BLAST_FURNACE = REGISTRATE.multiblock("highpressure_alloy_blast_furnace",
                        HPABFMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GCYMRecipeTypes.ALLOY_BLAST_RECIPES)
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, HPABFMachine::hpabfOverclock)
                .tooltips(
                        Component.translatable("gtcsolo.machine.hpabf.desc.1"),
                        Component.translatable("gtcsolo.machine.hpabf.desc.2"),
                        Component.translatable("gtcsolo.machine.hpabf.desc.3"),
                        Component.translatable("gtcsolo.machine.hpabf.desc.4"),
                        Component.translatable("gtcsolo.machine.hpabf.desc.5"),
                        Component.translatable("gtcsolo.machine.hpabf.desc.6"),
                        Component.translatable("gtcsolo.machine.hpabf.desc.7"))
                .appearanceBlock(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("##AAAAA##", "#########", "#########", "#########", "#########", "#########", "#########", "#########", "##AAAAA##")
                        .aisle("#AAAAAAA#", "#BAAAAAB#", "#BACCCAB#", "#BDDDDDB#", "#BDDDDDB#", "#BDDDDDB#", "#BACCCAB#", "#BAAAAAB#", "#AAAAAAA#")
                        .aisle("AAAAAAAAA", "#AAAAAAA#", "#AEEEEEA#", "#DEEEEED#", "#DEEEEED#", "#DEEEEED#", "#AEEEEEA#", "#AAAAAAA#", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "#AAEEEAA#", "#CE###EC#", "#DE###ED#", "#DE###ED#", "#DE###ED#", "#CEEEEEC#", "#AAAAAAA#", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "#AAEEEAA#", "#CE###EC#", "#DE###ED#", "#DE###ED#", "#DE###ED#", "#CEE#EEC#", "#AAAAAAA#", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "#AAEEEAA#", "#CE###EC#", "#DE###ED#", "#DE###ED#", "#DE###ED#", "#CEEEEEC#", "#AAAAAAA#", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "#AAAAAAA#", "#AEEEEEA#", "#DEEEEED#", "#DEEEEED#", "#DEEEEED#", "#AEEEEEA#", "#AAAAAAA#", "AAAAAAAAA")
                        .aisle("#AAAAAAA#", "#BAAYAAB#", "#BACCCAB#", "#BDDDDDB#", "#BDDDDDB#", "#BDDDDDB#", "#BACCCAB#", "#BAAAAAB#", "#AAAAAAA#")
                        .aisle("##AAAAA##", "#########", "#########", "#########", "#########", "#########", "#########", "#########", "##AAAAA##")
                        .where('Y', controller(blocks(definition.getBlock())))
                        .where('A', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get())
                                .or(autoAbilities(definition.getRecipeTypes()))
                                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(abilities(PartAbility.MUFFLER).setExactLimit(1))))
                        .where('B', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Tungsten)))
                        .where('C', blocks(GCYMBlocks.HEAT_VENT.get()))
                        .where('D', heatingCoils())
                        .where('E', blocks(ModBlocks.HTFF_CASING.get()))
                        .where('#', any())
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtceu", "block/casings/gcym/high_temperature_smelting_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/gcym/blast_alloy_smelter"))
                .additionalDisplay((controller, components) -> {
                    if (controller instanceof HPABFMachine hpabf && controller.isFormed()) {
                        components.add(Component.translatable("gtceu.multiblock.blast_furnace.max_temperature",
                                Component.literal(
                                        FormattingUtil.formatNumbers(hpabf.getBoostedCoilMaxTemp()) + "K")
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))));
                        components.add(Component.translatable("gtcsolo.machine.hpabf.display.boosted")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
                    }
                })
                .register();

        // =========================================================================
        //  Wire Manufacturing Factory — 5x5x18 ワイヤー加工工場
        //  構造パターンは run/cmdex/export/WMF.txt 由来
        //  A=ストレスプルーフケーシング(外殻+エネルギー入力+メンテナンス)
        //  B=搬入バス位置, X=搬出バス位置
        //  C=高強度鋼ケーシング(gtcsolo), D=タングステン鋼ギアボックス
        //  工業OC: 各段 duration×0.9, EUt×4, I/O×2 (並列ハッチは非対応)
        //  排他化: 他MBとハッチ共有不可
        // =========================================================================
        WIRE_MANUFACTURING_FACTORY = REGISTRATE.multiblock("wire_manufacturing_factory",
                        WMFMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.WIREMILL_RECIPES)
                .recipeModifiers(WMFMachine::industrialOverclock)
                .tooltips(
                        Component.translatable("gtcsolo.machine.wmf.desc.1"),
                        Component.translatable("gtcsolo.machine.wmf.desc.2"),
                        Component.translatable("gtcsolo.machine.wmf.desc.3"),
                        Component.translatable("gtcsolo.machine.wmf.desc.4"))
                .appearanceBlock(GCYMBlocks.CASING_STRESS_PROOF)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("AAAAA", "AAAAA", "ABBBA", "AAAAA", "AAAAA")
                        .aisle("AAAAA", "ACCCA", "A###A", "ACCCA", "AAAAA")
                        .aisle("AAAAA", "ACCCA", "A###A", "ACCCA", "AAAAA")
                        .aisle("AAAAA", "ACCCA", "A###A", "ACCCA", "AAAAA")
                        .aisle("AAAAA", "AAAAA", "A###A", "AAAAA", "AAAAA")
                        .aisle("AAAAA", "AACAA", "AA#AA", "#ADA#", "#AAA#")
                        .aisle("AAAAA", "AACAA", "AA#AA", "#ADA#", "#AAA#")
                        .aisle("AAAAA", "AACAA", "AA#AA", "#ADA#", "#AAA#")
                        .aisle("AAAAA", "AACAA", "AA#AA", "#ADA#", "#AAA#")
                        .aisle("AAAAA", "AADAA", "AADAA", "##A##", "##A##")
                        .aisle("AAAAA", "AADAA", "AADAA", "##A##", "##A##")
                        .aisle("AAAAA", "AADAA", "AADAA", "##A##", "##A##")
                        .aisle("AAAAA", "AADAA", "AADAA", "##A##", "##A##")
                        .aisle("AAAAA", "AA#AA", "AA#AA", "AAAAA", "AAAAA")
                        .aisle("AAAAA", "AC#CA", "AC#CA", "AC#CA", "AAAAA")
                        .aisle("AAAAA", "AC#CA", "AC#CA", "AC#CA", "AAAAA")
                        .aisle("AAAAA", "AC#CA", "AC#CA", "AC#CA", "AAAAA")
                        .aisle("AAEAA", "AAXAA", "AAXAA", "AAXAA", "AAAAA")
                        .where('E', controller(blocks(definition.getBlock())))
                        .where('A', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_STRESS_PROOF.get())
                                .or(abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1))))
                        .where('B', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_STRESS_PROOF.get())
                                .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1))))
                        .where('X', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_STRESS_PROOF.get())
                                .or(abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1))))
                        .where('C', blocks(ModBlocks.HIGH_STRENGTH_STEEL_CASING.get()))
                        .where('D', blocks(GTBlocks.CASING_TUNGSTENSTEEL_GEARBOX.get()))
                        .where('#', any())
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtceu", "block/casings/gcym/stress_proof_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/gcym/large_wiremill"))
                .register();

        // =========================================================================
        //  Material Press Factory — 7x4x9 マテリアルプレス工場
        //  LARGE_MATERIAL_PRESS (BENDER/COMPRESSOR/FORGE_HAMMER/FORMING_PRESS) と
        //  同じレシピタイプを担うが、PARALLEL_HATCH 非対応、代わりに工業OC
        //  草案: run/cmdex/export/material_press_factory.txt
        //  A=ストレスプルーフ外殻+メンテ, B=強化ガラス, C=高強度鋼ケーシング(gtcsolo)
        //  D=鋼パイプケーシング, E=鋼ギアボックス
        //  F=入力バス, G=出力バス, I=エネルギー入力ハッチ(MAX 2), H=コントローラー
        // =========================================================================
        MATERIAL_PRESS_FACTORY = REGISTRATE.multiblock("material_press_factory",
                        WorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(GTRecipeTypes.BENDER_RECIPES,
                             GTRecipeTypes.COMPRESSOR_RECIPES,
                             GTRecipeTypes.FORGE_HAMMER_RECIPES,
                             GTRecipeTypes.FORMING_PRESS_RECIPES)
                .recipeModifiers(WMFMachine::industrialOverclock)
                .tooltips(
                        Component.translatable("gtcsolo.machine.mpf.desc.1"),
                        Component.translatable("gtcsolo.machine.mpf.desc.2"),
                        Component.translatable("gtcsolo.machine.mpf.desc.3"),
                        Component.translatable("gtcsolo.machine.mpf.desc.4"))
                .appearanceBlock(GCYMBlocks.CASING_STRESS_PROOF)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA")
                        .aisle("AAAAAAA", "BACCCCD", "BACCCCC", "AAAAAAA")
                        .aisle("AAAAAAA", "BACCEED", "B#CC##F", "AAAAAAA")
                        .aisle("GACCAAA", "B###EED", "B#####F", "AAAAAAA")
                        .aisle("GACCAAA", "B###EED", "B#####F", "AAAAAAA")
                        .aisle("GACCAAA", "B###EED", "B#####F", "AAAAAAA")
                        .aisle("AAAAAAA", "BACCEED", "B#CC##F", "AAAAAAA")
                        .aisle("AAAAAAA", "AACCCCC", "HACCCCC", "AAAAAAA")
                        .aisle("AAAAAII", "AAAAAAA", "AAAAAAA", "AAAAAAA")
                        .where('H', controller(blocks(definition.getBlock())))
                        .where('A', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_STRESS_PROOF.get())
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1))))
                        .where('B', blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                        .where('C', blocks(ModBlocks.HIGH_STRENGTH_STEEL_CASING.get()))
                        .where('D', blocks(GTBlocks.CASING_STEEL_PIPE.get()))
                        .where('E', blocks(GTBlocks.CASING_STEEL_GEARBOX.get()))
                        .where('F', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_STRESS_PROOF.get())
                                .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1))))
                        .where('G', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_STRESS_PROOF.get())
                                .or(abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1))))
                        .where('I', HPABFMachine.exclusive(
                                blocks(GCYMBlocks.CASING_STRESS_PROOF.get())
                                .or(abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))))
                        .where('#', any())
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtceu", "block/casings/gcym/stress_proof_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/gcym/large_material_press"))
                .register();

        // =========================================================================
        //  Mekanism Infuser — 5x7x5 吹込み合金生成マルチブロック
        //  ({infused, reinforced, atomic, hypercharged}合金の生成)
        //  KubeJSから移植 (アップグレード(parallel_hatch等)対応のため)
        // =========================================================================
        MEKANISM_INFUSER = REGISTRATE.multiblock("mekanism_infuser",
                        WorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ModRecipeTypes.MEKANISM_INFUSER)
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH,
                        DIV.gtcsolo.util.GtcsoloRecipeModifiers::industrialOverclockWithUpgradeHatch)
                .tooltips(
                        Component.translatable("gtcsolo.machine.mekanism_infuser.desc.1"),
                        Component.translatable("gtcsolo.machine.mekanism_infuser.desc.2"),
                        Component.translatable("gtcsolo.machine.mekanism_infuser.desc.3"),
                        Component.translatable("gtcsolo.machine.mekanism_infuser.desc.4"),
                        Component.translatable("gtcsolo.machine.mekanism_infuser.desc.5"))
                .appearanceBlock(GTBlocks.CASING_INVAR_HEATPROOF)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("AAAAA", "BBBBB", "CCCCC", "CCCCC", "CCCCC", "BBBBB", "AAAAA")
                        .aisle("ABBBA", "BDDDB", "CDDDC", "CDDDC", "CDDDC", "BDDDB", "AAAAA")
                        .aisle("ABBBA", "BDDDB", "CD DC", "CD DC", "CD DC", "BDDDB", "AAFAA")
                        .aisle("ABBBA", "BDDDB", "CDDDC", "CDDDC", "CDDDC", "BDDDB", "AAAAA")
                        .aisle("AAEAA", "BBBBB", "CCCCC", "CCCCC", "CCCCC", "BBBBB", "AAAAA")
                        .where('E', controller(blocks(definition.getBlock())))
                        .where('A', blocks(GTBlocks.CASING_INVAR_HEATPROOF.get())
                                .or(autoAbilities(definition.getRecipeTypes()))
                                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(abilities(UPGRADE_HATCH).setMaxGlobalLimited(8))
                                // 新 Chemical INPUT_INFUSION hatch を受け入れ (migration)
                                .or(abilities(DIV.gtcsolo.integration.mekanism.capability
                                        .ChemicalPartAbilities.INPUT_INFUSION).setMinGlobalLimited(1)))
                        .where('B', blocks(GTBlocks.CASING_STEEL_SOLID.get()))
                        .where('C', heatingCoils())
                        .where('D', blocks(net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                .getValue(new ResourceLocation("mekanism", "block_steel"))))
                        .where('F', abilities(PartAbility.MUFFLER).setExactLimit(1))
                        .where(' ', any())
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtceu", "block/casings/solid/machine_casing_heatproof"),
                        new ResourceLocation("gtceu", "block/multiblock/large_miner"))
                .register();

        for (int[] pair : UPGRADE_HATCH_TIERS) {
            registerUpgradeHatch(pair[0], pair[1]);
        }

        // =========================================================================
        //  Micro Planet Miner — 7x7x7 角丸球状マルチブロック
        //  構造パターンは run/cmdex/export/micro_planet_miner.txt 由来
        //  A = シミュレーションケーシング or fluid/chemical ハッチ
        //  B = R-HICC ガラス (角丸球の窓)
        //  C = コントローラ (層 0 前面中央 A に配置)
        // =========================================================================
        MICRO_PLANET_MINER = REGISTRATE.multiblock("micro_planet_miner", WorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.MACERATOR_RECIPES)
                .tooltips(
                        Component.translatable("gtcsolo.machine.micro_planet_miner.desc.1"),
                        Component.translatable("gtcsolo.machine.micro_planet_miner.desc.2"))
                .appearanceBlock(ModBlocks.SIMULATION_CASING)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("##ACA##", "#######", "A#BBB#A", "A#BBB#A", "A#BBB#A", "#######", "##AAA##")
                        .aisle("#######", "#ABBBA#", "#B###B#", "#B###B#", "#B###B#", "#ABBBA#", "#######")
                        .aisle("A#BBB#A", "#B###B#", "B#####B", "B#####B", "B#####B", "#B###B#", "A#BBB#A")
                        .aisle("A#BBB#A", "#B###B#", "B#####B", "B#####B", "B#####B", "#B###B#", "A#BBB#A")
                        .aisle("A#BBB#A", "#B###B#", "B#####B", "B#####B", "B#####B", "#B###B#", "A#BBB#A")
                        .aisle("#######", "#ABBBA#", "#B###B#", "#B###B#", "#B###B#", "#ABBBA#", "#######")
                        .aisle("##AAA##", "#######", "A#BBB#A", "A#BBB#A", "A#BBB#A", "#######", "##AAA##")
                        .where('C', Predicates.controller(Predicates.blocks(definition.getBlock())))
                        .where('A', Predicates.blocks(ModBlocks.SIMULATION_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4))
                                .or(Predicates.abilities(
                                        DIV.gtcsolo.integration.mekanism.capability.ChemicalPartAbilities.INPUT_GAS)
                                        .setMaxGlobalLimited(2))
                                .or(Predicates.abilities(
                                        DIV.gtcsolo.integration.mekanism.capability.ChemicalPartAbilities.OUTPUT_GAS)
                                        .setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('B', Predicates.blocks(ModBlocks.R_HICC_GLASS.get()))
                        .where('#', Predicates.any())
                        .build())
                .workableCasingRenderer(
                        new ResourceLocation("gtcsolo", "block/simulation_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/large_material_press"))
                .register();

        // Mekanism chemical IO hatch 群 (GAS/INFUSION/OTHER × IN/OUT × 9tier + creative = 60台)
        DIV.gtcsolo.integration.mekanism.capability.ChemicalHatches.init();

        // Universal IO hatch 群 (fluid or gas 排他、IN/OUT × 9tier = 18台)
        DIV.gtcsolo.integration.mekanism.capability.UniversalHatches.init();

        // WEN ワイヤレスIOマシン群
        WENMachines.init();
    }

    private static void registerUpgradeHatch(int voltageTier, int tierIndex) {
        String name = "upgrade_hatch_tier_" + tierIndex;
        int slots = (tierIndex + 1) * (tierIndex + 1);

        MachineDefinition def = REGISTRATE.machine(name,
                        holder -> DIV.gtcsolo.machine.UpgradeHatchMachine.create(holder, voltageTier, tierIndex))
                .rotationState(RotationState.ALL)
                .tier(voltageTier)
                .abilities(UPGRADE_HATCH)
                .tooltips(
                        Component.translatable("gtcsolo.machine.upgrade_hatch.desc.1"),
                        Component.translatable("gtcsolo.machine.upgrade_hatch.desc.2", tierIndex, slots))
                .overlayTieredHullRenderer("upgrade_hatch")
                .register();

        UPGRADE_HATCHES.put(tierIndex, def);
    }

    private static void registerSpaceforgeEnergyHatch(int tier, int amperage) {
        String tierName = GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT);
        String name = "spaceforge_energy_hatch_" + tierName + "_" + amperage + "a";
        String overlayName = "sf_" + amperage + "a";

        MachineDefinition def = REGISTRATE.machine(name,
                        holder -> SpaceforgeEnergyHatchMachine.create(holder, tier, amperage))
                .rotationState(RotationState.ALL)
                .tier(tier)
                .abilities(SPACEFORGE_MAIN_ENERGY)
                .tooltips(
                        Component.translatable("gtcsolo.machine.sehatch.desc.1",
                                GTValues.VNF[tier], amperage),
                        Component.translatable("gtcsolo.machine.sehatch.desc.2"),
                        Component.translatable("gtcsolo.machine.sehatch.desc.3",
                                GTValues.VNF[Math.max(0, tier - 3)]),
                        Component.translatable("gtcsolo.machine.sehatch.desc.4"),
                        Component.translatable("gtcsolo.machine.sehatch.desc.5"),
                        Component.translatable("gtcsolo.machine.sehatch.desc.6"),
                        Component.translatable("gtcsolo.machine.sehatch.desc.7"))
                .overlayTieredHullRenderer(overlayName)
                .register();

        SPACEFORGE_ENERGY_HATCH.get(tier).put(amperage, def);
    }
}