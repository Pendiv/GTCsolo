package DIV.gtcsolo.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class RecipeDumpService {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public RecipeDumpModels.DumpExecutionResult dump(MinecraftServer server, ResourceLocation recipeTypeId) throws IOException {
        RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.getOptional(recipeTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown recipe type: " + recipeTypeId));

        List<Recipe<?>> matchingRecipes = server.getRecipeManager()
                .getRecipes()
                .stream()
                .filter(recipe -> recipe.getType() == recipeType)
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                .toList();

        Map<String, Object> schema = createSchema();
        Map<String, Object> defaults = createDefaults();

        List<Map<String, Object>> recipes = new ArrayList<>();
        for (Recipe<?> recipe : matchingRecipes) {
            recipes.add(toRecipeEntry(server, recipe, defaults));
        }

        RecipeDumpModels.DumpFile dumpFile = new RecipeDumpModels.DumpFile(
                recipeTypeId.toString(),
                schema,
                defaults,
                recipes
        );

        Path outputPath = resolveOutputPath(recipeTypeId);
        writeJson(outputPath, dumpFile);

        return new RecipeDumpModels.DumpExecutionResult(recipeTypeId, outputPath, recipes.size());
    }

    private Map<String, Object> createSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("id", "string");
        schema.put("serializer", "string|null");
        schema.put("group", "string");
        schema.put("ingredients", "list");
        schema.put("result_item", "object|null");

        schema.put("item_inputs", "list");
        schema.put("fluid_inputs", "list");
        schema.put("item_outputs", "list");
        schema.put("fluid_outputs", "list");

        schema.put("tick_item_inputs", "list");
        schema.put("tick_fluid_inputs", "list");
        schema.put("tick_item_outputs", "list");
        schema.put("tick_fluid_outputs", "list");

        schema.put("duration", "int");
        schema.put("input_eut", "int");
        schema.put("output_eut", "int");
        schema.put("eut", "int");

        schema.put("special", "object");
        return schema;
    }

    private Map<String, Object> createDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("serializer", null);
        defaults.put("group", "");
        defaults.put("ingredients", List.of());
        defaults.put("result_item", null);

        defaults.put("item_inputs", List.of());
        defaults.put("fluid_inputs", List.of());
        defaults.put("item_outputs", List.of());
        defaults.put("fluid_outputs", List.of());

        defaults.put("tick_item_inputs", List.of());
        defaults.put("tick_fluid_inputs", List.of());
        defaults.put("tick_item_outputs", List.of());
        defaults.put("tick_fluid_outputs", List.of());

        defaults.put("duration", 0);
        defaults.put("input_eut", 0);
        defaults.put("output_eut", 0);
        defaults.put("eut", 0);

        defaults.put("special", Map.of());
        return defaults;
    }

    private Map<String, Object> toRecipeEntry(
            MinecraftServer server,
            Recipe<?> recipe,
            Map<String, Object> defaults
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", recipe.getId().toString());

        String serializerId = stringifySerializer(recipe);
        if (!equalsDefault(serializerId, defaults.get("serializer"))) {
            entry.put("serializer", serializerId);
        }

        String group = safeString(recipe.getGroup());
        if (!equalsDefault(group, defaults.get("group"))) {
            entry.put("group", group);
        }

        List<RecipeDumpModels.DumpIngredient> ingredients = toIngredients(recipe.getIngredients());
        if (!equalsDefault(ingredients, defaults.get("ingredients"))) {
            entry.put("ingredients", ingredients);
        }

        RecipeDumpModels.DumpResultItem resultItem = toResultItem(recipe, server);
        if (!equalsDefault(resultItem, defaults.get("result_item"))) {
            entry.put("result_item", resultItem);
        }

        enrichGtRecipe(entry, defaults, recipe);

        Map<String, Object> special = buildSpecialSection(recipe);
        if (!equalsDefault(special, defaults.get("special"))) {
            entry.put("special", special);
        }

        return entry;
    }

    private void enrichGtRecipe(
            Map<String, Object> entry,
            Map<String, Object> defaults,
            Recipe<?> recipe
    ) {
        if (!"com.gregtechceu.gtceu.api.recipe.GTRecipe".equals(recipe.getClass().getName())) {
            return;
        }

        Integer duration = tryReadInt(recipe, "getDuration", "duration");
        if (duration != null && !equalsDefault(duration, defaults.get("duration"))) {
            entry.put("duration", duration);
        }

        CapabilityBuckets inputs = extractCapabilityBuckets(tryReadValue(recipe, "inputs"));
        CapabilityBuckets outputs = extractCapabilityBuckets(tryReadValue(recipe, "outputs"));
        CapabilityBuckets tickInputs = extractCapabilityBuckets(tryReadValue(recipe, "tickInputs"));
        CapabilityBuckets tickOutputs = extractCapabilityBuckets(tryReadValue(recipe, "tickOutputs"));

        putIfNotDefault(entry, defaults, "item_inputs", inputs.itemEntries);
        putIfNotDefault(entry, defaults, "fluid_inputs", inputs.fluidEntries);
        putIfNotDefault(entry, defaults, "item_outputs", outputs.itemEntries);
        putIfNotDefault(entry, defaults, "fluid_outputs", outputs.fluidEntries);

        putIfNotDefault(entry, defaults, "tick_item_inputs", tickInputs.itemEntries);
        putIfNotDefault(entry, defaults, "tick_fluid_inputs", tickInputs.fluidEntries);
        putIfNotDefault(entry, defaults, "tick_item_outputs", tickOutputs.itemEntries);
        putIfNotDefault(entry, defaults, "tick_fluid_outputs", tickOutputs.fluidEntries);

        Integer inputEUt = firstNonNull(
                extractEnergyValue(tryReadValue(recipe, "getInputEUt", "inputEUt")),
                inputs.energyValue,
                tickInputs.energyValue
        );
        Integer outputEUt = firstNonNull(
                extractEnergyValue(tryReadValue(recipe, "getOutputEUt", "outputEUt")),
                outputs.energyValue,
                tickOutputs.energyValue
        );
        Integer eut = chooseEUt(inputEUt, outputEUt);

        if (inputEUt != null && !equalsDefault(inputEUt, defaults.get("input_eut"))) {
            entry.put("input_eut", inputEUt);
        }
        if (outputEUt != null && !equalsDefault(outputEUt, defaults.get("output_eut"))) {
            entry.put("output_eut", outputEUt);
        }
        if (eut != null && !equalsDefault(eut, defaults.get("eut"))) {
            entry.put("eut", eut);
        }
    }

    private void putIfNotDefault(Map<String, Object> entry, Map<String, Object> defaults, String key, Object value) {
        if (!equalsDefault(value, defaults.get(key))) {
            entry.put(key, value);
        }
    }

    private Integer chooseEUt(Integer inputEUt, Integer outputEUt) {
        if (inputEUt != null && inputEUt != 0) {
            return inputEUt;
        }
        if (outputEUt != null && outputEUt != 0) {
            return outputEUt;
        }
        if (inputEUt != null) {
            return inputEUt;
        }
        return outputEUt;
    }

    private CapabilityBuckets extractCapabilityBuckets(Object rawValue) {
        CapabilityBuckets buckets = new CapabilityBuckets();
        if (!(rawValue instanceof Map<?, ?> map)) {
            return buckets;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            CapabilityKind kind = classifyCapability(entry.getKey());
            Object payload = entry.getValue();

            if (kind == CapabilityKind.ITEM) {
                buckets.itemEntries.addAll(normalizeGtEntryList(payload, ContentKind.ITEM));
            } else if (kind == CapabilityKind.FLUID) {
                buckets.fluidEntries.addAll(normalizeGtEntryList(payload, ContentKind.FLUID));
            } else if (kind == CapabilityKind.EU) {
                Integer energy = extractEnergyFromPayload(payload);
                if (energy != null) {
                    buckets.energyValue = energy;
                }
            }
        }

        return buckets;
    }

    private List<Map<String, Object>> normalizeGtEntryList(Object payload, ContentKind kind) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (payload == null) {
            return result;
        }

        Iterable<?> iterable = toIterable(payload);
        if (iterable == null) {
            Map<String, Object> single = normalizeSingleGtEntry(payload, kind);
            if (single != null) {
                result.add(single);
            }
            return result;
        }

        for (Object element : iterable) {
            Map<String, Object> normalized = normalizeSingleGtEntry(element, kind);
            if (normalized != null) {
                result.add(normalized);
            }
        }

        return result;
    }

    private Map<String, Object> normalizeSingleGtEntry(Object wrapper, ContentKind kind) {
        if (wrapper == null) {
            return null;
        }

        Object content = firstNonNullObject(
                tryReadValue(wrapper, "getContent", "content"),
                wrapper
        );

        Integer chance = firstNonNull(
                tryReadInt(wrapper, "getChance", "chance"),
                10000
        );

        Integer amount = extractAmount(content, kind);
        if (amount == null || amount <= 0) {
            amount = 1;
        }

        LinkedHashSet<String> options = collectOptions(content, kind);
        if (options.isEmpty()) {
            return null;
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("options", new ArrayList<>(options));
        entry.put("amount", amount);
        entry.put("chance", chance);
        return entry;
    }

    private LinkedHashSet<String> collectOptions(Object root, ContentKind kind) {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        collectOptionsRecursive(root, kind, 0, visited, options);
        return options;
    }

    private void collectOptionsRecursive(
            Object value,
            ContentKind kind,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            LinkedHashSet<String> out
    ) {
        value = unwrapAtomicReference(value);
        if (value == null || depth > 5) {
            return;
        }

        if (shouldTrackIdentity(value)) {
            if (visited.containsKey(value)) {
                return;
            }
            visited.put(value, Boolean.TRUE);
        }

        if (kind == ContentKind.ITEM) {
            if (value instanceof ItemStack stack) {
                addItemId(stack, out);
                return;
            }
            if (value instanceof Ingredient ingredient) {
                for (ItemStack stack : ingredient.getItems()) {
                    addItemId(stack, out);
                }
                return;
            }
            if (value instanceof Item item) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) {
                    out.add(id.toString());
                }
                return;
            }
        } else if (kind == ContentKind.FLUID) {
            if (value instanceof FluidStack stack) {
                addFluidId(stack, out);
                return;
            }
            if (value instanceof Fluid fluid) {
                ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
                if (id != null) {
                    out.add(id.toString());
                }
                return;
            }
        }

        if (value instanceof Map<?, ?> map) {
            for (Object nested : map.values()) {
                collectOptionsRecursive(nested, kind, depth + 1, visited, out);
            }
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object nested : iterable) {
                collectOptionsRecursive(nested, kind, depth + 1, visited, out);
            }
            return;
        }

        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                collectOptionsRecursive(Array.get(value, i), kind, depth + 1, visited, out);
            }
            return;
        }

        List<Object> nestedValues = new ArrayList<>();
        if (kind == ContentKind.ITEM) {
            addCandidateValues(nestedValues, value,
                    "getIngredient", "ingredient",
                    "getInner", "inner",
                    "getStack", "stack",
                    "getItemStack", "itemStack",
                    "getItem", "item",
                    "getItems", "items",
                    "getStacks", "stacks",
                    "getValues", "values",
                    "getContent", "content"
            );
        } else {
            addCandidateValues(nestedValues, value,
                    "getFluidStack", "fluidStack",
                    "getStack", "stack",
                    "getFluid", "fluid",
                    "getFluids", "fluids",
                    "getStacks", "stacks",
                    "getValues", "values",
                    "getContent", "content"
            );
        }

        for (Object nested : getAllDeclaredFieldValues(value)) {
            nestedValues.add(nested);
        }

        for (Object nested : nestedValues) {
            if (nested != value) {
                collectOptionsRecursive(nested, kind, depth + 1, visited, out);
            }
        }
    }

    private void addCandidateValues(List<Object> target, Object source, String... names) {
        for (String name : names) {
            Object value = tryReadValue(source, name);
            if (value != null) {
                target.add(value);
            }
        }
    }

    private List<Object> getAllDeclaredFieldValues(Object source) {
        List<Object> values = new ArrayList<>();
        Class<?> current = source.getClass();

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(source);
                    if (value != null && !isSimpleValueType(value.getClass())) {
                        values.add(value);
                    }
                } catch (Exception ignored) {
                }
            }
            current = current.getSuperclass();
        }

        return values;
    }

    private boolean isSimpleValueType(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || type == String.class
                || type == Boolean.class
                || type.isEnum()
                || type == Class.class;
    }

    private boolean shouldTrackIdentity(Object value) {
        return !(value instanceof String)
                && !(value instanceof Number)
                && !(value instanceof Boolean)
                && !value.getClass().isEnum();
    }

    private Integer extractAmount(Object content, ContentKind kind) {
        content = unwrapAtomicReference(content);
        if (content == null) {
            return null;
        }

        if (kind == ContentKind.ITEM) {
            if (content instanceof ItemStack stack && !stack.isEmpty()) {
                return stack.getCount();
            }
            if (content instanceof Ingredient) {
                return 1;
            }
        } else {
            if (content instanceof FluidStack stack && !stack.isEmpty()) {
                return stack.getAmount();
            }
        }

        Integer direct = tryReadInt(content,
                "getAmount", "amount",
                "getCount", "count"
        );
        if (direct != null && direct > 0) {
            return direct;
        }

        Object nested = firstNonNullObject(
                tryReadValue(content, "getInner", "inner"),
                tryReadValue(content, "getIngredient", "ingredient"),
                tryReadValue(content, "getStack", "stack"),
                tryReadValue(content, "getItemStack", "itemStack"),
                tryReadValue(content, "getFluidStack", "fluidStack"),
                tryReadValue(content, "getContent", "content")
        );
        if (nested != null && nested != content) {
            Integer nestedAmount = extractAmount(nested, kind);
            if (nestedAmount != null && nestedAmount > 0) {
                return nestedAmount;
            }
        }

        return null;
    }

    private Integer extractEnergyFromPayload(Object payload) {
        Iterable<?> iterable = toIterable(payload);
        if (iterable == null) {
            return extractEnergyValue(firstNonNullObject(
                    tryReadValue(payload, "getContent", "content"),
                    payload
            ));
        }

        for (Object element : iterable) {
            Object content = firstNonNullObject(
                    tryReadValue(element, "getContent", "content"),
                    element
            );
            Integer energy = extractEnergyValue(content);
            if (energy != null) {
                return energy;
            }
        }

        return null;
    }

    private Integer extractEnergyValue(Object value) {
        value = unwrapAtomicReference(value);
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        Integer explicit = tryReadInt(value,
                "getEUt", "EUt",
                "getEut", "eut",
                "getEu", "eu",
                "getAmount", "amount"
        );
        if (explicit != null) {
            return explicit;
        }

        Integer voltage = tryReadInt(value, "getVoltage", "voltage");
        Integer amperage = tryReadInt(value, "getAmperage", "amperage");
        if (voltage != null && amperage != null) {
            return voltage * amperage;
        }

        Object nested = firstNonNullObject(
                tryReadValue(value, "getContent", "content"),
                tryReadValue(value, "getValue", "value"),
                tryReadValue(value, "getStack", "stack")
        );
        if (nested != null && nested != value) {
            return extractEnergyValue(nested);
        }

        return null;
    }

    private Iterable<?> toIterable(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<Object> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                result.add(Array.get(value, i));
            }
            return result;
        }
        return null;
    }

    private void addItemId(ItemStack stack, Set<String> out) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            out.add(id.toString());
        }
    }

    private void addFluidId(FluidStack stack, Set<String> out) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        if (id != null) {
            out.add(id.toString());
        }
    }

    private CapabilityKind classifyCapability(Object capability) {
        if (capability == null) {
            return CapabilityKind.UNKNOWN;
        }

        String text = capability.getClass().getName().toLowerCase(Locale.ROOT)
                + " " + String.valueOf(capability).toLowerCase(Locale.ROOT);

        if (text.contains("fluidrecipecapability")) {
            return CapabilityKind.FLUID;
        }
        if (text.contains("itemrecipecapability")) {
            return CapabilityKind.ITEM;
        }
        if (text.contains("eurecipecapability")) {
            return CapabilityKind.EU;
        }
        return CapabilityKind.UNKNOWN;
    }

    private Integer tryReadInt(Object target, String... candidates) {
        Object value = tryReadValue(target, candidates);
        value = unwrapAtomicReference(value);

        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Object unwrapAtomicReference(Object value) {
        if (value instanceof AtomicReference<?> ref) {
            return ref.get();
        }
        return value;
    }

    private Object tryReadValue(Object target, String... candidates) {
        if (target == null) {
            return null;
        }

        for (String candidate : candidates) {
            Object byMethod = tryInvokeNoArg(target, candidate);
            if (byMethod != null) {
                return byMethod;
            }

            Object byField = tryReadField(target, candidate);
            if (byField != null) {
                return byField;
            }
        }

        return null;
    }

    private Object tryInvokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object tryReadField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private String stringifySerializer(Recipe<?> recipe) {
        ResourceLocation key = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        return key == null ? null : key.toString();
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private List<RecipeDumpModels.DumpIngredient> toIngredients(List<Ingredient> ingredients) {
        List<RecipeDumpModels.DumpIngredient> result = new ArrayList<>();

        for (Ingredient ingredient : ingredients) {
            ItemStack[] matchingStacks = ingredient.getItems();
            List<RecipeDumpModels.DumpIngredientOption> options = new ArrayList<>();

            for (ItemStack stack : matchingStacks) {
                if (stack.isEmpty()) {
                    continue;
                }

                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (itemId == null) {
                    continue;
                }

                options.add(new RecipeDumpModels.DumpIngredientOption(
                        itemId.toString(),
                        stack.getCount()
                ));
            }

            result.add(new RecipeDumpModels.DumpIngredient(options));
        }

        return result;
    }

    private RecipeDumpModels.DumpResultItem toResultItem(Recipe<?> recipe, MinecraftServer server) {
        ItemStack stack = recipe.getResultItem(server.registryAccess());
        if (stack.isEmpty()) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }

        return new RecipeDumpModels.DumpResultItem(itemId.toString(), stack.getCount());
    }

    private Map<String, Object> buildSpecialSection(Recipe<?> recipe) {
        Map<String, Object> special = new LinkedHashMap<>();

        if (recipe.isSpecial()) {
            special.put("is_special", true);
        }

        special.put("recipe_class", recipe.getClass().getName());
        return special;
    }

    private boolean equalsDefault(Object value, Object defaultValue) {
        return Objects.equals(value, defaultValue);
    }

    private Path resolveOutputPath(ResourceLocation recipeTypeId) {
        String namespace = recipeTypeId.getNamespace();
        String path = recipeTypeId.getPath().replace('/', '_');

        return FMLPaths.GAMEDIR.get()
                .resolve("kubejs")
                .resolve("exports")
                .resolve("gtcsolo")
                .resolve("recipes")
                .resolve(namespace + "__" + path + ".json");
    }

    private void writeJson(Path outputPath, RecipeDumpModels.DumpFile dumpFile) throws IOException {
        Files.createDirectories(outputPath.getParent());

        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            GSON.toJson(dumpFile, writer);
        }
    }

    public RecipeDumpModels.DumpFile readDump(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, RecipeDumpModels.DumpFile.class);
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object firstNonNullObject(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private enum CapabilityKind {
        ITEM,
        FLUID,
        EU,
        UNKNOWN
    }

    private enum ContentKind {
        ITEM,
        FLUID
    }

    private static final class CapabilityBuckets {
        private final List<Map<String, Object>> itemEntries = new ArrayList<>();
        private final List<Map<String, Object>> fluidEntries = new ArrayList<>();
        private Integer energyValue;
    }
}