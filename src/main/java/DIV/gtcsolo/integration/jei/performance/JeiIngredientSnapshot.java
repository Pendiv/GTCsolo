package DIV.gtcsolo.integration.jei.performance;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IClickableIngredient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ingredient-only counterpart of LDLib 1.0.48's slot collection, with no drawing/layout objects retained. */
public final class JeiIngredientSnapshot {
    private final Map<RecipeIngredientRole, Map<IIngredientType<?>, List<Object>>> ingredients =
            new EnumMap<>(RecipeIngredientRole.class);

    public void collect(IRecipeIngredientSlot slot) {
        RecipeIngredientRole role = ModularUIRecipeCategory.mapToRole(slot.getIngredientIO());
        for (Object value : slot.getXEIIngredients()) {
            if (!(value instanceof IClickableIngredient<?> clickable)) continue;
            if (role == null) {
                add(RecipeIngredientRole.INPUT, clickable);
                add(RecipeIngredientRole.OUTPUT, clickable);
            } else {
                add(role, clickable);
            }
        }
    }

    private void add(RecipeIngredientRole role, IClickableIngredient<?> ingredient) {
        ingredients.computeIfAbsent(role, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(ingredient.getIngredientType(), ignored -> new ArrayList<>())
                .add(ingredient.getIngredient());
    }

    public void write(IRecipeLayoutBuilder builder) {
        ingredients.forEach((role, types) -> types.forEach((type, values) -> addTo(builder, role, type, values)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addTo(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                              IIngredientType<?> type, List<Object> values) {
        builder.addInvisibleIngredients(role).addIngredients((IIngredientType) type, values);
    }
}
