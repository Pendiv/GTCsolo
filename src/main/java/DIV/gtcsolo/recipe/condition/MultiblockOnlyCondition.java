package DIV.gtcsolo.recipe.condition;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** A recipe condition, so increasing a single-block machine's output capacity cannot bypass it. */
public final class MultiblockOnlyCondition extends RecipeCondition {
    public static final Codec<MultiblockOnlyCondition> CODEC = RecordCodecBuilder.create(instance ->
            RecipeCondition.isReverse(instance).apply(instance, MultiblockOnlyCondition::new));

    public MultiblockOnlyCondition() { this(false); }
    public MultiblockOnlyCondition(boolean reverse) { super(reverse); }

    @Override public RecipeConditionType<?> getType() { return GtcsoloRecipeConditions.MULTIBLOCK_ONLY; }

    @Override public Component getTooltips() {
        return Component.translatable(isReverse ? "gtcsolo.recipe.condition.not_multiblock"
                : "gtcsolo.recipe.condition.multiblock_only");
    }

    @Override public IGuiTexture getInValidTexture() {
        return new ResourceTexture("gtceu:textures/gui/widget/information.png");
    }

    @Override public IGuiTexture getValidTexture() { return getInValidTexture(); }

    @Override public boolean test(@NotNull GTRecipe recipe, @NotNull RecipeLogic logic) {
        return logic.machine.self() instanceof IMultiController controller && controller.isFormed();
    }

    @Override public RecipeCondition createTemplate() { return new MultiblockOnlyCondition(); }
}
