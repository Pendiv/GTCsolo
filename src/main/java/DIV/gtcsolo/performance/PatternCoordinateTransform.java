package DIV.gtcsolo.performance;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Map;

/** Native GT orientation expressed with scalars; each result is still a new immutable BlockPos. */
public final class PatternCoordinateTransform {
    private static volatile boolean enabled = Boolean.parseBoolean(System.getProperty("gtcsolo.performance.coordinates", "true"));
    private PatternCoordinateTransform() {}

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static boolean supports(Direction front, Direction up) {
        return front != null && up != null && up.getAxis().isHorizontal();
    }
    public static Map<String, Object> snapshot() {
        return Map.of("enabled", enabled, "scope", "native_pattern_match_coordinate_projection");
    }

    public static BlockPos offset(RelativeDirection[] axes, int x, int y, int z,
                                  Direction front, Direction up, boolean flipped) {
        int worldX = 0, worldY = 0, worldZ = 0;
        for (int i = 0; i < 3; i++) {
            int value = i == 0 ? x : i == 1 ? y : z;
            // Assignment (rather than summation) also preserves native behavior for repeated/degenerate axes.
            switch (axes[i].getRelativeFacing(front, up, flipped)) {
                case UP -> worldY = value;
                case DOWN -> worldY = -value;
                case EAST -> worldX = value;
                case WEST -> worldX = -value;
                case SOUTH -> worldZ = value;
                case NORTH -> worldZ = -value;
            }
        }
        return new BlockPos(worldX, worldY, worldZ);
    }
}
