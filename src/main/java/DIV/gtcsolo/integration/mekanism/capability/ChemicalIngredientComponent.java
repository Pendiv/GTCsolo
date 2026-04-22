package DIV.gtcsolo.integration.mekanism.capability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import org.slf4j.Logger;

/**
 * KubeJS recipe schema 用の ChemicalIngredient コンポーネント.
 *
 * JS 側からは以下の形で渡せる:
 *   - "mekanism:hydrogen 1000"            (type prefix 省略、defaultType で補完)
 *   - "mekanism:hydrogen"                 (amount=1000 default)
 *   - "gas:mekanism:hydrogen 1000"         (明示prefix)
 *   - JsonObject { type, id, amount }
 *   - 既にChemicalIngredient インスタンス
 *
 * 4 種 (GAS/INFUSION/PIGMENT/SLURRY) それぞれ 1 インスタンスを持つ.
 */
public class ChemicalIngredientComponent implements RecipeComponent<ChemicalIngredient> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ChemicalIngredientComponent GAS      = new ChemicalIngredientComponent(ChemicalIngredient.Type.GAS);
    public static final ChemicalIngredientComponent INFUSION = new ChemicalIngredientComponent(ChemicalIngredient.Type.INFUSION);
    public static final ChemicalIngredientComponent PIGMENT  = new ChemicalIngredientComponent(ChemicalIngredient.Type.PIGMENT);
    public static final ChemicalIngredientComponent SLURRY   = new ChemicalIngredientComponent(ChemicalIngredient.Type.SLURRY);

    private final ChemicalIngredient.Type defaultType;
    private final SerializerChemicalIngredient serializer;

    private ChemicalIngredientComponent(ChemicalIngredient.Type defaultType) {
        this.defaultType = defaultType;
        this.serializer = SerializerChemicalIngredient.forType(defaultType);
    }

    @Override
    public Class<?> componentClass() {
        return ChemicalIngredient.class;
    }

    @Override
    public JsonElement write(RecipeJS recipe, ChemicalIngredient value) {
        return ChemicalIngredient.CODEC.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(false, err -> {
                    throw new IllegalStateException("ChemicalIngredient write error: " + err);
                });
    }

    @Override
    public ChemicalIngredient read(RecipeJS recipe, Object from) {
        LOGGER.debug("[ChemCap] Component[{}].read input={} ({})",
                defaultType, from, from == null ? "null" : from.getClass().getName());
        if (from == null) return ChemicalIngredient.empty(defaultType);
        if (from instanceof ChemicalIngredient ci) return ci;
        if (from instanceof JsonObject json) return serializer.fromJson(json);
        if (from instanceof JsonElement je) {
            // 文字列 JSON element も拾う
            if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isString()) {
                return serializer.parseString(je.getAsString());
            }
            if (je.isJsonObject()) return serializer.fromJson(je);
        }
        if (from instanceof CharSequence cs) return serializer.parseString(cs.toString());
        LOGGER.error("[ChemCap] Component[{}].read UNHANDLED type: {}", defaultType, from.getClass().getName());
        throw new IllegalArgumentException(
                "Cannot parse ChemicalIngredient from: " + from.getClass().getName() + " = " + from);
    }

    @Override
    public String toString() {
        return "chemical_" + defaultType.lowerName();
    }
}