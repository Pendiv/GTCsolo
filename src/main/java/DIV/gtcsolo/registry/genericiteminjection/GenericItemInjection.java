package DIV.gtcsolo.registry.genericiteminjection;

import DIV.gtcsolo.registry.iteminjection.KnownItemInjection;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

/**
 * One gtcsolo-owned item whose appearance is derived from an arbitrary loaded mod's item texture.
 */
public record GenericItemInjection(
        ResourceLocation id,
        ResourceLocation sourceItemId,
        ResourceLocation sourceTexture,
        String displayName,
        String variantLabel,
        int targetColor,
        KnownItemInjection.RecolorMethod method,
        KnownItemInjection.Stage stage) {

    public GenericItemInjection {
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
}
