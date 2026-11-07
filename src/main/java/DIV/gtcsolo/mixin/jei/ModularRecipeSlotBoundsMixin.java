package DIV.gtcsolo.mixin.jei;

import DIV.gtcsolo.integration.jei.ModularIngredientBounds;

import com.lowdragmc.lowdraglib.jei.IRecipeIngredientSlotWrapper;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.util.ImmutableRect2i;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Follow live LDLib widgets instead of JEI's layout-time rectangle snapshot. */
@Pseudo
@Mixin(targets = "mezz.jei.library.gui.ingredients.RecipeSlot", remap = false)
public abstract class ModularRecipeSlotBoundsMixin {
    @Shadow @Final private IDrawable overlay;
    @Shadow private ImmutableRect2i rect;

    @Unique
    private void gtcsolo$refreshBounds() {
        if (overlay instanceof IRecipeIngredientSlotWrapper wrapped) {
            var bounds = ModularIngredientBounds.visibleBounds(wrapped.slot.self());
            rect =
                    new ImmutableRect2i(
                            bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }
    }

    @Inject(
            method = {"isMouseOver", "getRect", "getAreaIncludingBackground"},
            at = @At("HEAD"))
    private void gtcsolo$beforeHitQuery(CallbackInfoReturnable<?> callback) {
        gtcsolo$refreshBounds();
    }

    @Inject(
            method = {"draw", "drawHighlight"},
            at = @At("HEAD"))
    private void gtcsolo$beforeDrawing(CallbackInfo callback) {
        gtcsolo$refreshBounds();
    }
}
