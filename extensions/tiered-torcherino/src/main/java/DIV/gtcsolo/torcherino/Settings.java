package DIV.gtcsolo.torcherino;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.EnumMap;

public final class Settings {
    public static final ForgeConfigSpec SPEC;
    private static final EnumMap<Profile, ForgeConfigSpec.IntValue> POWERS = new EnumMap<>(Profile.class);
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Acceleration points per GT tier. Restart required. One point: +100% extra ticks or +1 radius on one axis.").push("power");
        for (Profile profile : Profile.values()) POWERS.put(profile,
                builder.defineInRange(profile.label, profile.defaultPower, 1, 1024));
        builder.pop(); SPEC = builder.build();
    }
    private Settings() {}
    public static int power(Profile profile) { return POWERS.get(profile).get(); }
}
