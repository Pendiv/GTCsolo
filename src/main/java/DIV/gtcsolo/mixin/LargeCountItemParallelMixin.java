package DIV.gtcsolo.mixin;

import DIV.gtcsolo.util.RecipeAmountMath;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BinaryOperator;

/** Keeps GT 1.6.4's item parallel inventory aggregation from wrapping at 2^31. */
@Mixin(value = ItemRecipeCapability.class, remap = false)
public abstract class LargeCountItemParallelMixin {
    @ModifyArg(method = "getIngredientStacks", at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Collectors;toMap(Ljava/util/function/Function;Ljava/util/function/Function;Ljava/util/function/BinaryOperator;Ljava/util/function/Supplier;)Ljava/util/stream/Collector;"),
            index = 2, require = 1)
    private BinaryOperator<Integer> gtcsolo$saturateWithinHandler(BinaryOperator<Integer> original) {
        return RecipeAmountMath::add;
    }

    // This is the version-pinned lambda passed to map.computeInt for separate input buses.
    @Inject(method = "lambda$getIngredientStacks$10", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gtcsolo$saturateAcrossHandlers(Object2IntMap.Entry<ItemStack> entry,
                                                       ItemStack key, Integer previous,
                                                       CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(previous == null ? entry.getIntValue()
                : RecipeAmountMath.add(previous, entry.getIntValue()));
    }
}
