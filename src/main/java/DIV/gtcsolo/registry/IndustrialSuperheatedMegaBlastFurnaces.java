package DIV.gtcsolo.registry;

import DIV.gtcsolo.registry.material.DraconicMaterials;
import DIV.gtcsolo.machine.IndustrialSuperheatedMegaBlastFurnaceMachine;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GCYMRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/** Exact 15 x 28 x 15 export; only M is replaced by the production controller. */
public final class IndustrialSuperheatedMegaBlastFurnaces {
    public static MultiblockMachineDefinition CORE;
    private IndustrialSuperheatedMegaBlastFurnaces() {}

    public static void init() {
        CORE = ModMachines.REGISTRATE.multiblock("industrial_superheated_mega_blast_furnace",
                        IndustrialSuperheatedMegaBlastFurnaceMachine::new)
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(GTRecipeTypes.BLAST_RECIPES, GCYMRecipeTypes.ALLOY_BLAST_RECIPES,
                        ModRecipeTypes.EXTREME_TEMPERATURE_BLAST_FURNACE, GTRecipeTypes.FURNACE_RECIPES)
                .recipeModifiers(GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT)
                .alwaysTryModifyRecipe(true)
                .tooltips(Component.translatable("gtcsolo.ismbf.tooltip.summary"),
                        Component.translatable("gtcsolo.ismbf.tooltip.detail"))
                .appearanceBlock(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING)
                .pattern(IndustrialSuperheatedMegaBlastFurnaces::pattern)
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/gcym/high_temperature_smelting_casing"),
                        new ResourceLocation("gtceu", "block/multiblock/electric_blast_furnace"))
                .register();
    }

    private static BlockPattern pattern(MultiblockMachineDefinition definition) {
        return FactoryBlockPattern.start()
                .aisle("###AAAAAAAAA###", "###AAAAAAAAA###", "###############", "###############", "###############", "###############", "###BBBBBBBBBC##", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###DDDDDDDDD###", "###AAAAAAAAA###", "###AAAAAAAAA###", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###AAAAAAAAA###", "###AAAAAAAAA###")
                .aisle("##AAAAAAAAAAA##", "##AAAAAAAAAAA##", "###############", "###############", "###############", "##CCCCCCCCCC###", "#CBBBBBBBBBBBC#", "##DDD#####DDD##", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###DDD###DDD###", "##DAAAAAAAAAD##", "##AAAAAAAAAAA##", "##AAAAAAAAAAA##", "###CC#####CC###", "###############", "###############", "###############", "###############", "###############", "###CC#####CC###", "##AAAAAAAAAAA##", "##AAAAAEAAAAA##")
                .aisle("#AAAAAAAAAAAAA#", "#AAAAFFFFFAAAA#", "##CCAEEEEEACC##", "##C#########C##", "##C#########C##", "#CC########CC##", "CBBBBEEEEEBBBBC", "#DD#########DD#", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "##DD#######DD##", "#DAAAAAAAAAAAD#", "#AAAAAAAAAAAAA#", "#AAAAAAAAAAAAA#", "##CCC#####CCC##", "##C#########C##", "##C#########C##", "##C#########C##", "##C#########C##", "##C#########C##", "##CCC#####CCC##", "#AAAAAAAAAAAAA#", "#AAAAAAEAAAAAA#")
                .aisle("AAAAAAAAAAAAAAA", "AAAAFAAAAAFAAAA", "##CAAGGGGGAAC##", "#####HIEIH#####", "#####HIEIH#####", "#C###HIEIH##CC#", "BBBBBBBEBBBBBBB", "#D###########D#", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "#DD#########DD#", "DAAAAAAAAAAAAAD", "AAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAA", "#CC#########CC#", "###############", "###############", "###############", "###############", "###############", "#CC#########C##", "AAAAAAAAAAAAAAA", "AAAAAAAEAAAAAAA")
                .aisle("AAAAAAAAAAAAAAA", "AAAFAAAAAAAFAAA", "##AAGGGGGGGAA##", "####JAIIIEJ####", "####JAIIIEJ####", "#C##JAIIIEJ##C#", "BBBBBBIIIBBBBBB", "#D##EHHJHHE##D#", "#####EIIIE#####", "###############", "###############", "###############", "###############", "###############", "#####EIIIE#####", "#D##EHHJHHE##D#", "DAAAAAAAAAAAAAD", "AAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAA", "#CC#EEJJJEE#CC#", "####EHHJHHE####", "####E#####E####", "####E#####E####", "####E#####E####", "####EHHJHHE####", "#CC#EEJJJEE#CC#", "AAAAAEEEEEAAAAA", "AAAAAEEEEEAAAAA")
                .aisle("AAAAAAAAAAAAAAA", "AAFAAAAAAAAAFAA", "##EGGGGGGGGGE##", "###HAGKKKGEH###", "###HAGKKKGEH###", "#C#HAGKKKGEE#C#", "BBEBBGKKKGBBEBB", "####HGKKKGH####", "####EGKKKGE####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "####EGKKKGE####", "#D##HGKKKGH##D#", "DAAAAGKKKGAAAAD", "AAAAAGKKKGAAAAA", "AAAAAGKKKGAAAAA", "####EGKKKGE####", "####HGKKKGH####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "####HGKKKGH####", "####EGKKKGE####", "AAAAEGKKKGEAAAA", "AAAAEAIIIAEAAAA")
                .aisle("AAAAAAAAAAAAAAA", "AAFAAAAAAAAAFAA", "##EGGGGGGGGGE##", "###IIK###KII###", "###IIK###KII###", "#C#IIK###KII#C#", "BBEBIK###KIBEBB", "####HK###KH####", "####IK###KI####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "####IK###KI####", "####HK###KH####", "DAAAAK###KAAAAD", "AAAAAK###KAAAAA", "AAAAAK###KAAAAA", "####JK###KJ####", "####HK###KH####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "####HK###KH####", "####JK###KJ####", "AAAAEK###KEAAAA", "AAAAEIIIIIEAAAA")
                .aisle("AAAAAAAAAAAAAAA", "AAFAAAAAAAAAFAA", "##EGGGGGGGGGE##", "###EIK###KIE###", "###EIK###KIE###", "#C#EIK###KIE#C#", "BBEEIK###KIEEBB", "####JK###KJ####", "####IK###KI####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "####IK###KI####", "####JK###KJ####", "DAAAAK###KAAAAD", "AAAAAK###KAAAAA", "AAAAAK###KAAAAA", "####JK###KJ####", "####JK###KJ####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "####JK###KJ####", "####JK###KJ####", "AAAAEK###KEAAAA", "AEEEEIILIIEEEEA")
                .aisle("AAAAAAAAAAAAAAA", "AAFAAAAAAAAAFAA", "##EGGGGGGGGGE##", "###IIK###KII###", "###IIK###KII###", "#C#IIK###KII#C#", "BBEBIK###KIBEBB", "####HK###KH####", "####IK###KI####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "####IK###KI####", "####HK###KH####", "DAAAAK###KAAAAD", "AAAAAK###KAAAAA", "AAAAAK###KAAAAA", "####JK###KJ####", "####HK###KH####", "#####K###K#####", "#####K###K#####", "#####K###K#####", "####HK###KH####", "####JK###KJ####", "AAAAEK###KEAAAA", "AAAAEIIIIIEAAAA")
                .aisle("AAAAAAAAAAAAAAA", "AAFAAAAAAAAAFAA", "##EGGGGGGGGGE##", "###HAGKKKGAH###", "###HAGKKKGAH###", "#C#HAGKKKGAH#C#", "BBEBBGKKKGBBEBB", "####HGKKKGH####", "####EGKKKGE####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "####EGKKKGE####", "####HGKKKGH##D#", "DAAAAGKKKGAAAAD", "AAAAAGKKKGAAAAA", "AAAAAGKKKGAAAAA", "####EGKKKGE####", "####HGKKKGH####", "#####GKKKG#####", "#####GKKKG#####", "#####GKKKG#####", "####HGKKKGH####", "####EGKKKGE####", "AAAAEGKKKGEAAAA", "AAAAEAIIIAEAAAA")
                .aisle("AAAAAAAAAAAAAAA", "AAAFAAAAAAAFAAA", "##AAGGGGGGGAA##", "####JAIIIAJ####", "####JAIIIAJ####", "#C##JAIIIAJ##C#", "BBBBBBIIIBBBBBB", "#D##EHHJHHE##D#", "#####EIIIE#####", "###############", "###############", "###############", "###############", "###############", "#####EIIIE#####", "#D##EHHJHHE##D#", "DAAAAAAAAAAAAAD", "AAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAA", "#CC#EEJJJEE#CC#", "####EHHJHHE####", "####E#####E####", "####E#####E####", "####E#####E####", "####EHHJHHE####", "#CC#EEJJJEE#CC#", "AAAAAEEEEEAAAAA", "AAAAAEEEEEAAAAA")
                .aisle("AAAAAAAAAAAAAAA", "AAAAFAAAAAFAAAA", "##CAAGGGGGAAC##", "#####HIEIH#####", "#####HIEIH#####", "#C###HIEIH###C#", "BBBBBBBEBBBBBBB", "#D###########D#", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "#D##########DD#", "DAAAAAAAAAAAAAD", "AAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAA", "#CC#########CC#", "###############", "###############", "###############", "###############", "###############", "#CC#########CC#", "AAAAAAAAAAAAAAA", "AAAAAAAEAAAAAAA")
                .aisle("#AAAAAAAAAAAAA#", "#AAAAFFFFFAAAA#", "##CCAEEEEEACC##", "##C#########C##", "##C#########C##", "#CC#########CC#", "CBBBBEEEEEBBBBC", "#DD#########DD#", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "##D#########D##", "#DD########DD##", "#DAAAAAAAAAAAD#", "#AAAAAAAAAAAAA#", "#AAAAAAAAAAAAA#", "##CCC#####CCC##", "##C#########C##", "##C#########C##", "##C#########C##", "##C#########C##", "##C#########C##", "##CCC#####CCC##", "#AAAAAAAAAAAAA#", "#AAAAAAEAAAAAA#")
                .aisle("##AAAAAAAAAAA##", "##AAAAAAAAAAA##", "###############", "###############", "###############", "##CCCCCCCCCCC##", "#CBBBBBBBBBBBC#", "##DDD#####DDD##", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "##DDD####DDD###", "##DAAAAAAAAAD##", "##AAAAAAAAAAA##", "##AAAAAAAAAAA##", "###CC#####CC###", "###############", "###############", "###############", "###############", "###############", "###CC#####CC###", "##AAAAAAAAAAA##", "##AAAAAEAAAAA##")
                .aisle("###AAAAAAAAA###", "###AAAAMAAAA###", "###############", "###############", "###############", "###############", "##CBBBBBBBBBC##", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###DDDDDDDDD###", "###AAAAAAAAA###", "###AAAAAAAAA###", "###############", "###############", "###############", "###############", "###############", "###############", "###############", "###AAAAAAAAA###", "###AAAAAAAAA###")
                .where('M', controller(blocks(definition.getBlock())))
                .where('A', blocks(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get())
                        .or(autoAbilities(definition.getRecipeTypes(), false, false, true, true, true, true))
                        .or(abilities(PartAbility.INPUT_ENERGY, PartAbility.INPUT_LASER, PartAbility.SUBSTATION_INPUT_ENERGY)
                                .setMinGlobalLimited(1).setPreviewCount(1))
                        .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1)))
                .where('B', blocks(ModBlocks.MITHRILITE_ROBUST_MACHINE_CASING.get()))
                .where('C', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, ModMaterials.MITHRILITE)))
                .where('D', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, DraconicMaterials.DRACONIUM)))
                .where('E', blocks(ModBlocks.MITHRILITE_PIPE_CASING.get()))
                .where('F', blocks(ModBlocks.HIGH_STRENGTH_STEEL_CASING.get()))
                .where('G', blocks(ModBlocks.REFINED_OBSIDIAN_CASING.get()))
                .where('H', blocks(ModBlocks.MITHRILITE_FIREBOX_CASING.get()))
                .where('I', blocks(GCYMBlocks.HEAT_VENT.get()))
                .where('J', blocks(ModBlocks.MITHRILITE_EXTREME_ENGINE_INTAKE_CASING.get()))
                .where('K', heatingCoils())
                .where('L', abilities(PartAbility.MUFFLER).setExactLimit(1))
                .where('#', any())
                .build();
    }
}
