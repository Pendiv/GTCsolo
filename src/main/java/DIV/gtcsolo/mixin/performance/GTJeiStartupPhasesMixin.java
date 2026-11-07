package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.JeiStartupPhases;
import DIV.gtcsolo.integration.jei.performance.JeiOptimizationOptions;
import DIV.gtcsolo.integration.jei.performance.NativeGtRecipeIngredientIndex;
import DIV.gtcsolo.integration.jei.performance.JeiSetterLookup;
import DIV.gtcsolo.integration.jei.performance.LazyNativeMultiblockPreviews;
import com.gregtechceu.gtceu.integration.jei.GTJEIPlugin;
import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoCategory;
import com.gregtechceu.gtceu.integration.jei.oreprocessing.GTOreProcessingInfoCategory;
import com.gregtechceu.gtceu.integration.jei.orevein.GTBedrockFluidInfoCategory;
import com.gregtechceu.gtceu.integration.jei.orevein.GTBedrockOreInfoCategory;
import com.gregtechceu.gtceu.integration.jei.orevein.GTOreVeinInfoCategory;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory;
import mezz.jei.api.registration.IRecipeRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Profiling-only subphase boundaries around the existing GTCEu 1.6.4 calls. */
@Mixin(value = GTJEIPlugin.class, remap = false)
public abstract class GTJeiStartupPhasesMixin {
    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/integration/jei/multipage/MultiblockInfoCategory;registerRecipes(Lmezz/jei/api/registration/IRecipeRegistration;)V"))
    private void gtcsolo$timeMultiblocks(IRecipeRegistration registration) {
        JeiStartupPhases.run("gtceu.multiblock-previews",
                () -> LazyNativeMultiblockPreviews.runRegistration(registration,
                        () -> MultiblockInfoCategory.registerRecipes(registration)));
    }

    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/integration/jei/recipe/GTRecipeJEICategory;registerRecipes(Lmezz/jei/api/registration/IRecipeRegistration;)V"))
    private void gtcsolo$timeRecipes(IRecipeRegistration registration) {
        if (JeiOptimizationOptions.recipeIndexEnabled()) NativeGtRecipeIngredientIndex.resetStatistics();
        try {
            JeiStartupPhases.run("gtceu.processing-recipes",
                    () -> JeiSetterLookup.runScoped(() -> GTRecipeJEICategory.registerRecipes(registration)));
        } finally {
            if (JeiOptimizationOptions.recipeIndexEnabled()) NativeGtRecipeIngredientIndex.logStatistics();
        }
    }

    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/integration/jei/oreprocessing/GTOreProcessingInfoCategory;registerRecipes(Lmezz/jei/api/registration/IRecipeRegistration;)V"))
    private void gtcsolo$timeOreProcessing(IRecipeRegistration registration) {
        JeiStartupPhases.run("gtceu.ore-processing", () -> GTOreProcessingInfoCategory.registerRecipes(registration));
    }

    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/integration/jei/orevein/GTOreVeinInfoCategory;registerRecipes(Lmezz/jei/api/registration/IRecipeRegistration;)V"))
    private void gtcsolo$timeOreVeins(IRecipeRegistration registration) {
        JeiStartupPhases.run("gtceu.ore-veins", () -> GTOreVeinInfoCategory.registerRecipes(registration));
    }

    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/integration/jei/orevein/GTBedrockFluidInfoCategory;registerRecipes(Lmezz/jei/api/registration/IRecipeRegistration;)V"))
    private void gtcsolo$timeBedrockFluids(IRecipeRegistration registration) {
        JeiStartupPhases.run("gtceu.bedrock-fluids", () -> GTBedrockFluidInfoCategory.registerRecipes(registration));
    }

    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/integration/jei/orevein/GTBedrockOreInfoCategory;registerRecipes(Lmezz/jei/api/registration/IRecipeRegistration;)V"))
    private void gtcsolo$timeBedrockOres(IRecipeRegistration registration) {
        JeiStartupPhases.run("gtceu.bedrock-ores", () -> GTBedrockOreInfoCategory.registerRecipes(registration));
    }
}
