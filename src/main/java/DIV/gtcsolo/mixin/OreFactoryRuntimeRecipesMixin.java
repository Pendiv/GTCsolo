package DIV.gtcsolo.mixin;

import DIV.gtcsolo.recipe.orefactory.OreFactoryRecipes;
import DIV.gtcsolo.registry.OreProcessingFactoryMachines;
import dev.latvian.mods.kubejs.recipe.RecipesEventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** GT has finished rebuilding typed KJS recipes and lookups; the manager has not published them yet. */
@Pseudo
@Mixin(targets="com.gregtechceu.gtceu.integration.kjs.GregTechKubeJSPlugin",remap=false)
public abstract class OreFactoryRuntimeRecipesMixin {
    @Inject(method="injectRuntimeRecipes",at=@At("TAIL"),remap=false,require=1)
    private void gtcsolo$aggregateOreRoutes(RecipesEventJS event, RecipeManager manager,
                                           Map<ResourceLocation, Recipe<?>> recipes, CallbackInfo callback) {
        if(OreProcessingFactoryMachines.CORE!=null) OreFactoryRecipes.inject(recipes);
    }
}
