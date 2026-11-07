package DIV.gtcsolo.mixin;

import DIV.gtcsolo.recipe.mold.MoldRecipeSupport;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = NotifiableItemStackHandler.class, remap = false)
public abstract class IronMoldInputMixin {
    @Inject(method = "handleRecipe", at = @At("HEAD"), cancellable = true)
    private static void gtcsolo$wearIronMolds(IO io, GTRecipe recipe, List<Ingredient> left, boolean simulate,
                                             IO handlerIO, CustomItemStackHandler storage,
                                             CallbackInfoReturnable<List<Ingredient>> callback) {
        if (io == IO.IN && handlerIO == IO.IN && left.stream().anyMatch(input -> MoldRecipeSupport.mold(input) != null))
            callback.setReturnValue(MoldRecipeSupport.handle(recipe, left, storage, simulate));
    }
}
