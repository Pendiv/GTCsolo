package DIV.gtcsolo.dump.extend;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OreVeinIntrospector {
    private static final int MAX_STATIC_COLLECTION_SAMPLES = 3;
    private static final int MAX_STATIC_MAP_SAMPLES = 3;
    private static final int MAX_METHODS = 80;
    private static final int MAX_FIELDS = 80;

    /**
     * ここは「本ダンプ」ではなく、GTCEu の鉱脈関連の入口を探すための調査器。
     * class 候補を順番に試して、見つかった class の static field / method / sample object shape を出す。
     */
    public String probe(MinecraftServer server) {
        StringBuilder out = new StringBuilder(16_384);

        line(out, "=== GTCsolo Ore Vein Probe ===");
        line(out, "serverClass = " + server.getClass().getName());
        line(out, "");

        List<String> classCandidates = buildCandidateClasses();
        int foundCount = 0;

        for (String className : classCandidates) {
            Class<?> clazz = tryLoadClass(className);
            if (clazz == null) {
                continue;
            }

            foundCount++;
            appendClassProbe(out, clazz);
            line(out, "");
        }

        line(out, "foundCandidates = " + foundCount);
        line(out, "=== End Ore Vein Probe ===");

        String report = out.toString();
        for (String line : report.split("\\R")) {
            Gtcsolo.LOGGER.info("[ore_vein_probe] {}", line);
        }
        return report;
    }

    private void appendClassProbe(StringBuilder out, Class<?> clazz) {
        line(out, "--------------------------------------------------");
        line(out, "CLASS " + clazz.getName());
        line(out, "simpleName = " + clazz.getSimpleName());
        line(out, "superClass = " + (clazz.getSuperclass() == null ? "null" : clazz.getSuperclass().getName()));

        Field[] declaredFields = clazz.getDeclaredFields();
        Method[] declaredMethods = clazz.getDeclaredMethods();

        line(out, "declaredFieldCount = " + declaredFields.length);
        line(out, "declaredMethodCount = " + declaredMethods.length);

        line(out, "");
        line(out, "[FIELDS]");
        appendFields(out, clazz);

        line(out, "");
        line(out, "[METHODS]");
        appendMethods(out, clazz);

        line(out, "");
        line(out, "[STATIC FIELD VALUES]");
        appendStaticFieldValues(out, clazz);
    }

    private void appendFields(StringBuilder out, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> sorted = new ArrayList<>(List.of(fields));
        sorted.sort(Comparator.comparing(Field::getName));

        int count = 0;
        for (Field field : sorted) {
            if (count >= MAX_FIELDS) {
                line(out, "  ... truncated after " + count + " fields");
                break;
            }

            line(out, "  " + Modifier.toString(field.getModifiers())
                    + " " + field.getType().getName()
                    + " " + field.getName());
            count++;
        }
    }

    private void appendMethods(StringBuilder out, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        List<Method> sorted = new ArrayList<>(List.of(methods));
        sorted.sort(Comparator.comparing(Method::getName));

        int count = 0;
        for (Method method : sorted) {
            if (count >= MAX_METHODS) {
                line(out, "  ... truncated after " + count + " methods");
                break;
            }

            if (!looksRelevant(method)) {
                continue;
            }

            line(out, "  " + Modifier.toString(method.getModifiers())
                    + " " + method.getReturnType().getName()
                    + " " + method.getName()
                    + "(" + parameterSummary(method) + ")");
            count++;
        }

        if (count == 0) {
            line(out, "  (no relevant methods matched filter)");
        }
    }

    private void appendStaticFieldValues(StringBuilder out, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> sorted = new ArrayList<>(List.of(fields));
        sorted.sort(Comparator.comparing(Field::getName));

        boolean any = false;

        for (Field field : sorted) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            Object value;
            try {
                value = field.get(null);
            } catch (Throwable t) {
                line(out, "  " + field.getName() + " = <read failed: " + t.getClass().getSimpleName() + ">");
                any = true;
                continue;
            }

            if (value == null) {
                line(out, "  " + field.getName() + " = null");
                any = true;
                continue;
            }

            String valueClass = value.getClass().getName();
            line(out, "  " + field.getName() + " -> " + valueClass + " :: " + shorten(String.valueOf(value), 260));
            any = true;

            if (value instanceof Collection<?> collection) {
                appendCollectionSample(out, "    ", collection);
            } else if (value instanceof Map<?, ?> map) {
                appendMapSample(out, "    ", map);
            } else if (isInterestingPojo(value)) {
                appendObjectShape(out, "    ", value);
            }
        }

        if (!any) {
            line(out, "  (no static fields)");
        }
    }

    private void appendCollectionSample(StringBuilder out, String indent, Collection<?> collection) {
        line(out, indent + "collectionSize = " + collection.size());

        int i = 0;
        for (Object element : collection) {
            if (i >= MAX_STATIC_COLLECTION_SAMPLES) {
                line(out, indent + "... truncated after " + i + " elements");
                break;
            }

            line(out, indent + "[" + i + "] class = " + classNameOf(element));
            line(out, indent + "[" + i + "] text = " + shorten(String.valueOf(element), 220));

            if (isInterestingPojo(element)) {
                appendObjectShape(out, indent + "  ", element);
            }
            i++;
        }
    }

    private void appendMapSample(StringBuilder out, String indent, Map<?, ?> map) {
        line(out, indent + "mapSize = " + map.size());

        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (i >= MAX_STATIC_MAP_SAMPLES) {
                line(out, indent + "... truncated after " + i + " entries");
                break;
            }

            Object key = entry.getKey();
            Object value = entry.getValue();

            line(out, indent + "[" + i + "] keyClass = " + classNameOf(key));
            line(out, indent + "[" + i + "] keyText = " + shorten(String.valueOf(key), 220));
            line(out, indent + "[" + i + "] valueClass = " + classNameOf(value));
            line(out, indent + "[" + i + "] valueText = " + shorten(String.valueOf(value), 220));

            if (isInterestingPojo(value)) {
                appendObjectShape(out, indent + "  ", value);
            }
            i++;
        }
    }

    private void appendObjectShape(StringBuilder out, String indent, Object value) {
        Class<?> clazz = value.getClass();
        line(out, indent + "objectShape: " + clazz.getName());

        Field[] fields = clazz.getDeclaredFields();
        List<Field> sorted = new ArrayList<>(List.of(fields));
        sorted.sort(Comparator.comparing(Field::getName));

        int count = 0;
        for (Field field : sorted) {
            if (count >= 20) {
                line(out, indent + "  ... truncated after " + count + " fields");
                break;
            }

            field.setAccessible(true);
            try {
                Object nested = field.get(value);
                line(out, indent + "  " + field.getType().getName() + " " + field.getName()
                        + " = " + shorten(String.valueOf(nested), 180));
            } catch (Throwable t) {
                line(out, indent + "  " + field.getType().getName() + " " + field.getName()
                        + " = <read failed: " + t.getClass().getSimpleName() + ">");
            }
            count++;
        }
    }

    private boolean looksRelevant(Method method) {
        String name = method.getName().toLowerCase(Locale.ROOT);
        String ret = method.getReturnType().getName().toLowerCase(Locale.ROOT);

        return name.contains("vein")
                || name.contains("ore")
                || name.contains("layer")
                || name.contains("world")
                || name.contains("generator")
                || name.contains("deposit")
                || name.contains("weight")
                || name.contains("height")
                || name.contains("dimension")
                || ret.contains("vein")
                || ret.contains("ore")
                || ret.contains("layer")
                || ret.contains("worldgen")
                || ret.contains("generator")
                || ret.contains("map")
                || ret.contains("list")
                || ret.contains("collection");
    }

    private boolean isInterestingPojo(Object value) {
        if (value == null) {
            return false;
        }

        Class<?> clazz = value.getClass();
        return !clazz.isPrimitive()
                && clazz != String.class
                && !Number.class.isAssignableFrom(clazz)
                && clazz != Boolean.class
                && !clazz.isEnum()
                && !clazz.isArray()
                && !(value instanceof Collection<?>)
                && !(value instanceof Map<?, ?>);
    }

    private String parameterSummary(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            return "";
        }

        List<String> names = new ArrayList<>();
        for (Class<?> param : params) {
            names.add(param.getName());
        }
        return String.join(", ", names);
    }

    private String classNameOf(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private String shorten(String text, int max) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    private void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private Class<?> tryLoadClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }


    private List<String> buildCandidateClasses() {
        return List.of(
                // broad GT / worldgen
                "com.gregtechceu.gtceu.api.data.worldgen.GTWorldGen",
                "com.gregtechceu.gtceu.api.data.worldgen.GTWorldGenData",
                "com.gregtechceu.gtceu.api.data.worldgen.WorldGenRegistry",
                "com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils",
                "com.gregtechceu.gtceu.common.data.GTWorldGen",
                "com.gregtechceu.gtceu.common.data.worldgen.GTWorldGen",
                "com.gregtechceu.gtceu.common.data.worldgen.WorldGenRegistry",

                // vein / generator packages
                "com.gregtechceu.gtceu.api.data.worldgen.generator.veins.VeinGenerator",
                "com.gregtechceu.gtceu.api.data.worldgen.generator.veins.ClassicVeinGenerator",
                "com.gregtechceu.gtceu.api.data.worldgen.generator.veins.StandardVeinGenerator",
                "com.gregtechceu.gtceu.api.data.worldgen.generator.veins.OreVeinDefinition",
                "com.gregtechceu.gtceu.api.data.worldgen.generator.veins.VeinDefinition",
                "com.gregtechceu.gtceu.api.data.worldgen.generator.veins.LayeredVeinGenerator",
                "com.gregtechceu.gtceu.api.data.worldgen.generator.veins.LayeredVeinDefinition",

                // feature / placed feature style
                "com.gregtechceu.gtceu.api.data.worldgen.feature.GTOreFeature",
                "com.gregtechceu.gtceu.api.data.worldgen.feature.GTVeinFeature",
                "com.gregtechceu.gtceu.common.worldgen.GTOreFeature",
                "com.gregtechceu.gtceu.common.worldgen.GTVeinFeature",

                // registries / holders / definitions
                "com.gregtechceu.gtceu.api.registry.registrate.GTRegistries",
                "com.gregtechceu.gtceu.api.registry.GTRegistries",
                "com.gregtechceu.gtceu.api.data.worldgen.OreDepositDefinition",
                "com.gregtechceu.gtceu.api.data.worldgen.DepositDefinition",
                "com.gregtechceu.gtceu.api.data.worldgen.OreVeinWorldGenDefinition",
                "com.gregtechceu.gtceu.api.data.worldgen.WorldGenDefinition",

                // jei side, as fallback clues
                "com.gregtechceu.gtceu.integration.jei.GTJeiPlugin",
                "com.gregtechceu.gtceu.integration.jei.recipe.category.OreVeinInfoCategory",
                "com.gregtechceu.gtceu.integration.jei.recipe.category.GTOreCategory",
                "com.gregtechceu.gtceu.integration.jei.recipe.category.GTOreVeinCategory",
                "com.gregtechceu.gtceu.integration.jei.recipe.OreVeinInfoRecipe",
                "com.gregtechceu.gtceu.integration.jei.recipe.GTOreInfoRecipe",

                // prospector / ui clue classes
                "com.gregtechceu.gtceu.common.item.behavior.ProspectorBehavior",
                "com.gregtechceu.gtceu.api.item.toolitem.behavior.ProspectorBehavior"
        );
    }
}