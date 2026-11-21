package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.RecipeCleanroom;
import DIV.gtcsolo.integration.mekanism.RecipePower;
import DIV.gtcsolo.integration.mekanism.RecipeVoltage;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import mekanism.api.recipes.MekanismRecipe;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MekanismRecipe.class, remap = false)
public abstract class RecipeVoltageMixin implements RecipeVoltage, RecipeCleanroom {
    @Unique @Nullable private CleanroomType gtcsolo$requiredCleanroom;
    @Unique private RecipePower gtcsolo$power;

    @Override
    public RecipePower gtcsolo$getPower() {
        return gtcsolo$power;
    }

    @Override
    public void gtcsolo$setPower(RecipePower power) {
        gtcsolo$power = power;
    }

    @Override
    public @Nullable CleanroomType gtcsolo$getRequiredCleanroom() {
        return gtcsolo$requiredCleanroom;
    }

    @Override
    public void gtcsolo$setRequiredCleanroom(@Nullable CleanroomType type) {
        gtcsolo$requiredCleanroom = type;
    }
}
