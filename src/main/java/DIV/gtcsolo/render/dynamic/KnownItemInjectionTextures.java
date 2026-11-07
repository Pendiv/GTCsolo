package DIV.gtcsolo.render.dynamic;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.iteminjection.KnownItemInjection;
import DIV.gtcsolo.registry.iteminjection.KnownItemInjections;

import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client resource generator for known-item injection declarations.
 *
 * <p>Transforms the complete PNG strip, so animated textures keep every frame. The source .png.mcmeta is
 * copied into the dynamic resource pack for every generated item.</p>
 */
public final class KnownItemInjectionTextures {

    private KnownItemInjectionTextures() {}

    public static void generateAll() {
        List<Request> requests = KnownItemInjections.declarations().stream()
                .map(declaration -> new Request(
                        declaration.id(),
                        declaration.sourceTexture(),
                        declaration.targetColor(),
                        declaration.method()))
                .toList();
        generate("KnownItemInjection", requests);
    }

    public static void generate(String logLabel, List<Request> requests) {
        Map<ResourceLocation, SourceImage> sourceCache = new HashMap<>();
        int generated = 0;
        int failed = 0;

        try {
            for (Request request : requests) {
                // A null result is a cached missing source too: one absent PNG can back hundreds of variants.
                if (!sourceCache.containsKey(request.sourceTexture())) {
                    sourceCache.put(request.sourceTexture(), loadSource(logLabel, request.sourceTexture()));
                }
                SourceImage source = sourceCache.get(request.sourceTexture());
                if (source == null) {
                    failed++;
                    continue;
                }
                NativeImage transformed = transform(source, request);
                try {
                    GTDynamicResourcePack.addItemTexture(request.id(), transformed.asByteArray());
                    injectModel(request.id());
                    if (source.metadata() != null) {
                        GTDynamicResourcePack.DATA.put(
                                itemTextureMetadataLocation(request.id()),
                                source.metadata());
                    }
                    generated++;
                } catch (IOException exception) {
                    Gtcsolo.LOGGER.error("[{}] PNG encode failed for {}: {}",
                            logLabel, request.id(), exception.getMessage(), exception);
                    failed++;
                } finally {
                    transformed.close();
                }
            }
        } finally {
            sourceCache.values().stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(source -> source.image().close());
        }

        Gtcsolo.LOGGER.info(
                "[{}] generated {} candidate texture/model pair(s), failed={}",
                logLabel,
                generated,
                failed);
    }

    private static SourceImage loadSource(String logLabel, ResourceLocation texture) {
        ResourceLocation pngLocation = new ResourceLocation(
                texture.getNamespace(),
                "textures/" + texture.getPath() + ".png");
        byte[] png = loadResource(pngLocation);
        if (png == null) {
            Gtcsolo.LOGGER.error("[{}] source texture missing: {}", logLabel, pngLocation);
            return null;
        }

        try (InputStream stream = new java.io.ByteArrayInputStream(png)) {
            NativeImage image = NativeImage.read(stream);
            byte[] metadata = loadResource(new ResourceLocation(
                    texture.getNamespace(),
                    pngLocation.getPath() + ".mcmeta"));
            return new SourceImage(image, metadata, analyse(image));
        } catch (IOException exception) {
            Gtcsolo.LOGGER.error("[{}] source PNG decode failed for {}: {}",
                    logLabel, pngLocation, exception.getMessage(), exception);
            return null;
        }
    }

    private static byte[] loadResource(ResourceLocation location) {
        IModFileInfo modFileInfo = ModList.get().getModFileById(location.getNamespace());
        if (modFileInfo != null) {
            Path path = modFileInfo.getFile().findResource(
                    "assets/" + location.getNamespace() + "/" + location.getPath());
            byte[] bundled = readResource(path, location);
            if (bundled != null) {
                return bundled;
            }
        }

        Path assetsRoot = FMLPaths.GAMEDIR.get()
                .resolve("kubejs")
                .resolve("assets")
                .resolve(location.getNamespace())
                .normalize();
        Path loosePath = assetsRoot.resolve(location.getPath()).normalize();
        if (!loosePath.startsWith(assetsRoot)) {
            Gtcsolo.LOGGER.error(
                    "[KnownItemInjection] rejected resource path outside KubeJS assets: {}",
                    location);
            return null;
        }
        return readResource(loosePath, location);
    }

    private static byte[] readResource(Path path, ResourceLocation location) {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            Gtcsolo.LOGGER.error("[KnownItemInjection] resource read failed for {}: {}",
                    location, exception.getMessage(), exception);
            return null;
        }
    }

    private static NativeImage transform(SourceImage source, Request request) {
        NativeImage input = source.image();
        NativeImage output = new NativeImage(input.getWidth(), input.getHeight(), false);
        float[] target = rgb(request.targetColor());
        float[] targetHsv = rgbToHsv(target[0], target[1], target[2]);

        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                int pixel = input.getPixelRGBA(x, y);
                int alphaByte = (pixel >>> 24) & 0xFF;
                float alpha = alphaByte / 255.0F;
                float red = (pixel & 0xFF) / 255.0F;
                float green = ((pixel >>> 8) & 0xFF) / 255.0F;
                float blue = ((pixel >>> 16) & 0xFF) / 255.0F;

                float[] result = switch (request.method()) {
                    case MULTIPLY -> new float[] {
                            red * target[0],
                            green * target[1],
                            blue * target[2]
                    };
                    case LUMA_TINT -> lumaTint(red, green, blue, target);
                    case HUE_SHIFT -> hueShift(
                            red, green, blue, source.analysis().dominantHue(), targetHsv[0]);
                    case LINEAR_TRANSFER -> linearTransfer(
                            red, green, blue, source.analysis().sourceRgb(), target);
                    case SELECTIVE_TRANSFER -> selectiveTransfer(
                            red, green, blue, alpha,
                            source.analysis().dominantHue(), targetHsv);
                };

                int outR = Math.round(clamp01(result[0]) * 255.0F);
                int outG = Math.round(clamp01(result[1]) * 255.0F);
                int outB = Math.round(clamp01(result[2]) * 255.0F);
                output.setPixelRGBA(
                        x,
                        y,
                        (alphaByte << 24) | (outB << 16) | (outG << 8) | outR);
            }
        }
        return output;
    }

    private static Analysis analyse(NativeImage image) {
        double sin = 0.0;
        double cos = 0.0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                float alpha = ((pixel >>> 24) & 0xFF) / 255.0F;
                float[] hsv = rgbToHsv(
                        (pixel & 0xFF) / 255.0F,
                        ((pixel >>> 8) & 0xFF) / 255.0F,
                        ((pixel >>> 16) & 0xFF) / 255.0F);
                if (alpha <= 0.05F || hsv[1] <= 0.20F || hsv[2] <= 0.10F) {
                    continue;
                }
                double weight = hsv[1] * hsv[1] * hsv[2] * alpha;
                double angle = hsv[0] * Math.PI * 2.0;
                sin += Math.sin(angle) * weight;
                cos += Math.cos(angle) * weight;
            }
        }

        float dominantHue = (float) (Math.atan2(sin, cos) / (Math.PI * 2.0));
        if (dominantHue < 0.0F) dominantHue += 1.0F;

        double redTotal = 0.0;
        double greenTotal = 0.0;
        double blueTotal = 0.0;
        double weightTotal = 0.0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                float alpha = ((pixel >>> 24) & 0xFF) / 255.0F;
                float red = (pixel & 0xFF) / 255.0F;
                float green = ((pixel >>> 8) & 0xFF) / 255.0F;
                float blue = ((pixel >>> 16) & 0xFF) / 255.0F;
                float[] hsv = rgbToHsv(red, green, blue);
                if (alpha <= 0.05F || hsv[1] <= 0.20F || hsv[2] <= 0.10F ||
                        hueDistance(hsv[0], dominantHue) >= 0.12F) {
                    continue;
                }
                double weight = hsv[1] * hsv[1] * hsv[2] * alpha;
                redTotal += red * weight;
                greenTotal += green * weight;
                blueTotal += blue * weight;
                weightTotal += weight;
            }
        }
        float[] sourceRgb = weightTotal == 0.0 ?
                new float[] { 0.5F, 0.5F, 0.5F } :
                new float[] {
                        (float) (redTotal / weightTotal),
                        (float) (greenTotal / weightTotal),
                        (float) (blueTotal / weightTotal)
                };
        return new Analysis(dominantHue, sourceRgb);
    }

    private static float[] lumaTint(float red, float green, float blue, float[] target) {
        float linearR = srgbToLinear(red);
        float linearG = srgbToLinear(green);
        float linearB = srgbToLinear(blue);
        float luminance = linearR * 0.2126F + linearG * 0.7152F + linearB * 0.0722F;
        float targetR = srgbToLinear(target[0]);
        float targetG = srgbToLinear(target[1]);
        float targetB = srgbToLinear(target[2]);
        float targetLuminance = Math.max(
                targetR * 0.2126F + targetG * 0.7152F + targetB * 0.0722F,
                0.000001F);
        return new float[] {
                linearToSrgb(luminance * targetR / targetLuminance),
                linearToSrgb(luminance * targetG / targetLuminance),
                linearToSrgb(luminance * targetB / targetLuminance)
        };
    }

    private static float[] hueShift(
            float red, float green, float blue, float sourceHue, float targetHue) {
        float[] hsv = rgbToHsv(red, green, blue);
        hsv[0] = wrapHue(hsv[0] + targetHue - sourceHue);
        return hsvToRgb(hsv[0], hsv[1], hsv[2]);
    }

    private static float[] linearTransfer(
            float red, float green, float blue, float[] source, float[] target) {
        return new float[] {
                linearToSrgb(srgbToLinear(red) - srgbToLinear(source[0]) + srgbToLinear(target[0])),
                linearToSrgb(srgbToLinear(green) - srgbToLinear(source[1]) + srgbToLinear(target[1])),
                linearToSrgb(srgbToLinear(blue) - srgbToLinear(source[2]) + srgbToLinear(target[2]))
        };
    }

    private static float[] selectiveTransfer(
            float red, float green, float blue, float alpha,
            float sourceHue, float[] targetHsv) {
        float[] hsv = rgbToHsv(red, green, blue);
        float distance = hueDistance(hsv[0], sourceHue);
        float hueWeight = 1.0F - smoothstep(0.05F, 0.24F, distance);
        float chromaWeight = smoothstep(0.08F, 0.42F, hsv[1]);
        float weight = hueWeight * chromaWeight * alpha;
        float delta = wrapSignedHue(targetHsv[0] - hsv[0]);
        hsv[0] = wrapHue(hsv[0] + delta * weight);
        float targetSaturation = Math.max(targetHsv[1] * 0.85F, hsv[1]);
        hsv[1] += (targetSaturation - hsv[1]) * weight * 0.55F;
        return hsvToRgb(hsv[0], hsv[1], hsv[2]);
    }

    private static void injectModel(ResourceLocation itemId) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", itemId.getNamespace() + ":item/" + itemId.getPath());
        model.add("textures", textures);
        GTDynamicResourcePack.addItemModel(itemId, model);
    }

    private static ResourceLocation itemTextureMetadataLocation(ResourceLocation itemId) {
        return new ResourceLocation(
                itemId.getNamespace(),
                "textures/item/" + itemId.getPath() + ".png.mcmeta");
    }

    private static float[] rgb(int color) {
        return new float[] {
                ((color >>> 16) & 0xFF) / 255.0F,
                ((color >>> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        };
    }

    private static float[] rgbToHsv(float red, float green, float blue) {
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;
        float hue;
        if (delta == 0.0F) {
            hue = 0.0F;
        } else if (max == red) {
            hue = ((green - blue) / delta) % 6.0F;
        } else if (max == green) {
            hue = (blue - red) / delta + 2.0F;
        } else {
            hue = (red - green) / delta + 4.0F;
        }
        hue = wrapHue(hue / 6.0F);
        float saturation = max == 0.0F ? 0.0F : delta / max;
        return new float[] { hue, saturation, max };
    }

    private static float[] hsvToRgb(float hue, float saturation, float value) {
        float scaled = wrapHue(hue) * 6.0F;
        int sector = (int) Math.floor(scaled);
        float fraction = scaled - sector;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - fraction * saturation);
        float t = value * (1.0F - (1.0F - fraction) * saturation);
        return switch (sector % 6) {
            case 0 -> new float[] { value, t, p };
            case 1 -> new float[] { q, value, p };
            case 2 -> new float[] { p, value, t };
            case 3 -> new float[] { p, q, value };
            case 4 -> new float[] { t, p, value };
            default -> new float[] { value, p, q };
        };
    }

    private static float srgbToLinear(float value) {
        return value <= 0.04045F ?
                value / 12.92F :
                (float) Math.pow((value + 0.055F) / 1.055F, 2.4);
    }

    private static float linearToSrgb(float value) {
        value = clamp01(value);
        return value <= 0.0031308F ?
                value * 12.92F :
                1.055F * (float) Math.pow(value, 1.0 / 2.4) - 0.055F;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float scaled = clamp01((value - edge0) / (edge1 - edge0));
        return scaled * scaled * (3.0F - 2.0F * scaled);
    }

    private static float hueDistance(float first, float second) {
        return Math.abs(wrapSignedHue(first - second));
    }

    private static float wrapHue(float hue) {
        hue %= 1.0F;
        return hue < 0.0F ? hue + 1.0F : hue;
    }

    private static float wrapSignedHue(float hue) {
        return (hue + 0.5F - (float) Math.floor(hue + 0.5F)) - 0.5F;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public record Request(
            ResourceLocation id,
            ResourceLocation sourceTexture,
            int targetColor,
            KnownItemInjection.RecolorMethod method) {}

    private record Analysis(float dominantHue, float[] sourceRgb) {}

    private record SourceImage(NativeImage image, byte[] metadata, Analysis analysis) {}
}
