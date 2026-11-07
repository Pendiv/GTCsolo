package DIV.gtcsolo.machine.fusion;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import net.minecraft.network.chat.Component;

/** Native retained heat/cooldown, with expensive conversion only after a real recipe is ready. */
public final class MegaFusionMachine extends FusionReactorMachine {
    public MegaFusionMachine(IMachineBlockEntity holder) { super(holder,FusionMk4Machine.TIER); }

    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new ThermalRecipeLogic(this); }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        energyContainer.resetBasicInfo(MegaFusionRules.STORAGE,0,0,0,0);
        energyContainer.setEnergyStored(Math.min(energyContainer.getEnergyStored(),MegaFusionRules.STORAGE));
        updatePreHeatSubscription();
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof MegaFusionMachine reactor) || recipe.recipeType!=GTRecipeTypes.FUSION_RECIPES
                || RecipeHelper.getRecipeEUtTier(recipe)>reactor.getTier()) return ModifierFunction.NULL;
        // Pure selection: a candidate without enough batch inputs/output space must not burn startup EU.
        if (!reactor.canHeat(recipe)) return ModifierFunction.NULL;
        return MegaFusionRules::transform;
    }

    private boolean canHeat(GTRecipe recipe) {
        if (recipe==null || !recipe.data.contains("eu_to_start")) return false;
        long cost=MegaFusionRules.heatingCost(heat,recipe.data.getLong("eu_to_start"));
        return cost>=0 && energyContainer.getEnergyStored()>=cost;
    }

    private boolean ensureHeat(GTRecipe recipe) {
        if (!canHeat(recipe)) return false;
        long target=recipe.data.getLong("eu_to_start");
        long cost=MegaFusionRules.heatingCost(heat,target);
        if (cost>0) {
            energyContainer.removeEnergy(cost);
            heat=target;
            updatePreHeatSubscription();
        }
        return true;
    }

    @Override public boolean beforeWorking(GTRecipe recipe) {
        return super.beforeWorking(recipe) && ensureHeat(recipe);
    }

    @Override public void updateHeat() {
        // Running means no EU draw, including refilling the startup buffer.
        // Idle, paused and waiting states retain GT's cooling and charging rules.
        if (getRecipeLogic().isWorking() && isWorkingEnabled()) return;
        super.updateHeat();
    }

    /** Restores lost heat on resume without discarding already-consumed batch inputs. */
    private static final class ThermalRecipeLogic extends RecipeLogic {
        private final MegaFusionMachine reactor;
        private ThermalRecipeLogic(MegaFusionMachine machine) { super(machine); reactor=machine; }
        @Override public GTRecipe.ActionResult handleTickRecipe(GTRecipe recipe) {
            if (!reactor.ensureHeat(recipe))
                return GTRecipe.ActionResult.fail(()->Component.translatable("gtceu.recipe_logic.insufficient_fuel"));
            // The native onWorking now sees sufficient heat and cannot perform its 1 EU -> 1 heat conversion.
            return super.handleTickRecipe(recipe);
        }
    }
}
