package DIV.gtcsolo.api.halo;

import DIV.gtcsolo.registry.halo.HaloInjections;

import net.minecraft.resources.ResourceLocation;

/**
 * Stable string-based entry point intended for KubeJS startup scripts and other addons.
 */
public final class HaloAPI {

    public static final String DEFAULT_HALO_TEXTURE = "avaritia:misc/halo";

    private HaloAPI() {}

    public static void fixed(
            String targetItemId,
            String argb,
            int size,
            boolean pulse) {
        fixed(targetItemId, DEFAULT_HALO_TEXTURE, argb, size, pulse);
    }

    public static void fixed(
            String targetItemId,
            String haloTexture,
            String argb,
            int size,
            boolean pulse) {
        HaloInjections.attach(
                resourceLocation(targetItemId, "targetItemId"),
                resourceLocation(haloTexture, "haloTexture"),
                parseArgb(argb),
                size,
                pulse);
    }

    public static void rainbow(
            String targetItemId,
            int size,
            boolean pulse,
            long cycleMillis) {
        rainbow(targetItemId, DEFAULT_HALO_TEXTURE, size, pulse, cycleMillis);
    }

    public static void rainbow(
            String targetItemId,
            String haloTexture,
            int size,
            boolean pulse,
            long cycleMillis) {
        HaloInjections.attachRainbow(
                resourceLocation(targetItemId, "targetItemId"),
                resourceLocation(haloTexture, "haloTexture"),
                size,
                pulse,
                cycleMillis);
    }

    public static boolean has(String targetItemId) {
        return HaloInjections.containsTarget(resourceLocation(targetItemId, "targetItemId"));
    }

    private static ResourceLocation resourceLocation(String value, String argumentName) {
        ResourceLocation location = ResourceLocation.tryParse(value);
        if (location == null) {
            throw new IllegalArgumentException(argumentName + " is not a valid resource location: " + value);
        }
        return location;
    }

    private static int parseArgb(String value) {
        if (value == null) {
            throw new IllegalArgumentException("argb cannot be null");
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        if (normalized.length() == 6) {
            normalized = "FF" + normalized;
        }
        if (!normalized.matches("[0-9a-fA-F]{8}")) {
            throw new IllegalArgumentException(
                    "argb must be RRGGBB or AARRGGBB hex, for example #C64CFF or #FFC64CFF: " + value);
        }
        return (int) Long.parseUnsignedLong(normalized, 16);
    }
}
