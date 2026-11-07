package DIV.gtcsolo.integration.jei.performance;

/** Startup switches are read at use time so baseline and optimized runs use the same JAR. */
public final class JeiOptimizationOptions {
    private JeiOptimizationOptions() {}

    public static boolean hyperlinkEnabled() {
        return Boolean.parseBoolean(System.getProperty("gtcsolo.optimization.hyperlink", "true"));
    }

    public static boolean introductionIndexEnabled() {
        return Boolean.parseBoolean(System.getProperty("gtcsolo.optimization.jei.introductionIndex",
                Boolean.toString(nativePreviewLazyEnabled())));
    }

    public static boolean recipeIndexEnabled() {
        return Boolean.parseBoolean(System.getProperty("gtcsolo.optimization.jei.recipeIndex", "true"));
    }

    public static boolean recipeIndexVerificationEnabled() {
        return Boolean.getBoolean("gtcsolo.optimization.jei.recipeIndex.verify");
    }

    public static boolean setterLookupEnabled() {
        return Boolean.parseBoolean(System.getProperty("gtcsolo.optimization.jei.setterLookup", "true"));
    }

    public static boolean nativePreviewLazyEnabled() {
        return Boolean.parseBoolean(System.getProperty("gtcsolo.optimization.jei.nativePreview", "true"));
    }

    public static boolean nativePreviewVerificationEnabled() {
        return Boolean.getBoolean("gtcsolo.optimization.jei.nativePreview.verify");
    }

    public static boolean timingEnabled() {
        return Boolean.getBoolean("gtcsolo.optimization.jei.timing");
    }
}
