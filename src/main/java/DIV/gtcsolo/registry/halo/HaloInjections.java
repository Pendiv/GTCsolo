package DIV.gtcsolo.registry.halo;

import DIV.gtcsolo.Gtcsolo;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common-side declaration registry for item halos.
 *
 * <p>Declarations are data only. The client applies them after all generated item models have
 * entered GTCEu's dynamic resource pack.</p>
 */
public final class HaloInjections {

    private static final List<HaloInjection> DECLARATIONS = new ArrayList<>();
    private static final Set<ResourceLocation> TARGETS = new HashSet<>();
    private static boolean bootstrapped;

    private HaloInjections() {}

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        Gtcsolo.LOGGER.info(
                "[HaloInjection] declaration registry ready; external startup declarations may now be added");
    }

    public static void attach(
            ResourceLocation targetItemId,
            ResourceLocation haloTexture,
            int color,
            int size,
            boolean pulse) {
        attach(targetItemId, haloTexture, color, size, pulse, List.of());
    }

    public static void attach(
            ResourceLocation targetItemId,
            ResourceLocation haloTexture,
            int color,
            int size,
            boolean pulse,
            List<Integer> layerColors) {
        HaloInjection declaration = new HaloInjection(
                targetItemId,
                haloTexture,
                color,
                size,
                pulse,
                layerColors,
                HaloInjection.ColorMode.STATIC,
                0L);
        if (!TARGETS.add(targetItemId)) {
            throw new IllegalArgumentException("Duplicate halo target: " + targetItemId);
        }
        DECLARATIONS.add(declaration);
    }

    public static void attachRainbow(
            ResourceLocation targetItemId,
            ResourceLocation haloTexture,
            int size,
            boolean pulse,
            long cycleMillis) {
        attachRainbow(targetItemId, haloTexture, size, pulse, cycleMillis, List.of());
    }

    public static void attachRainbow(
            ResourceLocation targetItemId,
            ResourceLocation haloTexture,
            int size,
            boolean pulse,
            long cycleMillis,
            List<Integer> layerColors) {
        HaloInjection declaration = new HaloInjection(
                targetItemId,
                haloTexture,
                0xFFFFFFFF,
                size,
                pulse,
                layerColors,
                HaloInjection.ColorMode.RAINBOW,
                cycleMillis);
        if (!TARGETS.add(targetItemId)) {
            throw new IllegalArgumentException("Duplicate halo target: " + targetItemId);
        }
        DECLARATIONS.add(declaration);
    }

    public static List<HaloInjection> declarations() {
        return List.copyOf(DECLARATIONS);
    }

    public static boolean containsTarget(ResourceLocation targetItemId) {
        return TARGETS.contains(targetItemId);
    }
}
