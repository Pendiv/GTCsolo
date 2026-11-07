package DIV.gtcsolo.mixin.jei;

import DIV.gtcsolo.integration.jei.JeiRecipeBackgrounds;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An explicit background-only document must not suppress native item/fluid/chemical slots. */
@Mixin(value = GTRecipeTypeUI.class, remap = false)
public abstract class GTRecipeBackgroundOnlyMixin {
    @Shadow @Final private GTRecipeType recipeType;
    @Shadow public abstract CompoundTag getCustomUI();

    @Inject(method = "hasCustomUI", at = @At("RETURN"), cancellable = true, remap = false)
    private void gtcsolo$keepNativeLayout(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && JeiRecipeBackgrounds.usesDefaultLayout(getCustomUI())) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "reloadCustomUI", at = @At("RETURN"), remap = false)
    private void gtcsolo$reloadBackground(CallbackInfo callback) {
        JeiRecipeBackgrounds.invalidate(recipeType.registryName);
    }
}
