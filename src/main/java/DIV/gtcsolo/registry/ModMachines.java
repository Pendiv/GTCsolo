package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.CCMachine;
import DIV.gtcsolo.machine.FECMachine;
import DIV.gtcsolo.machine.WENMainStorageMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

public class ModMachines {

    public static final GTRegistrate REGISTRATE = GTRegistrate.create("gtcsolo");

    public static MultiblockMachineDefinition FEC;
    public static MultiblockMachineDefinition EEBF;
    public static MultiblockMachineDefinition CC;
    public static MultiblockMachineDefinition WEN_MAIN_STORAGE;

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
                                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2)))
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

        // WEN ワイヤレスIOマシン群
        WENMachines.init();
    }
}