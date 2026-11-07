package DIV.gtcsolo.integration.jei;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** Per-recipe-type colors for JEI's generated information labels. */
public final class JeiRecipeTextColors {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern RECIPE_TYPE_ID =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern RGB_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final int MAX_CONFIG_BYTES = 1024 * 1024;
    private static final Map<String, Integer> DEFAULT_COLORS = Map.of(
            "gtceu:large_chemical_reactor", 0xFF78CFFF,
            "gtceu:multiprocess_chemical_reactor", 0xFF78CFFF);

    private JeiRecipeTextColors() {}

    /** The holder is initialized once, only when JEI first creates a recipe wrapper. */
    private static final class Configuration {
        private static final Map<String, Integer> COLORS = loadConfiguration(
                FMLPaths.CONFIGDIR.get().resolve("gtcsolo/jei_recipe_text_colors.json"));
    }

    @Nullable
    public static Integer colorForRecipeType(String recipeTypeId) {
        return Configuration.COLORS.get(recipeTypeId);
    }

    /** Missing configuration uses defaults; malformed existing configuration disables styling. */
    public static Map<String, Integer> loadConfiguration(Path path) {
        try {
            if (Files.notExists(path)) return DEFAULT_COLORS;
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_CONFIG_BYTES) {
                throw new IOException("Expected a JSON file no larger than 1 MiB");
            }
            return parseConfiguration(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Cannot read JEI recipe text colors from {}; keeping original text colors", path, exception);
            return Map.of();
        }
    }

    /** Parse without Minecraft registry access, so future recipe types can be configured as well. */
    public static Map<String, Integer> parseConfiguration(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("JEI recipe text colors must be a JSON object");
        }
        JsonElement entries = parsed.getAsJsonObject().get("recipeTypes");
        if (entries == null || !entries.isJsonObject()) {
            throw new IllegalArgumentException("recipeTypes must be a JSON object");
        }
        JsonObject recipeTypes = entries.getAsJsonObject();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : recipeTypes.entrySet()) {
            JsonElement value = entry.getValue();
            if (!RECIPE_TYPE_ID.matcher(entry.getKey()).matches()
                    || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isString()
                    || !RGB_COLOR.matcher(value.getAsString()).matches()) {
                throw new IllegalArgumentException(
                        "Expected a namespace:path recipe type and a #RRGGBB color: " + entry.getKey());
            }
            result.put(entry.getKey(), 0xFF000000 | Integer.parseInt(value.getAsString().substring(1), 16));
        }
        return Map.copyOf(result);
    }
}
