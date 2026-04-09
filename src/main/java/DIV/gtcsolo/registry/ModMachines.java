package DIV.gtcsolo.registry;

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

    public static void init() {
        FEC = REGISTRATE.multiblock("fec", CoilWorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ModRecipeTypes.FEC)
                .recipeModifiers(GTRecipeModifiers::ebfOverclock, GTRecipeModifiers.PARALLEL_HATCH)
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
                                .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                                .or(abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('Y', controller(blocks(definition.getBlock())))
                        .build())
                .workableCasingModel(
                        new ResourceLocation("gtcsolo", "block/high_strength_steel_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/assembly_line"))
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

        EEBF = REGISTRATE.multiblock("extended_electric_blast_furnace", CoilWorkableElectricMultiblockMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.BLAST_RECIPES)
                .recipeModifiers(GTRecipeModifiers::ebfOverclock)
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
                .workableCasingModel(
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
    }
}