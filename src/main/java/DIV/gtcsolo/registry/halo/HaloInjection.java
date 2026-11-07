package DIV.gtcsolo.registry.halo;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * Declares a Re-Avaritia halo for an existing item model.
 *
 * <p>The target item itself is not replaced. On the client, its model JSON is decorated with
 * either Re-Avaritia's public {@code avaritia:halo} loader or gtcsolo's compatible
 * real-time-color loader.</p>
 */
public record HaloInjection(
        ResourceLocation targetItemId,
        ResourceLocation haloTexture,
        int color,
        int size,
        boolean pulse,
        List<Integer> layerColors,
        ColorMode colorMode,
        long cycleMillis) {

    public HaloInjection {
        Objects.requireNonNull(targetItemId, "targetItemId");
        Objects.requireNonNull(haloTexture, "haloTexture");
        Objects.requireNonNull(layerColors, "layerColors");
        Objects.requireNonNull(colorMode, "colorMode");
        if (size < 0 || size > 64) {
            throw new IllegalArgumentException("Halo size must be between 0 and 64: " + size);
        }
        if (colorMode == ColorMode.STATIC && cycleMillis != 0L) {
            throw new IllegalArgumentException("Static halo cannot have a color cycle");
        }
        if (colorMode == ColorMode.RAINBOW && cycleMillis < 250L) {
            throw new IllegalArgumentException("Rainbow halo cycle must be at least 250 ms");
        }
        layerColors = List.copyOf(layerColors);
    }

    public enum ColorMode {
        STATIC,
        RAINBOW
    }
}
