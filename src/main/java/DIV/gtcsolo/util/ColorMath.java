package DIV.gtcsolo.util;

/** Shared 24-bit RGB HSL lightness adjustment for both item-injection registries. */
public final class ColorMath {
    private ColorMath() {}

    public static int adjustLightness(int color, float offset) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float lightness = (max + min) * 0.5F;
        float saturation;
        float hue;

        if (max == min) {
            hue = 0.0F;
            saturation = 0.0F;
        } else {
            float delta = max - min;
            saturation = lightness > 0.5F ? delta / (2.0F - max - min) : delta / (max + min);
            if (max == r) {
                hue = ((g - b) / delta + (g < b ? 6.0F : 0.0F)) / 6.0F;
            } else if (max == g) {
                hue = ((b - r) / delta + 2.0F) / 6.0F;
            } else {
                hue = ((r - g) / delta + 4.0F) / 6.0F;
            }
        }

        lightness = clamp01(lightness + offset);
        float q = lightness < 0.5F ?
                lightness * (1.0F + saturation) :
                lightness + saturation - lightness * saturation;
        float p = 2.0F * lightness - q;
        int outR;
        int outG;
        int outB;
        if (saturation == 0.0F) {
            outR = outG = outB = Math.round(lightness * 255.0F);
        } else {
            outR = Math.round(hueToRgb(p, q, hue + 1.0F / 3.0F) * 255.0F);
            outG = Math.round(hueToRgb(p, q, hue) * 255.0F);
            outB = Math.round(hueToRgb(p, q, hue - 1.0F / 3.0F) * 255.0F);
        }
        return (outR << 16) | (outG << 8) | outB;
    }

    private static float hueToRgb(float p, float q, float hue) {
        if (hue < 0.0F) hue += 1.0F;
        if (hue > 1.0F) hue -= 1.0F;
        if (hue < 1.0F / 6.0F) return p + (q - p) * 6.0F * hue;
        if (hue < 1.0F / 2.0F) return q;
        if (hue < 2.0F / 3.0F) return p + (q - p) * (2.0F / 3.0F - hue) * 6.0F;
        return p;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

}
