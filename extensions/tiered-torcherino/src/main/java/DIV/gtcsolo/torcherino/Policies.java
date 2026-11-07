package DIV.gtcsolo.torcherino;

import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumMap;

public final class Policies {
    private static final EnumMap<Profile, Integer> REGISTERED = new EnumMap<>(Profile.class);
    private Policies() {}
    public static void registered(Profile profile, int power) { REGISTERED.put(profile, power); }
    public static int power(BlockState state) {
        Profile profile = Profile.from(state);
        if (profile == null) return -1;
        Integer power = REGISTERED.get(profile);
        if (power == null) throw new IllegalStateException("Tier not registered: " + profile.id());
        return power;
    }
}
