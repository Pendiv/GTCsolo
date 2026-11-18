package DIV.gtcsolo.registry;

import DIV.gtcsolo.machine.LargeScaleChemicalReactorMachine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.resources.ResourceLocation;

import static com.gregtechceu.gtceu.api.pattern.Predicates.abilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.any;
import static com.gregtechceu.gtceu.api.pattern.Predicates.autoAbilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.api.pattern.Predicates.controller;
import static com.gregtechceu.gtceu.api.pattern.Predicates.frames;

public final class LargeScaleChemicalReactors {
    private LargeScaleChemicalReactors() {}

    public static void init() {
        ModMachines.REGISTRATE.multiblock("large_scale_chemical_reactor", LargeScaleChemicalReactorMachine::new)
                .tier(GTValues.EV)
                .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
                .recipeType(GTRecipeTypes.LARGE_CHEMICAL_RECIPES)
                .recipeModifiers(true, LargeScaleChemicalReactorMachine::processingMode)
                .appearanceBlock(GTBlocks.CASING_PTFE_INERT)
                .pattern(definition -> FactoryBlockPattern.start(
                                RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
                        .aisle("AAAAAAA", "B#ASA#B", "#######", "#######", "B#####B", "AAAAAAA")
                        .aisle("AAAAAAA", "BAAAAAB", "BAAAAAB", "BAAAAAB", "BAAAAAB", "AAAAAAA")
                        .aisle("AAAAAAA", "BA###AB", "#A###A#", "#A###A#", "BA###AB", "AAAAAAA")
                        .aisle("AAAAAAA", "BA###AB", "#A###A#", "#A###A#", "BA###AB", "AAAAAAA")
                        .aisle("AAAAAAA", "BAAAAAB", "BAAAAAB", "BAAAAAB", "BAAAAAB", "AAAAAAA")
                        .aisle("AAAAAAA", "B#####B", "#######", "#######", "BB###BB", "AAAAAAA")
                        .where('S', controller(blocks(definition.getBlock())))
                        .where('A', blocks(GTBlocks.CASING_PTFE_INERT.get())
                                .or(abilities(PartAbility.INPUT_ENERGY).setExactLimit(1).setPreviewCount(1))
                                .or(autoAbilities(definition.getRecipeTypes(), false, false, true, true, true, true))
                                .or(autoAbilities(true, false, true)))
                        .where('B', frames(GTMaterials.Roentgenium))
                        .where('#', any())
                        .build())
                .workableCasingRenderer(new ResourceLocation("gtceu", "block/casings/solid/machine_casing_inert_ptfe"),
                        new ResourceLocation("gtceu", "block/multiblock/large_chemical_reactor"))
                .register();
    }
}
