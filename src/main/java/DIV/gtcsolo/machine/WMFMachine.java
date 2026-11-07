package DIV.gtcsolo.machine;

import DIV.gtcsolo.util.GtcsoloRecipeModifiers;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;

import javax.annotation.Nonnull;

public class WMFMachine extends WorkableElectricMultiblockMachine {

    public WMFMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public static ModifierFunction industrialOverclock(@Nonnull MetaMachine machine, @Nonnull GTRecipe recipe) {
        return GtcsoloRecipeModifiers.industrialOverclock(machine, recipe);
    }
}