package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.RecipePower;
import DIV.gtcsolo.integration.mekanism.RecipeVoltage;
import mekanism.api.recipes.MekanismRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MekanismRecipe.class, remap = false)
public abstract class RecipeVoltageMixin implements RecipeVoltage {
    @Unique private RecipePower gtcsolo$power;

    @Override
    public RecipePower gtcsolo$getPower() {
        return gtcsolo$power;
    }

    @Override
    public void gtcsolo$setPower(RecipePower power) {
        gtcsolo$power = power;
    }
}
