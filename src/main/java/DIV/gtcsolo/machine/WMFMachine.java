package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import javax.annotation.Nonnull;

public class WMFMachine extends WorkableElectricMultiblockMachine {

    // 工業OC: 各段で duration×0.9, EUt×4, 入出力×2
    private static final double INDUSTRIAL_DURATION_FACTOR = 0.9;
    private static final double INDUSTRIAL_EUT_FACTOR = 4.0;
    private static final double INDUSTRIAL_IO_FACTOR = 2.0;

    public WMFMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public static ModifierFunction industrialOverclock(@Nonnull MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof WMFMachine wmf)) {
            return RecipeModifier.nullWrongType(WMFMachine.class, machine);
        }

        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        int machineTier = wmf.getTier();
        if (recipeTier > machineTier) {
            return ModifierFunction.NULL;
        }

        int n = Math.max(0, machineTier - recipeTier);
        if (n == 0) return ModifierFunction.IDENTITY;

        double durationMult = Math.pow(INDUSTRIAL_DURATION_FACTOR, n);
        double eutMult = Math.pow(INDUSTRIAL_EUT_FACTOR, n);
        double ioMult = Math.pow(INDUSTRIAL_IO_FACTOR, n);
        int ioMultInt = (int) Math.round(ioMult);

        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(ioMult))
                .eutMultiplier(eutMult)
                .durationMultiplier(durationMult)
                .addOCs(n)
                .parallels(ioMultInt)
                .build();
    }
}