package DIV.gtcsolo.mixin;

import DIV.gtcsolo.recipe.mold.MoldRecipeSupport;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Retain non-consumable recipe metadata while executing the mold's input-side wear operation. */
@Mixin(targets = "com.gregtechceu.gtceu.api.recipe.RecipeRunner", remap = false)
public abstract class IronMoldRecipeRunnerMixin {
    @Shadow @Final private IO io;
    @Shadow @Final private boolean isTick;

    @Redirect(method = "fillContent", at = @At(value = "FIELD",
            target = "Lcom/gregtechceu/gtceu/api/recipe/content/Content;chance:I"), require = 1)
    private int gtcsolo$executeMoldWear(Content content) {
        if (io == IO.IN && !isTick && content.content instanceof Ingredient ingredient
                && MoldRecipeSupport.mold(ingredient) != null) return content.maxChance;
        return content.chance;
    }
}
