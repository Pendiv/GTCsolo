package DIV.gtcsolo.mixin;

import DIV.gtcsolo.util.RecipeAmountMath;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.function.BinaryOperator;

/** Keeps same-fluid and matching-tag totals non-negative when GT limits parallels. */
@Mixin(value = FluidRecipeCapability.class, remap = false)
public abstract class LargeCountFluidParallelMixin {
    @ModifyArg(method = "getMaxParallelRatio", at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Collectors;toMap(Ljava/util/function/Function;Ljava/util/function/Function;Ljava/util/function/BinaryOperator;Ljava/util/function/Supplier;)Ljava/util/stream/Collector;"),
            index = 2, require = 1)
    private BinaryOperator<Integer> gtcsolo$saturateAcrossHandlers(BinaryOperator<Integer> original) {
        return RecipeAmountMath::add;
    }

    // GT 1.6.4: the second test is the consumable-fluid loop. Its only store to
    // local 11 is `available += inputFluid.getValue()`. Earlier, local 11 also
    // holds recipe fluidAmount, so the slice deliberately excludes that code.
    @ModifyVariable(method = "getMaxParallelRatio", at = @At("STORE"), index = 11,
            slice = @Slice(from = @At(value = "INVOKE", ordinal = 1,
                    target = "Lcom/gregtechceu/gtceu/api/recipe/ingredient/FluidIngredient;test(Lnet/minecraftforge/fluids/FluidStack;)Z")),
            require = 1)
    private int gtcsolo$saturateMatchingTagAmount(int sum) {
        return RecipeAmountMath.saturateWrappedSum(sum);
    }
}
