package DIV.gtcsolo.machine.fusion;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;

/** UHV fusion with native heat/charging/cooling, fixed storage and no overclocking. */
public final class FusionMk4Machine extends FusionReactorMachine {
    public static final int TIER = GTValues.UHV;
    public static final int ENERGY_PORTS = 32;
    public static final long STARTUP_CAPACITY = 1_280_000_000L;
    public static final int CENTER_DISTANCE = 8;

    public FusionMk4Machine(IMachineBlockEntity holder) {
        super(holder, TIER);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        // Native UHV storage would double with the new 32 ports. Keep the specified 1,280 MEU.
        energyContainer.resetBasicInfo(STARTUP_CAPACITY, 0, 0, 0, 0);
        energyContainer.setEnergyStored(Math.min(energyContainer.getEnergyStored(), STARTUP_CAPACITY));
        updatePreHeatSubscription();
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof FusionMk4Machine reactor)
                || RecipeHelper.getRecipeEUtTier(recipe) > TIER
                || !recipe.data.contains("eu_to_start")) return ModifierFunction.NULL;
        long startup = recipe.data.getLong("eu_to_start");
        if (startup < 0 || startup > reactor.energyContainer.getEnergyCapacity()) return ModifierFunction.NULL;
        long difference = startup - reactor.heat;
        if (difference > 0) {
            if (reactor.energyContainer.getEnergyStored() < difference) return ModifierFunction.NULL;
            reactor.energyContainer.removeEnergy(difference);
            reactor.heat += difference;
            reactor.updatePreHeatSubscription();
        }
        // Preserve the recipe's EU/t, duration and input/output quantities, without FUSION_OC.
        return ModifierFunction.IDENTITY;
    }
}
