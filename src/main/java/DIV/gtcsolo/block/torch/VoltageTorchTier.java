package DIV.gtcsolo.block.torch;

/** The LV..MAX order is the GTCEu 1.6.4 GTValues.VN order, without ULV. */
public enum VoltageTorchTier {
    LV(1, "lv", "LV"),
    MV(2, "mv", "MV"),
    HV(3, "hv", "HV"),
    EV(4, "ev", "EV"),
    IV(5, "iv", "IV"),
    LUV(6, "luv", "LuV"),
    ZPM(7, "zpm", "ZPM"),
    UV(8, "uv", "UV"),
    UHV(9, "uhv", "UHV"),
    UEV(10, "uev", "UEV"),
    UIV(11, "uiv", "UIV"),
    UXV(12, "uxv", "UXV"),
    OPV(13, "opv", "OpV"),
    MAX(14, "max", "MAX");

    public static final int LIGHT_LEVEL = 14;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 14;

    private final int level;
    private final String id;
    private final String displayName;

    VoltageTorchTier(int level, String id, String displayName) {
        this.level = level;
        this.id = id;
        this.displayName = displayName;
    }

    public int level() {
        return level;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int radius() {
        return radiusForLevel(level);
    }

    public static int radiusForLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Voltage torch level must be 1..14: " + level);
        }
        return LIGHT_LEVEL + 2 * level * level + 1;
    }
}
