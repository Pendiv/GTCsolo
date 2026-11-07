package DIV.gtcsolo.integration.jei.performance;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reuses the exact input stacks already computed by GTCEu 1.6.4's first native preview page.
 * It does not build a shape, clone a block, or retain a second cache. Unknown layouts use the native path.
 */
public final class NativePreviewIngredientIndex {
    private static final int NATIVE_VISIBLE_PART_SLOTS = 18;
    private static final Field CACHE = field(PatternPreviewWidget.class, "CACHE");
    private static final Field PARTS = patternPartsField();

    private NativePreviewIngredientIndex() {}

    public static boolean isIngredientIndexBuilder(IRecipeLayoutBuilder builder) {
        // The real layout must still run LDLib's visible-slot integration and tooltips.
        return builder.getClass().getName().equals(
                "mezz.jei.library.gui.recipes.supplier.builder.IngredientSupplierBuilder");
    }

    public static boolean addCachedInputs(IRecipeLayoutBuilder builder, MultiblockMachineDefinition definition) {
        Optional<List<ItemStack>> inputs = cachedInputs(definition);
        if (inputs.isEmpty()) return false;
        if (!inputs.get().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(inputs.get());
        }
        return true;
    }

    public static Optional<List<ItemStack>> cachedInputs(MultiblockMachineDefinition definition) {
        Optional<List<ItemStack>> deferred = LazyNativeMultiblockPreviews.cachedInputs(definition);
        if (deferred.isPresent()) return deferred;
        if (CACHE == null || PARTS == null) return Optional.empty();
        try {
            Object value = CACHE.get(null);
            if (!(value instanceof Map<?, ?> cache)) return Optional.empty();
            Object patterns = cache.get(definition);
            if (patterns == null || !patterns.getClass().isArray()) return Optional.empty();
            if (Array.getLength(patterns) == 0) return Optional.of(List.of());
            Object first = Array.get(patterns, 0);
            if (first == null) return Optional.empty();
            Object parts = PARTS.get(first);
            if (!(parts instanceof List<?> lists)) return Optional.empty();
            return copyIndexedInputs(lists);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<List<ItemStack>> copyIndexedInputs(List<?> parts) {
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < Math.min(parts.size(), NATIVE_VISIBLE_PART_SLOTS); i++) {
            if (!(parts.get(i) instanceof List<?> alternatives)) return Optional.empty();
            for (Object alternative : alternatives) {
                if (!(alternative instanceof ItemStack stack)) return Optional.empty();
                if (!stack.isEmpty()) inputs.add(stack.copy());
            }
        }
        return Optional.of(inputs);
    }

    private static Field patternPartsField() {
        for (Class<?> nested : PatternPreviewWidget.class.getDeclaredClasses()) {
            if (nested.getSimpleName().equals("MBPattern")) return field(nested, "parts");
        }
        return null;
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
