package DIV.gtcsolo.machine.fusion;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;

/** Native charging, heat persistence, cooldown, UI and recipe logic, in a different shell. */
public final class FusionVariantMachine extends FusionReactorMachine {
    private final FusionVariantSettings settings;

    public FusionVariantMachine(IMachineBlockEntity holder, FusionVariantSettings settings) {
        super(holder, settings.tier());
        this.settings = settings;
    }

    public FusionVariantSettings settings() { return settings; }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        long capacity = settings.startupCapacity(energyContainer.getEnergyCapacity());
        if (capacity != energyContainer.getEnergyCapacity()) {
            // Match the existing MK IV's 1,280 MEU, not the capacity of its 32 hatch inputs.
            energyContainer.resetBasicInfo(capacity, 0, 0, 0, 0);
            energyContainer.setEnergyStored(Math.min(energyContainer.getEnergyStored(), capacity));
            updatePreHeatSubscription();
        }
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof FusionVariantMachine reactor)) return ModifierFunction.NULL;
        if (reactor.settings.fusionOverclocking()) return FusionReactorMachine.recipeModifier(machine, recipe);
        // Same startup charging and no-OC policy as the existing FusionMk4Machine.
        if (RecipeHelper.getRecipeEUtTier(recipe) > reactor.getTier() || !recipe.data.contains("eu_to_start"))
            return ModifierFunction.NULL;
        long startup = recipe.data.getLong("eu_to_start");
        if (startup < 0 || startup > reactor.energyContainer.getEnergyCapacity()) return ModifierFunction.NULL;
        long difference = startup - reactor.heat;
        if (difference > 0) {
            if (reactor.energyContainer.getEnergyStored() < difference) return ModifierFunction.NULL;
            reactor.energyContainer.removeEnergy(difference);
            reactor.heat += difference;
            reactor.updatePreHeatSubscription();
        }
        return ModifierFunction.IDENTITY;
    }
}
