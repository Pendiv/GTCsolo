package DIV.gtcsolo.integration.jei;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

/** Created only by JEI's wrapper; machine screens and other recipe viewers keep native widgets. */
public final class StyledJeiRecipeWidget extends GTRecipeWidget {
    private final GTRecipe styledRecipe;

    public StyledJeiRecipeWidget(GTRecipe recipe) {
        super(recipe);
        styledRecipe = recipe;
        applyStyle();
    }

    @Override
    public void setRecipeOC(int button, boolean shiftClick) {
        super.setRecipeOC(button, shiftClick);
        // GT recreates condition/data labels when the displayed overclock changes.
        applyStyle();
    }

    private void applyStyle() {
        Integer color = JeiRecipeBackgrounds.informationColorForType(styledRecipe.recipeType);
        var background = JeiRecipeBackgrounds.backgroundForType(styledRecipe.recipeType);
        if (background != null) {
            // GT's outer widget includes the automatic time/EU/condition information below the slots.
            setBackground(new RecipeCardBackground(background, this::getSelfPositionX));
        }
        for (Widget widget : widgets) {
            if (color != null && widget instanceof LabelWidget label) {
                // The RTUI itself is a child group: leave its authored labels and slot amounts alone.
                // Native section codes and independently styled child Components keep their colors.
                label.setTextColor(color);
            }
        }
    }
}
