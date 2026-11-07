package DIV.gtcsolo.integration.jei.performance;

import DIV.gtcsolo.Gtcsolo;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.common.recipe.condition.DimensionCondition;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guarded indexing path for native GTCEu item/fluid recipes. Real GUI layout always stays native.
 * Native capability conversion and widget binding preserve tags, circuits and non-consumable roles.
 */
public final class NativeGtRecipeIngredientIndex {
    private static final Field UI_BUILDER = uiBuilderField();
    private static final ThreadLocal<Boolean> VERIFYING_NATIVE = new ThreadLocal<>();
    private static final Map<String, Long> FALLBACKS = new LinkedHashMap<>();
    private static long indexed, verified, mismatches, failures;

    private NativeGtRecipeIngredientIndex() {}

    public static boolean isVerifyingNative() { return Boolean.TRUE.equals(VERIFYING_NATIVE.get()); }

    public static void resetStatistics() {
        indexed = verified = mismatches = failures = 0;
        FALLBACKS.clear();
    }

    public static void logStatistics() {
        Gtcsolo.LOGGER.info("[JEI-index] indexed={} verified={} mismatches={} failures={} fallback={}",
                indexed, verified, mismatches, failures, FALLBACKS);
    }

    public static boolean tryIndex(ModularUIRecipeCategory<?> category, IRecipeLayoutBuilder builder,
                                   GTRecipe recipe, IFocusGroup focuses) {
        if (!NativePreviewIngredientIndex.isIngredientIndexBuilder(builder)) return false;
        try {
            String reason = unsupportedReason(recipe);
            if (reason != null) return fallback(reason);
            JeiIngredientSnapshot snapshot = snapshot(recipe);
            if (JeiOptimizationOptions.recipeIndexVerificationEnabled()) {
                if (!matchesNative(category, builder, recipe, focuses, snapshot)) {
                    mismatches++;
                    if (mismatches <= 10) Gtcsolo.LOGGER.warn(
                            "[JEI-index] mismatch recipe={} type={}; using native index",
                            recipe.id, recipe.recipeType.registryName);
                    return fallback("index-mismatch");
                }
                verified++;
            }
            snapshot.write(builder);
            indexed++;
            return true;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            failures++;
            if (failures <= 3) Gtcsolo.LOGGER.warn("[JEI-index] fallback recipe={} reason={}", recipe.id, failure.toString());
            return fallback("index-exception");
        }
    }

    private static boolean fallback(String reason) {
        FALLBACKS.merge(reason, 1L, Long::sum);
        return false;
    }

    private static String unsupportedReason(GTRecipe recipe) throws IllegalAccessException {
        GTRecipeTypeUI ui = recipe.recipeType.getRecipeUI();
        if (ui.getClass() != GTRecipeTypeUI.class) return "ui-subclass";
        if (UI_BUILDER == null || UI_BUILDER.get(ui) != null) return "ui-extension";
        if (ui.hasCustomUI()) return "custom-ui";
        if (recipe.recipeType.isHasResearchSlot() || recipe.recipeType.isScanner()
                || recipe.conditions.stream().anyMatch(ResearchCondition.class::isInstance)) return "research-or-scanner";
        // GTRecipeWidget adds the dimension marker as an extra INPUT slot outside capability storage.
        if (recipe.conditions.stream().anyMatch(DimensionCondition.class::isInstance)) return "dimension-marker";
        for (Map<RecipeCapability<?>, List<Content>> map : List.of(
                recipe.inputs, recipe.tickInputs, recipe.outputs, recipe.tickOutputs)) {
            for (RecipeCapability<?> capability : map.keySet()) if (!known(capability)) return "custom-capability";
        }
        for (Map<RecipeCapability<?>, Integer> map : List.of(recipe.recipeType.maxInputs, recipe.recipeType.maxOutputs)) {
            for (var entry : map.entrySet()) if (entry.getValue() > 0 && !known(entry.getKey())) return "custom-capacity";
        }
        return null;
    }

    private static boolean known(RecipeCapability<?> capability) {
        return capability == ItemRecipeCapability.CAP || capability == FluidRecipeCapability.CAP
                || capability == EURecipeCapability.CAP;
    }

    private static JeiIngredientSnapshot snapshot(GTRecipe recipe) {
        Map<IO, Map<RecipeCapability<?>, List<Content>>> rows = new LinkedHashMap<>();
        rows.put(IO.IN, merged(recipe.inputs, recipe.tickInputs));
        rows.put(IO.OUT, merged(recipe.outputs, recipe.tickOutputs));
        Table<IO, RecipeCapability<?>, Object> storages = HashBasedTable.create();
        for (var direction : rows.entrySet()) {
            for (var entry : direction.getValue().entrySet()) {
                RecipeCapability<?> cap = entry.getKey();
                if (cap == EURecipeCapability.CAP) continue;
                List<Object> contents = cap.createXEIContainerContents(entry.getValue(), recipe, direction.getKey());
                int slots = maximum(recipe, cap, direction.getKey());
                while (contents.size() < slots) contents.add(null);
                Object storage = cap.createXEIContainer(contents);
                if (storage != null) storages.put(direction.getKey(), cap, storage);
            }
        }
        var holder = new GTRecipeTypeUI.RecipeHolder(ProgressWidget.JEIProgress,
                storages, recipe.data.copy(), recipe.conditions, false, false);
        JeiIngredientSnapshot snapshot = new JeiIngredientSnapshot();
        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        for (var direction : rows.entrySet()) {
            IO io = direction.getKey();
            for (var entry : direction.getValue().entrySet()) {
                RecipeCapability<?> cap = entry.getKey();
                if (cap == EURecipeCapability.CAP || !cap.doRenderSlot) continue;
                Object storage = storages.get(io, cap);
                if (storage == null) continue;
                List<Content> contents = entry.getValue();
                // Native padding slots are empty; scanner/research paths that can synthesize extra entries are excluded.
                int visibleContents = Math.min(maximum(recipe, cap, io), contents.size());
                for (int index = 0; index < visibleContents; index++) {
                    Widget widget = cap.createWidget();
                    if (!(widget instanceof IRecipeIngredientSlot slot))
                        throw new IllegalStateException("Native capability returned an unknown ingredient widget");
                    // These are the native UI-template binding and subsequent GTRecipeWidget content binding.
                    cap.applyWidgetInfo(widget, index, true, io, holder, recipe.recipeType, null, null, storage, 0, 0);
                    if (index < contents.size()) cap.applyWidgetInfo(widget, index, true, io, null,
                            recipe.recipeType, recipe, contents.get(index), null, recipeTier, 0);
                    // Native GTRecipeWidget propagates this when attaching the UI group; tanks snapshot their fluid here.
                    widget.setClientSideWidget();
                    snapshot.collect(slot);
                }
            }
        }
        return snapshot;
    }

    private static Map<RecipeCapability<?>, List<Content>> merged(Map<RecipeCapability<?>, List<Content>> regular,
                                                                 Map<RecipeCapability<?>, List<Content>> perTick) {
        // GTRecipeWidget's existing container table replaces a regular row with a per-tick row of the same cap.
        Map<RecipeCapability<?>, List<Content>> result = new LinkedHashMap<>(regular);
        result.putAll(perTick);
        return result;
    }

    private static int maximum(GTRecipe recipe, RecipeCapability<?> cap, IO io) {
        return io == IO.IN ? recipe.recipeType.getMaxInputs(cap) : recipe.recipeType.getMaxOutputs(cap);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean matchesNative(ModularUIRecipeCategory<?> category, IRecipeLayoutBuilder actual,
                                          GTRecipe recipe, IFocusGroup focuses, JeiIngredientSnapshot snapshot)
            throws ReflectiveOperationException {
        Field field = actual.getClass().getDeclaredField("ingredientManager");
        field.setAccessible(true);
        IIngredientManager manager = (IIngredientManager) field.get(actual);
        var constructor = actual.getClass().getConstructor(IIngredientManager.class);
        IRecipeLayoutBuilder nativeBuilder = (IRecipeLayoutBuilder) constructor.newInstance(manager);
        IRecipeLayoutBuilder fastBuilder = (IRecipeLayoutBuilder) constructor.newInstance(manager);
        VERIFYING_NATIVE.set(Boolean.TRUE);
        try {
            ((ModularUIRecipeCategory) category).setRecipe(nativeBuilder, recipe, focuses);
        } finally {
            VERIFYING_NATIVE.remove();
        }
        snapshot.write(fastBuilder);
        return fingerprint(nativeBuilder, manager).equals(fingerprint(fastBuilder, manager));
    }

    private static Map<RoleType, Set<IngredientKey>> fingerprint(IRecipeLayoutBuilder builder,
                                                                 IIngredientManager manager)
            throws ReflectiveOperationException {
        Object supplier = builder.getClass().getMethod("buildIngredientSupplier").invoke(builder);
        var getIngredients = supplier.getClass().getMethod("getIngredients", RecipeIngredientRole.class);
        Map<RoleType, Set<IngredientKey>> result = new HashMap<>();
        for (RecipeIngredientRole role : RecipeIngredientRole.values()) {
            Object values = getIngredients.invoke(supplier, role);
            if (!(values instanceof Collection<?> collection)) throw new IllegalStateException("Unknown ingredient supplier");
            for (Object value : collection) {
                if (!(value instanceof ITypedIngredient<?> typed)) throw new IllegalStateException("Unknown typed ingredient");
                result.computeIfAbsent(new RoleType(role, typed.getType()), ignored -> new HashSet<>())
                        .add(key(manager, typed));
            }
        }
        return result;
    }

    static boolean sameIngredientSuppliers(IRecipeLayoutBuilder one, IRecipeLayoutBuilder two,
                                           IIngredientManager manager) throws ReflectiveOperationException {
        return fingerprint(one, manager).equals(fingerprint(two, manager));
    }

    private static <T> IngredientKey key(IIngredientManager manager, ITypedIngredient<T> ingredient) {
        IIngredientHelper<T> helper = manager.getIngredientHelper(ingredient.getType());
        T value = ingredient.getIngredient();
        return new IngredientKey(helper.getUniqueId(value, UidContext.Recipe),
                helper.getUniqueId(value, UidContext.Ingredient), helper.getAmount(value));
    }

    private record IngredientKey(String recipeUid, String ingredientUid, long amount) {}

    private record RoleType(RecipeIngredientRole role, IIngredientType<?> type) {
        @Override public boolean equals(Object other) {
            return other instanceof RoleType key && role == key.role && type == key.type;
        }
        @Override public int hashCode() { return 31 * role.hashCode() + System.identityHashCode(type); }
    }

    private static Field uiBuilderField() {
        try {
            Field field = GTRecipeTypeUI.class.getDeclaredField("uiBuilder");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return null;
        }
    }
}
