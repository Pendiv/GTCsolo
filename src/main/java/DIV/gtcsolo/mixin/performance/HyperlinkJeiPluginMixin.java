package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.IndexedMaterializerRecipes;
import DIV.gtcsolo.integration.jei.performance.JeiOptimizationOptions;
import DIV.gtcsolo.integration.jei.performance.JeiStartupPhases;
import com.sakurafuld.hyperdaimc.content.over.materializer.MaterializerHandler;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Leaves gameplay materialization and every other Hyperlink JEI registration unchanged. */
@Pseudo
@Mixin(targets = "com.sakurafuld.hyperdaimc.compat.jei.HyperJeiPlugin", remap = false)
public abstract class HyperlinkJeiPluginMixin {
    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE",
            target = "Lcom/sakurafuld/hyperdaimc/content/over/materializer/MaterializerHandler;loadRecipe(Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
    private List<MaterializerHandler.Process> gtcsolo$indexMaterializerCatalog(Level level) {
        boolean optimized = JeiOptimizationOptions.hyperlinkEnabled();
        return JeiStartupPhases.measure("hyperlink.materializer." + (optimized ? "indexed" : "native"),
                () -> optimized ? IndexedMaterializerRecipes.load(level) : MaterializerHandler.loadRecipe(level));
    }
}
