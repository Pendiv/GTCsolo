package DIV.gtcsolo.integration.mekanism.capability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * ChemicalIngredient の (JSON + Network) シリアライザ.
 * 各 ChemicalRecipeCapability インスタンスは、対応 type を持つ専用 Serializer を持つ.
 *
 * 文字列入力 "namespace:path [amount]" に type prefix が無い場合、defaultType で補完する.
 * "gas:mekanism:hydrogen 1000" のように明示 prefix も受付.
 */
public final class SerializerChemicalIngredient implements IContentSerializer<ChemicalIngredient> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final SerializerChemicalIngredient GAS      = new SerializerChemicalIngredient(ChemicalIngredient.Type.GAS);
    public static final SerializerChemicalIngredient INFUSION = new SerializerChemicalIngredient(ChemicalIngredient.Type.INFUSION);
    public static final SerializerChemicalIngredient PIGMENT  = new SerializerChemicalIngredient(ChemicalIngredient.Type.PIGMENT);
    public static final SerializerChemicalIngredient SLURRY   = new SerializerChemicalIngredient(ChemicalIngredient.Type.SLURRY);

    private final ChemicalIngredient.Type defaultType;

    private SerializerChemicalIngredient(ChemicalIngredient.Type defaultType) {
        this.defaultType = defaultType;
    }

    public static SerializerChemicalIngredient forType(ChemicalIngredient.Type type) {
        switch (type) {
            case GAS:      return GAS;
            case INFUSION: return INFUSION;
            case PIGMENT:  return PIGMENT;
            case SLURRY:   return SLURRY;
        }
        throw new IllegalStateException();
    }

    @Override
    public Codec<ChemicalIngredient> codec() {
        return ChemicalIngredient.CODEC;
    }

    @Override
    public ChemicalIngredient fromJson(JsonElement json) {
        return codec().parse(JsonOps.INSTANCE, json)
                .getOrThrow(false, err -> {
                    throw new IllegalStateException("ChemicalIngredient JSON parse error: " + err);
                });
    }

    @Override
    public JsonElement toJson(ChemicalIngredient content) {
        return codec().encodeStart(JsonOps.INSTANCE, content)
                .getOrThrow(false, err -> {
                    throw new IllegalStateException("ChemicalIngredient JSON encode error: " + err);
                });
    }

    /**
     * KubeJS 等から渡された任意オブジェクトを ChemicalIngredient に解釈.
     * - ChemicalIngredient: そのまま
     * - JsonObject: CODEC で decode
     * - CharSequence: parseString (defaultType fallback)
     */
    @Override
    public ChemicalIngredient of(Object o) {
        LOGGER.debug("[ChemCap] Serializer[{}].of input={} ({})",
                defaultType, o, o == null ? "null" : o.getClass().getName());
        if (o instanceof ChemicalIngredient ci) return ci;
        if (o instanceof JsonObject json) return fromJson(json);
        if (o instanceof CharSequence cs) return parseString(cs.toString());
        LOGGER.error("[ChemCap] Serializer[{}].of CANNOT coerce {}",
                defaultType, o == null ? "null" : o.getClass().getName());
        throw new IllegalArgumentException(
                "Cannot coerce to ChemicalIngredient: " + (o == null ? "null" : o.getClass().getName()));
    }

    /**
     * 文字列パース. 受付フォーマット:
     *   "namespace:path"             → defaultType, amount=1000
     *   "namespace:path 1000"        → defaultType, amount=1000
     *   "gas:namespace:path 1000"    → 明示type
     *   "infusion:ns:p"              → 明示type, amount=1000 default
     */
    public ChemicalIngredient parseString(String s) {
        LOGGER.debug("[ChemCap] parseString defaultType={} raw='{}'", defaultType, s);
        String trimmed = s.trim();
        long amount = 1000L;
        int space = trimmed.indexOf(' ');
        if (space >= 0) {
            amount = Long.parseLong(trimmed.substring(space + 1).trim());
            trimmed = trimmed.substring(0, space).trim();
        }

        // 空白分離後のトークンで type prefix を判定.
        // colon 数 = 2 なら type 明示 ("gas:ns:path"), = 1 なら defaultType ("ns:path")
        int first = trimmed.indexOf(':');
        if (first < 0) {
            throw new IllegalArgumentException("Expected 'namespace:path', got: " + s);
        }
        int second = trimmed.indexOf(':', first + 1);

        ChemicalIngredient.Type type;
        String idStr;
        if (second > 0) {
            String prefix = trimmed.substring(0, first).toLowerCase();
            try {
                type = ChemicalIngredient.Type.valueOf(prefix.toUpperCase());
                idStr = trimmed.substring(first + 1);
            } catch (IllegalArgumentException nonType) {
                // prefix が type と一致しない → colon 2個は ns:path:variant みたいな形ではない.
                // namespace:path として扱い、defaultType を使う.
                type = defaultType;
                idStr = trimmed;
            }
        } else {
            type = defaultType;
            idStr = trimmed;
        }
        ChemicalIngredient result = new ChemicalIngredient(type, new ResourceLocation(idStr), amount);
        LOGGER.debug("[ChemCap] parseString -> {}", result);
        return result;
    }

    @Override
    public ChemicalIngredient defaultValue() {
        return ChemicalIngredient.empty(defaultType);
    }
}