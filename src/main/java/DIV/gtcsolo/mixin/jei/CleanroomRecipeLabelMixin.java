package DIV.gtcsolo.mixin.jei;

import DIV.gtcsolo.integration.jei.StyledJeiRecipeWidget;
import DIV.gtcsolo.recipe.condition.GtcsoloRecipeConditions;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.common.recipe.condition.CleanroomCondition;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GTRecipeWidget.class, remap = false)
public abstract class CleanroomRecipeLabelMixin {
    @Redirect(method = "setRecipeWidget", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/recipe/RecipeCondition;getTooltips()Lnet/minecraft/network/chat/Component;",
            ordinal = 1))
    private Component gtcsolo$cleanroomLabel(RecipeCondition condition) {
        if ((Object) this instanceof StyledJeiRecipeWidget
                && condition instanceof CleanroomCondition cleanroom && !cleanroom.isReverse()) {
            CleanroomType type = cleanroom.getCleanroom();
            if (type == CleanroomType.CLEANROOM) return Component.translatable("gtcsolo.jei.cleanroom.required");
            if (type == CleanroomType.STERILE_CLEANROOM) return Component.translatable("gtcsolo.jei.cleanroom.sterile_required");
            if (type == GtcsoloRecipeConditions.DAMPED_WASHING_CLEANROOM)
                return Component.translatable("gtcsolo.jei.cleanroom.damped_washing_required");
        }
        return condition.getTooltips();
    }
}
