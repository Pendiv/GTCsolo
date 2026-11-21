package DIV.gtcsolo.mixin.mekanism;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CachedRecipe.class, remap = false)
public interface CachedRecipeConditionsAccess {
    @Accessor("canHolderFunction")
    BooleanSupplier gtcsolo$getHolderCondition();

    @Accessor("requiredTicks")
    IntSupplier gtcsolo$getRequiredTicks();

    @Accessor("perTickEnergy")
    void gtcsolo$setPerTickEnergy(FloatingLongSupplier supplier);
}
