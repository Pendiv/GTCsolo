package DIV.gtcsolo.mixin;

import DIV.gtcsolo.util.RecipeAmountMath;
import com.gregtechceu.gtceu.utils.GTHashMaps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Fixes the per-handler fluid sum before FluidRecipeCapability merges handlers. */
@Mixin(value = GTHashMaps.class, remap = false)
public abstract class LargeCountFluidCollectionMixin {
    @Redirect(method = "fromFluidCollection(Ljava/util/Collection;)Ljava/util/Map;",
            at = @At(value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap;put(Ljava/lang/Object;I)I"),
            require = 1)
    private static int gtcsolo$saturateWithinHandler(Object2IntMap<Object> map, Object key, int sum) {
        // GT has already filtered out empty / non-positive fluids; this put is
        // immediately preceded by existing amount + incoming positive amount.
        return map.put(key, RecipeAmountMath.saturateWrappedSum(sum));
    }
}
