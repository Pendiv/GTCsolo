package DIV.gtcsolo.registry.iteminjection;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

/**
 * One concrete item produced from a known item's texture.
 *
 * <p>The source item remains untouched. This declaration owns a new registry ID and a dynamically generated
 * texture/model.</p>
 */
public record KnownItemInjection(
        ResourceLocation id,
        ResourceLocation sourceItemId,
        ResourceLocation sourceTexture,
        String displayName,
        String variantLabel,
        int targetColor,
        RecolorMethod method,
        Stage stage) {

    public KnownItemInjection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceItemId, "sourceItemId");
        Objects.requireNonNull(sourceTexture, "sourceTexture");
        Objects.requireNonNull(variantLabel, "variantLabel");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(stage, "stage");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank for " + id);
        }
    }

    public String targetHex() {
        return String.format(Locale.ROOT, "%06X", targetColor & 0xFFFFFF);
    }

    public enum RecolorMethod {
        MULTIPLY("multiply", "Multiply"),
        LUMA_TINT("luma_tint", "Luma Tint"),
        HUE_SHIFT("hue_shift", "Hue Shift"),
        LINEAR_TRANSFER("linear_transfer", "Linear Transfer"),
        SELECTIVE_TRANSFER("selective_transfer", "Selective Transfer");

        private final String id;
        private final String displayName;

        RecolorMethod(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum Stage {
        PROTOTYPE,
        FINISHED
    }
}
