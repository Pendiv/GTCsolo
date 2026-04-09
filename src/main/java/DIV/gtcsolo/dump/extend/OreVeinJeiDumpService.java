package DIV.gtcsolo.dump.extend;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class OreVeinJeiDumpService {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private static final String TARGET_CATEGORY_ID = "gtceu:ore_vein_diagram";
    private static final String TARGET_WRAPPER_CLASS = "com.gregtechceu.gtceu.integration.jei.orevein.GTOreVeinInfoWrapper";
    private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern INT_PATTERN = Pattern.compile("-?\\d+");
    private static final Pattern NAMED_INT_PATTERN = Pattern.compile("(minY|maxY|yRadius|min_y|max_y|y_radius|radius|thickness|height)\\s*[=:]\\s*(-?\\d+)");

    public OreVeinJeiDumpResult dump(MinecraftServer server) throws IOException {
        Object runtime = GtcSoloJeiPlugin.getRuntimeOrThrow();

        Object apiRecipeManager = invokeNoArg(runtime, "getRecipeManager");
        if (apiRecipeManager == null) {
            throw new IllegalStateException("Could not access JEI recipe manager from runtime.");
        }

        Object recipeManager = resolveInternalRecipeManager(runtime, apiRecipeManager);

        Gtcsolo.LOGGER.info("JEI runtime class = {}", classNameOf(runtime));
        Gtcsolo.LOGGER.info("JEI api recipe manager class = {}", classNameOf(apiRecipeManager));
        Gtcsolo.LOGGER.info("JEI resolved recipe manager class = {}", classNameOf(recipeManager));

        List<Object> recipeTypes = resolveRecipeTypes(recipeManager, runtime);
        Object oreVeinRecipeType = findOreVeinRecipeType(apiRecipeManager, recipeManager, runtime, recipeTypes);
        if (oreVeinRecipeType == null) {
            throw new IllegalStateException(
                    "Could not find JEI ore vein category/type. Check latest.log for [JEI category] / [JEI recipe type] lines."
            );
        }

        List<Object> wrappers = resolveRecipes(apiRecipeManager, recipeManager, oreVeinRecipeType);
        if (wrappers.isEmpty()) {
            throw new IllegalStateException("JEI category exists but returned no wrappers: " + TARGET_CATEGORY_ID);
        }

        List<Map<String, Object>> dumped = new ArrayList<>();
        for (Object wrapper : wrappers) {
            dumped.add(dumpWrapper(wrapper));
        }

        dumped.sort(Comparator.comparing(this::sortKeyForVein));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "gtcsolo:ore_vein_definition_member_probe");
        root.put("schema", 4);
        root.put("category_id", TARGET_CATEGORY_ID);
        root.put("count", dumped.size());
        root.put("veins", dumped);

        Path outputPath = FMLPaths.GAMEDIR.get()
                .resolve("kubejs")
                .resolve("exports")
                .resolve("gtcsolo")
                .resolve("extended")
                .resolve("ore_veins_from_jei.json");

        Files.createDirectories(outputPath.getParent());
        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }

        Gtcsolo.LOGGER.info("Dumped {} JEI ore vein wrappers to {}", dumped.size(), outputPath.toAbsolutePath());
        return new OreVeinJeiDumpResult(outputPath, dumped.size());
    }

    private String sortKeyForVein(Map<String, Object> vein) {
        Object definitionClass = vein.get("definition_class");
        if (definitionClass != null) {
            return String.valueOf(definitionClass);
        }

        Object blocksObj = vein.get("blocks");
        if (blocksObj instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            return String.valueOf(list.get(0));
        }

        Object generatorObj = vein.get("generator");
        if (generatorObj instanceof Map<?, ?> map) {
            Object kind = map.get("kind");
            if (kind != null) {
                return String.valueOf(kind);
            }
        }

        return "zzz";
    }

    private List<Object> resolveRecipeTypes(Object recipeManager, Object runtime) {
        Object direct = firstNonNull(
                invokeNoArg(recipeManager, "getRecipeTypes"),
                tryReadField(recipeManager, "recipeTypes"),
                tryReadField(recipeManager, "recipeTypeList"),
                invokeNoArg(runtime, "getRecipeTypes")
        );

        List<Object> types = toObjectList(direct);
        if (!types.isEmpty()) {
            return types;
        }

        Object categories = firstNonNull(
                invokeNoArg(recipeManager, "getRecipeCategories"),
                tryReadField(recipeManager, "recipeCategories"),
                tryReadField(recipeManager, "categories"),
                invokeNoArg(runtime, "getRecipeCategories")
        );

        List<Object> categoryObjects = toObjectList(categories);
        List<Object> result = new ArrayList<>();
        for (Object category : categoryObjects) {
            Object recipeType = firstNonNull(
                    invokeNoArg(category, "getRecipeType"),
                    tryReadField(category, "recipeType")
            );
            if (recipeType != null) {
                result.add(recipeType);
            }
        }
        return result;
    }

    private Object findOreVeinRecipeType(Object apiRecipeManager, Object recipeManager, Object runtime, List<Object> recipeTypes) {
        Object exact = findRecipeType(recipeTypes, TARGET_CATEGORY_ID);
        if (exact != null) {
            Gtcsolo.LOGGER.info("Matched ore vein JEI recipe type by exact uid: {}", describeRecipeType(exact));
            return exact;
        }

        List<Object> categories = resolveRecipeCategories(recipeManager, runtime);
        for (Object category : categories) {
            Object recipeType = firstNonNull(
                    invokeNoArg(category, "getRecipeType"),
                    tryReadField(category, "recipeType")
            );

            String categoryDesc = describeJeiCategory(category);
            String typeDesc = describeRecipeType(recipeType);

            if (looksLikeOreVeinText(categoryDesc) || looksLikeOreVeinText(typeDesc)) {
                Gtcsolo.LOGGER.info("Matched ore vein JEI category by fuzzy scan: {}", categoryDesc);
                return recipeType;
            }
        }

        for (Object recipeType : recipeTypes) {
            String typeDesc = describeRecipeType(recipeType);
            if (looksLikeOreVeinText(typeDesc)) {
                Gtcsolo.LOGGER.info("Matched ore vein JEI recipe type by fuzzy type scan: {}", typeDesc);
                return recipeType;
            }
        }

        for (Object recipeType : recipeTypes) {
            List<Object> wrappers = resolveRecipesLenient(apiRecipeManager, recipeManager, recipeType);
            if (wrappers.isEmpty()) {
                continue;
            }

            Object first = wrappers.get(0);
            if (first != null && TARGET_WRAPPER_CLASS.equals(first.getClass().getName())) {
                Gtcsolo.LOGGER.info("Matched ore vein JEI recipe type by wrapper probe: {}", describeRecipeType(recipeType));
                return recipeType;
            }
        }

        for (Object category : categories) {
            Gtcsolo.LOGGER.info("[JEI category] {}", describeJeiCategory(category));
        }
        for (Object recipeType : recipeTypes) {
            Gtcsolo.LOGGER.info("[JEI recipe type] {}", describeRecipeType(recipeType));
        }

        return null;
    }

    private Object findRecipeType(List<Object> recipeTypes, String targetId) {
        for (Object recipeType : recipeTypes) {
            String uid = stringifyRecipeType(recipeType);
            if (targetId.equals(uid)) {
                return recipeType;
            }
        }
        return null;
    }

    private List<Object> resolveRecipeCategories(Object recipeManager, Object runtime) {
        Object categories = firstNonNull(
                invokeNoArg(recipeManager, "getRecipeCategories"),
                tryReadField(recipeManager, "recipeCategories"),
                tryReadField(recipeManager, "categories"),
                invokeNoArg(runtime, "getRecipeCategories")
        );
        return toObjectList(categories);
    }

    private List<Object> resolveRecipes(Object apiRecipeManager, Object internalRecipeManager, Object recipeType) {
        Object lookup = invokeCompatible(apiRecipeManager, "createRecipeLookup", recipeType);
        if (lookup != null) {
            Object recipes = firstNonNull(
                    invokeNoArg(lookup, "get"),
                    invokeNoArg(lookup, "toList"),
                    invokeNoArg(lookup, "list")
            );

            List<Object> list = toObjectList(recipes);
            if (!list.isEmpty()) {
                return list;
            }
        }

        Object emptyFocusGroup = resolveEmptyFocusGroup();
        if (emptyFocusGroup != null) {
            Object recipes = firstNonNull(
                    invokeCompatible(internalRecipeManager, "getRecipesStream", recipeType, emptyFocusGroup, false),
                    invokeCompatible(internalRecipeManager, "getRecipes", recipeType, emptyFocusGroup, false)
            );

            List<Object> list = toObjectList(recipes);
            if (!list.isEmpty()) {
                return list;
            }
        }

        Object fallback = firstNonNull(
                invokeCompatible(apiRecipeManager, "getRecipes", recipeType),
                invokeCompatible(internalRecipeManager, "getRecipes", recipeType),
                invokeCompatible(apiRecipeManager, "getRecipesForType", recipeType),
                invokeCompatible(internalRecipeManager, "getRecipesForType", recipeType)
        );

        List<Object> list = toObjectList(fallback);
        if (!list.isEmpty()) {
            return list;
        }

        throw new IllegalStateException("Could not resolve recipes from JEI recipe manager for category " + TARGET_CATEGORY_ID);
    }

    private List<Object> resolveRecipesLenient(Object apiRecipeManager, Object internalRecipeManager, Object recipeType) {
        try {
            return resolveRecipes(apiRecipeManager, internalRecipeManager, recipeType);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private Object resolveEmptyFocusGroup() {
        try {
            Class<?> focusGroupClass = Class.forName("mezz.jei.library.focus.FocusGroup");
            Field emptyField = focusGroupClass.getDeclaredField("EMPTY");
            emptyField.setAccessible(true);
            return emptyField.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Map<String, Object> dumpWrapper(Object wrapper) {
        Map<String, Object> out = new LinkedHashMap<>();

        Object definition = firstNonNull(
                invokeNoArg(wrapper, "getDefinition"),
                invokeNoArg(wrapper, "getVein"),
                tryReadField(wrapper, "definition"),
                tryReadField(wrapper, "vein"),
                tryReadField(wrapper, "oreDefinition")
        );

        Object generator = firstNonNull(
                invokeNoArg(definition, "getGenerator"),
                tryReadField(definition, "generator"),
                tryReadField(definition, "veinGenerator"),
                invokeNoArg(wrapper, "getGenerator"),
                tryReadField(wrapper, "generator")
        );
        String registryId = resolveOreVeinRegistryId(definition);
        putIfNotNull(out, "id", registryId);
        putIfNotNull(out, "id_source", registryId == null ? null : "GTRegistries.ORE_VEINS.reverse_lookup");
        Object holderLike = firstNonNull(
                tryReadField(definition, "holder"),
                tryReadField(definition, "key"),
                tryReadField(definition, "resourceKey"),
                tryReadField(definition, "registryKey"),
                invokeNoArg(definition, "getHolder"),
                invokeNoArg(definition, "getKey"),
                invokeNoArg(definition, "getResourceKey"),
                invokeNoArg(definition, "getRegistryKey")
        );

        out.put("wrapper_class", classNameOf(wrapper));
        out.put("definition_class", classNameOf(definition));
        putIfNotNull(out, "holder_class", holderLike == null ? null : classNameOf(holderLike));
        putIfNotNull(out, "generator_class", generator == null ? null : classNameOf(generator));

        Map<String, Object> memberProbe = buildDefinitionMemberProbe(definition, holderLike, generator);
        out.put("member_probe", memberProbe);

        Integer weight = firstNonNull(
                tryReadInt(wrapper, "getWeight", "weight"),
                tryReadInt(definition, "getWeight", "weight", "getVeinWeight", "veinWeight")
        );
        putIfNotNull(out, "weight", weight);

        Map<String, Object> clusterSize = extractClusterSize(definition, wrapper);
        if (!clusterSize.isEmpty()) {
            out.put("cluster_size", clusterSize);
        }

        Float density = firstNonNull(
                tryReadFloat(wrapper, "getDensity", "density"),
                tryReadFloat(definition, "getDensity", "density")
        );
        putIfNotNull(out, "density", density);

        String layer = extractLayer(definition);
        putIfNotNull(out, "layer", layer);

        Map<String, Object> height = extractHeight(definition, generator);
        if (!height.isEmpty()) {
            out.put("height", height);
        }

        List<String> dimensions = extractDimensionHints(definition);
        if (!dimensions.isEmpty()) {
            out.put("dimensions", dimensions);
        }

        List<String> biomes = extractBiomeHints(definition);
        if (!biomes.isEmpty()) {
            out.put("biomes", biomes);
        }

        if (generator != null) {
            Map<String, Object> generatorDump = dumpGenerator(generator);
            if (!generatorDump.isEmpty()) {
                out.put("generator", generatorDump);
            }
        }

        List<String> blocks = extractBlocksForDump(definition, generator);
        if (!blocks.isEmpty()) {
            out.put("blocks", blocks);
        }

        return out;
    }

    private Map<String, Object> buildDefinitionMemberProbe(Object definition, Object holderLike, Object generator) {
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("definition_declared_fields", listDeclaredFieldNames(definition));
        out.put("definition_zero_arg_methods", listZeroArgMethodNames(definition));
        out.put("definition_interesting_members", collectInterestingMembers(definition, "definition", true));

        if (holderLike != null) {
            out.put("holder_declared_fields", listDeclaredFieldNames(holderLike));
            out.put("holder_zero_arg_methods", listZeroArgMethodNames(holderLike));
            out.put("holder_interesting_members", collectInterestingMembers(holderLike, "holder", true));
        }

        if (generator != null) {
            out.put("generator_declared_fields", listDeclaredFieldNames(generator));
            out.put("generator_zero_arg_methods", listZeroArgMethodNames(generator));
            out.put("generator_interesting_members", collectInterestingMembers(generator, "generator", false));
        }

        return out;
    }

    private List<String> listDeclaredFieldNames(Object target) {
        List<String> out = new ArrayList<>();
        if (target == null) {
            return out;
        }

        for (Field field : getAllFields(target.getClass())) {
            out.add(field.getName());
        }

        out.sort(String::compareTo);
        return out;
    }

    private List<String> listZeroArgMethodNames(Object target) {
        List<String> out = new ArrayList<>();
        if (target == null) {
            return out;
        }

        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            out.add(method.getName());
        }

        out = new ArrayList<>(new LinkedHashSet<>(out));
        out.sort(String::compareTo);
        return out;
    }

    private List<Map<String, Object>> collectInterestingMembers(Object target, String prefix, boolean strict) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (target == null) {
            return out;
        }

        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (Field field : getAllFields(target.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String name = field.getName();
            if (!isInterestingMemberName(name, strict)) {
                continue;
            }

            Object value = tryReadDeclaredField(field, target);
            Map<String, Object> row = makeMemberRow(prefix + ".field." + name, value);
            String dedupeKey = row.get("member") + " -> " + row.get("summary");
            if (seen.add(dedupeKey)) {
                out.add(row);
            }
        }

        for (Method method : target.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            String name = method.getName();
            if (!isInterestingMemberName(name, strict)) {
                continue;
            }

            Object value = invokeMethodSafe(target, method);
            Map<String, Object> row = makeMemberRow(prefix + ".method." + name + "()", value);
            String dedupeKey = row.get("member") + " -> " + row.get("summary");
            if (seen.add(dedupeKey)) {
                out.add(row);
            }
        }

        return out;
    }

    private boolean isInterestingMemberName(String name, boolean strict) {
        if (name == null) {
            return false;
        }

        String lower = name.toLowerCase(Locale.ROOT);

        boolean base = lower.contains("id")
                || lower.contains("key")
                || lower.contains("registry")
                || lower.contains("holder")
                || lower.contains("location")
                || lower.contains("name")
                || lower.contains("assign")
                || lower.contains("resource")
                || lower.contains("vein");

        if (!base) {
            return false;
        }

        if (!strict) {
            return true;
        }

        return !(lower.contains("block")
                || lower.contains("material")
                || lower.contains("chance")
                || lower.contains("weight")
                || lower.contains("density")
                || lower.contains("entry")
                || lower.contains("layer"));
    }

    private Map<String, Object> makeMemberRow(String memberName, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("member", memberName);
        row.put("value_class", value == null ? "null" : value.getClass().getName());
        row.put("summary", summarizeValue(value));
        row.put("resource_locations", extractResourceLocations(value));
        return row;
    }

    private String summarizeValue(Object value) {
        value = unwrap(value);
        if (value == null) {
            return "null";
        }

        if (value instanceof ResourceLocation rl) {
            return rl.toString();
        }

        if (value instanceof CharSequence cs) {
            String s = cs.toString();
            return shorten(s, 220);
        }

        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return String.valueOf(value);
        }

        if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }

        Object nested = firstNonNull(
                invokeNoArg(value, "location"),
                invokeNoArg(value, "getId"),
                tryReadField(value, "id"),
                invokeNoArg(value, "getUid"),
                tryReadField(value, "uid"),
                invokeNoArg(value, "getRegistryName"),
                tryReadField(value, "registryName"),
                invokeNoArg(value, "getKey"),
                tryReadField(value, "key"),
                invokeNoArg(value, "getResourceKey"),
                tryReadField(value, "resourceKey"),
                invokeNoArg(value, "getRegistryKey"),
                tryReadField(value, "registryKey"),
                invokeNoArg(value, "unwrapKey")
        );

        if (nested != null && nested != value) {
            String nestedSummary = summarizeValue(nested);
            if (nestedSummary != null && !nestedSummary.equals("null")) {
                return nestedSummary;
            }
        }

        String text = safeString(value);
        return shorten(text, 220);
    }

    private List<String> extractResourceLocations(Object value) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        collectResourceLocations(value, 0, new IdentityHashMap<>(), out);
        return new ArrayList<>(out);
    }

    private void collectResourceLocations(Object value, int depth, IdentityHashMap<Object, Boolean> visited, LinkedHashSet<String> out) {
        value = unwrap(value);
        if (value == null || depth > 2) {
            return;
        }

        if (value instanceof ResourceLocation rl) {
            out.add(rl.toString());
            return;
        }

        if (shouldTrackIdentity(value)) {
            if (visited.containsKey(value)) {
                return;
            }
            visited.put(value, Boolean.TRUE);
        }

        if (value instanceof CharSequence cs) {
            out.addAll(extractResourceLocationsFromText(cs.toString()));
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object nested : iterable) {
                collectResourceLocations(nested, depth + 1, visited, out);
            }
            return;
        }

        if (value instanceof Map<?, ?> map) {
            for (Object nested : map.values()) {
                collectResourceLocations(nested, depth + 1, visited, out);
            }
            return;
        }

        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                collectResourceLocations(Array.get(value, i), depth + 1, visited, out);
            }
            return;
        }

        if (shouldSkipDeepReflection(value)) {
            return;
        }

        Object nested = firstNonNull(
                invokeNoArg(value, "location"),
                invokeNoArg(value, "getId"),
                tryReadField(value, "id"),
                invokeNoArg(value, "getUid"),
                tryReadField(value, "uid"),
                invokeNoArg(value, "getRegistryName"),
                tryReadField(value, "registryName"),
                invokeNoArg(value, "getKey"),
                tryReadField(value, "key"),
                invokeNoArg(value, "getResourceKey"),
                tryReadField(value, "resourceKey"),
                invokeNoArg(value, "getRegistryKey"),
                tryReadField(value, "registryKey"),
                invokeNoArg(value, "unwrapKey")
        );
        if (nested != null && nested != value) {
            collectResourceLocations(nested, depth + 1, visited, out);
        }

        out.addAll(extractResourceLocationsFromText(safeString(value)));
    }

    private String shorten(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    private List<String> extractResourceLocationsFromText(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }

        Matcher matcher = RESOURCE_LOCATION_PATTERN.matcher(text);
        while (matcher.find()) {
            out.add(matcher.group());
        }
        return out;
    }

    private String extractLayer(Object definition) {
        Object raw = firstNonNull(
                tryReadValue(definition, "getLayer", "layer"),
                tryReadField(definition, "layer")
        );
        return normalizeSimpleName(raw);
    }

    private String normalizeSimpleName(Object value) {
        value = unwrap(value);
        if (value == null) {
            return null;
        }

        if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }

        if (value instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        String id = stringifyIdLike(value);
        if (id != null) {
            return id;
        }

        String text = safeString(value);
        if (text == null || text.isBlank() || "null".equals(text)) {
            return null;
        }
        return text;
    }

    private Map<String, Object> extractClusterSize(Object definition, Object wrapper) {
        Map<String, Object> out = new LinkedHashMap<>();

        Object raw = firstNonNull(
                tryReadValue(wrapper, "getClusterSize", "clusterSize", "getSize", "size"),
                tryReadValue(definition, "getClusterSize", "clusterSize", "getSize", "size")
        );
        raw = unwrap(raw);

        if (raw == null) {
            return out;
        }

        if (raw instanceof Number n) {
            out.put("exact", n.intValue());
            return out;
        }

        String text = safeString(raw);
        if (text == null || text.isBlank() || "null".equals(text)) {
            return out;
        }

        String cleaned = text.trim().replace(" ", "");
        if ((cleaned.startsWith("[") && cleaned.endsWith("]"))
                || (cleaned.startsWith("(") && cleaned.endsWith(")"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        int dash = cleaned.indexOf('-');
        int comma = cleaned.indexOf(',');

        if (dash > 0) {
            Integer min = tryParseIntLoose(cleaned.substring(0, dash));
            Integer max = tryParseIntLoose(cleaned.substring(dash + 1));
            putIfNotNull(out, "min", min);
            putIfNotNull(out, "max", max);
            if (!out.isEmpty()) {
                return out;
            }
        }

        if (comma > 0) {
            Integer min = tryParseIntLoose(cleaned.substring(0, comma));
            Integer max = tryParseIntLoose(cleaned.substring(comma + 1));
            putIfNotNull(out, "min", min);
            putIfNotNull(out, "max", max);
            if (!out.isEmpty()) {
                return out;
            }
        }

        Integer exact = tryParseIntLoose(cleaned);
        if (exact != null) {
            out.put("exact", exact);
        } else {
            out.put("text", text);
        }

        return out;
    }

    private Integer tryParseIntLoose(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = INT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, Object> extractHeight(Object definition, Object generator) {
        Map<String, Object> out = new LinkedHashMap<>();

        Integer minY = firstNonNull(
                tryReadInt(definition, "getMinY", "minY", "getMinimumY", "minimumY", "getMinHeight", "minHeight"),
                tryReadInt(generator, "getMinY", "minY", "getMinimumY", "minimumY", "getMinHeight", "minHeight")
        );
        Integer maxY = firstNonNull(
                tryReadInt(definition, "getMaxY", "maxY", "getMaximumY", "maximumY", "getMaxHeight", "maxHeight"),
                tryReadInt(generator, "getMaxY", "maxY", "getMaximumY", "maximumY", "getMaxHeight", "maxHeight")
        );
        Integer yRadius = firstNonNull(
                tryReadInt(definition, "getYRadius", "yRadius"),
                tryReadInt(generator, "getYRadius", "yRadius")
        );

        Object heightRange = firstNonNull(
                invokeNoArg(definition, "getHeightRange"),
                tryReadField(definition, "heightRange"),
                invokeNoArg(definition, "getHeight"),
                tryReadField(definition, "height"),
                invokeNoArg(generator, "getHeightRange"),
                tryReadField(generator, "heightRange"),
                invokeNoArg(generator, "getHeight"),
                tryReadField(generator, "height")
        );

        if (heightRange != null) {
            if (minY == null) {
                minY = firstNonNull(
                        tryReadInt(heightRange, "getMinY", "minY", "getMinInclusive", "minInclusive", "getMin", "min")
                );
            }
            if (maxY == null) {
                maxY = firstNonNull(
                        tryReadInt(heightRange, "getMaxY", "maxY", "getMaxInclusive", "maxInclusive", "getMax", "max")
                );
            }
            if (yRadius == null) {
                yRadius = tryReadInt(heightRange, "getYRadius", "yRadius");
            }
        }

        Map<String, Integer> parsed = parseNamedIntsFromText(
                safeString(generator),
                safeString(tryReadField(definition, "veinGenerator")),
                safeString(definition)
        );

        if (minY == null) minY = parsed.get("minY");
        if (maxY == null) maxY = parsed.get("maxY");
        if (yRadius == null) yRadius = parsed.get("yRadius");

        putIfNotNull(out, "min_y", minY);
        putIfNotNull(out, "max_y", maxY);
        putIfNotNull(out, "y_radius", yRadius);
        return out;
    }

    private Map<String, Integer> parseNamedIntsFromText(String... texts) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (texts == null) {
            return out;
        }

        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }

            Matcher matcher = NAMED_INT_PATTERN.matcher(text);
            while (matcher.find()) {
                String key = matcher.group(1);
                Integer value = tryParseIntLoose(matcher.group(2));
                if (value == null) {
                    continue;
                }

                switch (key) {
                    case "minY", "min_y" -> out.putIfAbsent("minY", value);
                    case "maxY", "max_y" -> out.putIfAbsent("maxY", value);
                    case "yRadius", "y_radius" -> out.putIfAbsent("yRadius", value);
                    default -> {
                    }
                }
            }
        }

        return out;
    }

    private List<String> extractDimensionHints(Object definition) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (definition == null) {
            return new ArrayList<>(out);
        }

        Object dimensionRoot = firstNonNull(
                invokeNoArg(definition, "getDimensionFilter"),
                tryReadField(definition, "dimensionFilter"),
                tryReadField(definition, "dimensions"),
                tryReadField(definition, "dimension"),
                definition
        );

        addAllNonBlank(out, normalizeDimensionTokens(extractNamedStrings(dimensionRoot, "dimension", "marker", "overworld", "nether", "end")));
        return new ArrayList<>(out);
    }

    private List<String> normalizeDimensionTokens(List<String> rawTokens) {
        LinkedHashSet<String> out = new LinkedHashSet<>();

        for (String raw : rawTokens) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String lower = raw.toLowerCase(Locale.ROOT);

            if (lower.contains("overworld")) {
                out.add("minecraft:overworld");
            }
            if (lower.contains("the_nether") || lower.endsWith(":nether") || lower.contains(" nether")) {
                out.add("minecraft:the_nether");
            }
            if (lower.contains("the_end") || lower.endsWith(":end") || lower.contains(" end")) {
                out.add("minecraft:the_end");
            }

            for (String candidate : extractResourceLocationsFromText(raw)) {
                String idLower = candidate.toLowerCase(Locale.ROOT);
                if (idLower.equals("minecraft:dimension")
                        || idLower.equals("minecraft:dimension_type")) {
                    continue;
                }
                if (idLower.equals("minecraft:overworld")
                        || idLower.equals("minecraft:the_nether")
                        || idLower.equals("minecraft:the_end")
                        || !idLower.startsWith("minecraft:worldgen/")) {
                    out.add(candidate);
                }
            }
        }

        return new ArrayList<>(out);
    }

    private List<String> extractBiomeHints(Object definition) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (definition == null) {
            return new ArrayList<>(out);
        }

        Object biomeRoot = firstNonNull(
                invokeNoArg(definition, "getBiomeFilter"),
                tryReadField(definition, "biomeFilter"),
                tryReadField(definition, "biomes"),
                definition
        );

        addAllNonBlank(out, normalizeBiomeTokens(extractNamedStrings(biomeRoot, "biome", "taiga", "forest", "desert", "plains", "ocean")));
        return new ArrayList<>(out);
    }

    private List<String> normalizeBiomeTokens(List<String> rawTokens) {
        LinkedHashSet<String> out = new LinkedHashSet<>();

        for (String raw : rawTokens) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            for (String candidate : extractResourceLocationsFromText(raw)) {
                String lower = candidate.toLowerCase(Locale.ROOT);
                if (lower.equals("minecraft:worldgen/biome")
                        || lower.equals("minecraft:biome")
                        || lower.endsWith("/biome")) {
                    continue;
                }

                out.add(candidate);
            }
        }

        return new ArrayList<>(out);
    }

    private List<String> extractBlocksForDump(Object definition, Object generator) {
        LinkedHashSet<String> out = new LinkedHashSet<>();

        if (generator != null) {
            addAllNonBlank(out, extractBlocksFromGeneratorEntries(generator));
            addAllNonBlank(out, normalizeBlockTokens(extractBlockLikeStrings(generator)));
        }

        if (definition != null) {
            addAllNonBlank(out, normalizeBlockTokens(extractBlockLikeStrings(tryReadField(definition, "layer"))));
            addAllNonBlank(out, normalizeBlockTokens(extractBlockLikeStrings(tryReadField(definition, "indicatorGenerators"))));
            addAllNonBlank(out, normalizeBlockTokens(extractBlockLikeStrings(tryReadField(definition, "veinGenerator"))));
        }

        out.remove("minecraft:air");
        return new ArrayList<>(out);
    }

    private List<String> extractBlocksFromGeneratorEntries(Object generator) {
        LinkedHashSet<String> out = new LinkedHashSet<>();

        Object entriesObj = firstNonNull(
                invokeNoArg(generator, "getAllEntries"),
                invokeNoArg(generator, "getEntries"),
                tryReadField(generator, "entries"),
                tryReadField(generator, "defaultEntries")
        );

        for (Object entry : toObjectList(entriesObj)) {
            addAllNonBlank(out, normalizeBlockTokens(extractBlockLikeStrings(entry)));
        }

        return new ArrayList<>(out);
    }

    private List<String> normalizeBlockTokens(List<String> rawTokens) {
        LinkedHashSet<String> out = new LinkedHashSet<>();

        for (String raw : rawTokens) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            for (String candidate : extractResourceLocationsFromText(raw)) {
                String lower = candidate.toLowerCase(Locale.ROOT);
                if (!isProbablyBlockId(lower)) {
                    continue;
                }

                out.add(candidate);
            }
        }

        return new ArrayList<>(out);
    }

    private boolean isProbablyBlockId(String lower) {
        if (lower == null || !lower.contains(":")) {
            return false;
        }

        if (lower.equals("minecraft:air")) {
            return true;
        }

        if (lower.contains("dimension")
                || lower.contains("biome")
                || lower.contains("worldgen")
                || lower.contains("vein")
                || lower.contains("weight")
                || lower.contains("density")
                || lower.contains("container.inventory")) {
            return false;
        }

        return true;
    }

    private void addAllNonBlank(LinkedHashSet<String> out, List<String> values) {
        if (values == null) {
            return;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value);
            }
        }
    }

    private Map<String, Object> dumpGenerator(Object generator) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("class", classNameOf(generator));
        out.put("kind", classifyGenerator(generator));

        Integer radius = firstNonNull(
                tryReadInt(generator, "getRadius", "radius"),
                tryReadInt(generator, "getMinRadius", "minRadius")
        );
        Integer thickness = tryReadInt(generator, "getThickness", "thickness");
        Integer minY = tryReadInt(generator, "getMinY", "minY");
        Integer maxY = tryReadInt(generator, "getMaxY", "maxY");
        Integer yRadius = tryReadInt(generator, "getYRadius", "yRadius");

        putIfNotNull(out, "radius", radius);
        putIfNotNull(out, "thickness", thickness);
        putIfNotNull(out, "min_y", minY);
        putIfNotNull(out, "max_y", maxY);
        putIfNotNull(out, "y_radius", yRadius);

        Object entriesObj = firstNonNull(
                invokeNoArg(generator, "getAllEntries"),
                invokeNoArg(generator, "getEntries"),
                tryReadField(generator, "entries"),
                tryReadField(generator, "defaultEntries")
        );

        List<Object> entries = toObjectList(entriesObj);
        List<Map<String, Object>> dumpedEntries = new ArrayList<>();
        for (Object entry : entries) {
            Map<String, Object> dumped = new LinkedHashMap<>();

            Integer weight = tryReadInt(entry, "getWeight", "weight");
            Integer amount = firstNonNull(
                    tryReadInt(entry, "getAmount", "amount"),
                    tryReadInt(entry, "getCount", "count")
            );
            List<String> blocks = normalizeBlockTokens(extractBlockLikeStrings(entry));

            putIfNotNull(dumped, "weight", weight);
            putIfNotNull(dumped, "amount", amount);
            if (!blocks.isEmpty()) {
                dumped.put("blocks", blocks);
            }

            if (!dumped.isEmpty()) {
                dumpedEntries.add(dumped);
            }
        }

        if (!dumpedEntries.isEmpty()) {
            out.put("entries", dumpedEntries);
        }

        List<String> blocks = extractBlocksFromGeneratorEntries(generator);
        if (!blocks.isEmpty()) {
            out.put("blocks", blocks);
        }

        return out;
    }

    private List<String> extractNamedStrings(Object root, String... keywords) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectNamedStrings(root, 0, new IdentityHashMap<>(), result, keywords);
        return new ArrayList<>(result);
    }

    private void collectNamedStrings(
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            LinkedHashSet<String> out,
            String... keywords
    ) {
        value = unwrap(value);
        if (value == null || depth > 3) {
            return;
        }

        if (shouldTrackIdentity(value)) {
            if (visited.containsKey(value)) {
                return;
            }
            visited.put(value, Boolean.TRUE);
        }

        String idLike = stringifyIdLike(value);
        if (idLike != null && containsAnyKeyword(idLike, keywords)) {
            out.add(idLike);
        }

        if (value instanceof CharSequence sequence) {
            String text = sequence.toString();
            if (containsAnyKeyword(text, keywords)) {
                out.add(text);
            }
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object nested : iterable) {
                collectNamedStrings(nested, depth + 1, visited, out, keywords);
            }
            return;
        }

        if (value instanceof Map<?, ?> map) {
            for (Object nested : map.values()) {
                collectNamedStrings(nested, depth + 1, visited, out, keywords);
            }
            return;
        }

        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                collectNamedStrings(Array.get(value, i), depth + 1, visited, out, keywords);
            }
            return;
        }

        if (shouldSkipDeepReflection(value)) {
            return;
        }

        for (Field field : getAllFields(value.getClass())) {
            Object nested = tryReadDeclaredField(field, value);
            if (nested == null) {
                continue;
            }

            String fieldName = field.getName().toLowerCase(Locale.ROOT);

            if (containsAnyKeyword(fieldName, keywords)) {
                String nestedId = stringifyIdLike(nested);
                if (nestedId != null) {
                    out.add(nestedId);
                } else if (nested instanceof CharSequence sequence) {
                    out.add(sequence.toString());
                } else if (isSimpleValue(nested)) {
                    out.add(String.valueOf(nested));
                }
            }

            collectNamedStrings(nested, depth + 1, visited, out, keywords);
        }
    }

    private List<String> extractBlockLikeStrings(Object root) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectBlockLikeStrings(root, 0, new IdentityHashMap<>(), result);
        return new ArrayList<>(result);
    }

    private void collectBlockLikeStrings(
            Object value,
            int depth,
            IdentityHashMap<Object, Boolean> visited,
            LinkedHashSet<String> out
    ) {
        value = unwrap(value);
        if (value == null || depth > 3) {
            return;
        }

        if (shouldTrackIdentity(value)) {
            if (visited.containsKey(value)) {
                return;
            }
            visited.put(value, Boolean.TRUE);
        }

        String idLike = stringifyIdLike(value);
        if (idLike != null && (idLike.contains("_ore") || idLike.contains("block") || idLike.contains(":"))) {
            out.add(idLike);
        }

        if (value instanceof CharSequence sequence) {
            out.add(sequence.toString());
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object nested : iterable) {
                collectBlockLikeStrings(nested, depth + 1, visited, out);
            }
            return;
        }

        if (value instanceof Map<?, ?> map) {
            for (Object nested : map.values()) {
                collectBlockLikeStrings(nested, depth + 1, visited, out);
            }
            return;
        }

        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                collectBlockLikeStrings(Array.get(value, i), depth + 1, visited, out);
            }
            return;
        }

        if (shouldSkipDeepReflection(value)) {
            return;
        }

        for (Field field : getAllFields(value.getClass())) {
            String fieldName = field.getName().toLowerCase(Locale.ROOT);
            if (!(fieldName.contains("block")
                    || fieldName.contains("target")
                    || fieldName.contains("entry")
                    || fieldName.contains("layer")
                    || fieldName.contains("state")
                    || fieldName.contains("ore"))) {
                continue;
            }

            Object nested = tryReadDeclaredField(field, value);
            if (nested == null) {
                continue;
            }

            collectBlockLikeStrings(nested, depth + 1, visited, out);
        }
    }

    private boolean shouldSkipDeepReflection(Object value) {
        if (value == null) {
            return true;
        }

        Class<?> clazz = value.getClass();
        String name = clazz.getName();

        return clazz.isEnum()
                || name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.");
    }

    private Object tryReadDeclaredField(Field field, Object target) {
        try {
            if (!field.canAccess(target)) {
                field.setAccessible(true);
            }
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String describeJeiCategory(Object category) {
        if (category == null) {
            return "null";
        }

        Object recipeType = firstNonNull(
                invokeNoArg(category, "getRecipeType"),
                tryReadField(category, "recipeType")
        );

        Object title = firstNonNull(
                invokeNoArg(category, "getTitle"),
                tryReadField(category, "title")
        );

        String uid = firstNonNull(
                stringifyIdLike(invokeNoArg(category, "getUid")),
                stringifyIdLike(tryReadField(category, "uid")),
                stringifyIdLike(recipeType)
        );

        return classNameOf(category)
                + " | uid=" + uid
                + " | title=" + safeString(title)
                + " | recipeType=" + safeString(recipeType);
    }

    private String describeRecipeType(Object recipeType) {
        if (recipeType == null) {
            return "null";
        }

        Object recipeClass = firstNonNull(
                invokeNoArg(recipeType, "getRecipeClass"),
                tryReadField(recipeType, "recipeClass")
        );

        return classNameOf(recipeType)
                + " | uid=" + stringifyRecipeType(recipeType)
                + " | recipeClass=" + safeString(recipeClass)
                + " | text=" + safeString(recipeType);
    }

    private boolean looksLikeOreVeinText(String text) {
        if (text == null) {
            return false;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("ore_vein")
                || lower.contains("orevein")
                || lower.contains(".orevein.")
                || (lower.contains("ore") && lower.contains("vein"));
    }

    private String stringifyRecipeType(Object recipeType) {
        Object uid = firstNonNull(
                invokeNoArg(recipeType, "getUid"),
                invokeNoArg(recipeType, "getRecipeTypeUid"),
                tryReadField(recipeType, "uid"),
                tryReadField(recipeType, "recipeTypeUid"),
                recipeType
        );
        return stringifyIdLike(uid);
    }

    private String stringifyIdLike(Object value) {
        value = unwrap(value);
        if (value == null) {
            return null;
        }

        if (value instanceof ResourceLocation rl) {
            return rl.toString();
        }

        Object nested = firstNonNull(
                invokeNoArg(value, "location"),
                invokeNoArg(value, "getId"),
                tryReadField(value, "id"),
                invokeNoArg(value, "getUid"),
                tryReadField(value, "uid"),
                invokeNoArg(value, "getRegistryName"),
                tryReadField(value, "registryName"),
                invokeNoArg(value, "getKey"),
                tryReadField(value, "key"),
                invokeNoArg(value, "getResourceKey"),
                tryReadField(value, "resourceKey"),
                invokeNoArg(value, "getRegistryKey"),
                tryReadField(value, "registryKey"),
                invokeNoArg(value, "unwrapKey")
        );

        if (nested instanceof ResourceLocation rl) {
            return rl.toString();
        }
        if (nested != null && nested != value) {
            String extracted = stringifyIdLike(nested);
            if (extracted != null) {
                return extracted;
            }
        }

        List<String> matches = extractResourceLocationsFromText(safeString(value));
        return matches.isEmpty() ? null : matches.get(matches.size() - 1);
    }

    private List<Object> toObjectList(Object source) {
        List<Object> result = new ArrayList<>();
        source = unwrap(source);

        if (source == null) {
            return result;
        }

        if (source instanceof Stream<?> stream) {
            stream.forEach(result::add);
            return result;
        }

        if (source instanceof Map<?, ?> map) {
            result.addAll(map.values());
            return result;
        }

        if (source instanceof Collection<?> collection) {
            result.addAll(collection);
            return result;
        }

        if (source instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                result.add(element);
            }
            return result;
        }

        if (source.getClass().isArray()) {
            int len = Array.getLength(source);
            for (int i = 0; i < len; i++) {
                result.add(Array.get(source, i));
            }
            return result;
        }

        result.add(source);
        return result;
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        Class<?> current = type;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                result.add(field);
            }
            current = current.getSuperclass();
        }

        result.sort(Comparator.comparing(Field::getName));
        return result;
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeCompatible(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (method.getParameterCount() != args.length) {
                continue;
            }
            if (!parametersAccept(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Object invokeMethodSafe(Object target, Method method) {
        if (target == null || method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean parametersAccept(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }

            Class<?> paramType = box(paramTypes[i]);
            Class<?> argType = box(arg.getClass());
            if (!paramType.isAssignableFrom(argType)) {
                return false;
            }
        }
        return true;
    }

    private Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private Object tryReadField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }

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

    private Integer tryReadInt(Object target, String... names) {
        Object value = tryReadValue(target, names);
        value = unwrap(value);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private Float tryReadFloat(Object target, String... names) {
        Object value = tryReadValue(target, names);
        value = unwrap(value);
        if (value instanceof Number n) {
            return n.floatValue();
        }
        return null;
    }

    private Object tryReadValue(Object target, String... names) {
        for (String name : names) {
            Object byMethod = invokeNoArg(target, name);
            if (byMethod != null) {
                return byMethod;
            }

            Object byField = tryReadField(target, name);
            if (byField != null) {
                return byField;
            }
        }
        return null;
    }

    private Object unwrap(Object value) {
        if (value instanceof java.util.concurrent.atomic.AtomicReference<?> ref) {
            return ref.get();
        }
        return value;
    }

    private boolean shouldTrackIdentity(Object value) {
        return !(value instanceof String)
                && !(value instanceof Number)
                && !(value instanceof Boolean)
                && !(value instanceof Character)
                && !(value instanceof ResourceLocation)
                && !value.getClass().isEnum();
    }

    private boolean isSimpleValue(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof ResourceLocation
                || value.getClass().isEnum();
    }

    private boolean containsAnyKeyword(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Object resolveInternalRecipeManager(Object runtime, Object apiRecipeManager) {
        return firstNonNull(
                tryReadField(apiRecipeManager, "recipeManagerInternal"),
                tryReadField(apiRecipeManager, "internal"),
                tryReadField(apiRecipeManager, "delegate"),
                tryReadField(runtime, "recipeManagerInternal"),
                tryReadField(runtime, "recipeManager"),
                apiRecipeManager
        );
    }

    private String classifyGenerator(Object generator) {
        if (generator == null) {
            return "unknown";
        }

        String name = generator.getClass().getName().toLowerCase(Locale.ROOT);
        if (name.contains("classic")) return "classic";
        if (name.contains("standard")) return "standard";
        if (name.contains("layered")) return "layered";
        if (name.contains("veined")) return "veined";
        if (name.contains("dike")) return "dike";
        if (name.contains("cuboid")) return "cuboid";
        return generator.getClass().getSimpleName();
    }
    private String resolveOreVeinRegistryId(Object definition) {
        if (definition == null) {
            return null;
        }

        Object registry = GTRegistries.ORE_VEINS;
        if (registry == null) {
            return null;
        }

        // 1. entrySet()/getEntries() 系を最優先
        String byEntries = firstNonNull(
                tryResolveFromEntrySet(registry, definition),
                tryResolveFromEntriesMethod(registry, definition)
        );
        if (byEntries != null) {
            return byEntries;
        }

        // 2. getKey(value) 系があれば使う
        String byGetKey = tryResolveFromGetKey(registry, definition);
        if (byGetKey != null) {
            return byGetKey;
        }

        // 3. keySet + get(key) の組み合わせ
        String byKeySet = tryResolveFromKeySet(registry, definition);
        if (byKeySet != null) {
            return byKeySet;
        }

        return null;
    }
    private String tryResolveFromEntrySet(Object registry, Object targetDefinition) {
        Object entrySetObj = invokeNoArg(registry, "entrySet");
        if (entrySetObj == null) {
            return null;
        }

        for (Object entryObj : toObjectList(entrySetObj)) {
            if (!(entryObj instanceof Map.Entry<?, ?> entry)) {
                continue;
            }

            Object key = entry.getKey();
            Object value = entry.getValue();

            if (sameDefinition(value, targetDefinition)) {
                return normalizeRegistryKey(key);
            }
        }

        return null;
    }
    private String tryResolveFromEntriesMethod(Object registry, Object targetDefinition) {
        Object entriesObj = firstNonNull(
                invokeNoArg(registry, "getEntries"),
                invokeNoArg(registry, "entries")
        );
        if (entriesObj == null) {
            return null;
        }

        for (Object entryObj : toObjectList(entriesObj)) {
            Object key = firstNonNull(
                    invokeNoArg(entryObj, "getKey"),
                    tryReadField(entryObj, "key")
            );
            Object value = firstNonNull(
                    invokeNoArg(entryObj, "getValue"),
                    tryReadField(entryObj, "value")
            );

            if (sameDefinition(value, targetDefinition)) {
                return normalizeRegistryKey(key);
            }
        }

        return null;
    }
    private String tryResolveFromGetKey(Object registry, Object targetDefinition) {
        Object key = firstNonNull(
                invokeCompatible(registry, "getKey", targetDefinition),
                invokeCompatible(registry, "key", targetDefinition)
        );
        return normalizeRegistryKey(key);
    }
    private String tryResolveFromKeySet(Object registry, Object targetDefinition) {
        Object keySetObj = firstNonNull(
                invokeNoArg(registry, "keySet"),
                invokeNoArg(registry, "getKeys"),
                invokeNoArg(registry, "keys")
        );
        if (keySetObj == null) {
            return null;
        }

        for (Object key : toObjectList(keySetObj)) {
            Object value = firstNonNull(
                    invokeCompatible(registry, "get", key),
                    invokeCompatible(registry, "getValue", key),
                    invokeCompatible(registry, "value", key)
            );

            if (sameDefinition(value, targetDefinition)) {
                return normalizeRegistryKey(key);
            }
        }

        return null;
    }
    private boolean sameDefinition(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        try {
            return a.equals(b);
        } catch (Throwable ignored) {
            return false;
        }
    }
    private String normalizeRegistryKey(Object key) {
        key = unwrap(key);
        if (key == null) {
            return null;
        }

        if (key instanceof ResourceLocation rl) {
            return rl.toString();
        }

        Object nested = firstNonNull(
                invokeNoArg(key, "location"),
                invokeNoArg(key, "getId"),
                tryReadField(key, "id"),
                invokeNoArg(key, "getLocation"),
                tryReadField(key, "location"),
                invokeNoArg(key, "unwrapKey")
        );

        if (nested != null && nested != key) {
            return normalizeRegistryKey(nested);
        }

        String text = safeString(key);
        List<String> ids = extractResourceLocationsFromText(text);
        return ids.isEmpty() ? null : ids.get(ids.size() - 1);
    }
    private String classNameOf(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private String safeString(Object value) {
        if (value == null) {
            return "null";
        }

        String text;
        try {
            text = String.valueOf(value);
        } catch (Exception e) {
            return "<toString failed: " + e.getClass().getSimpleName() + ">";
        }

        if (text.length() > 220) {
            return text.substring(0, 220) + "...";
        }
        return text;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.equals("null") ? null : text;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
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

    public record OreVeinJeiDumpResult(
            Path outputPath,
            int count
    ) {
    }
}