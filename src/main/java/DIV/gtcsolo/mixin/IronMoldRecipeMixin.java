package DIV.gtcsolo.mixin;

import DIV.gtcsolo.recipe.mold.MoldRecipeSupport;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.Map;

@Mixin(value = GTRecipe.class, remap = false)
public abstract class IronMoldRecipeMixin {
    @Shadow @Final @Mutable public Map<RecipeCapability<?>, List<Content>> inputs;
    @Inject(method = "<init>*", at = @At("RETURN"), require = 2)
    private void gtcsolo$acceptIronMolds(CallbackInfo callback) {
        var extended = MoldRecipeSupport.extend(inputs);
        if (extended != null) inputs = extended;
    }
}
