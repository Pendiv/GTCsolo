package DIV.gtcsolo.torcherino;

/** Independent allocation policy. Ranges are radii; visible widths are 2*r+1. */
public final class Budget {
    public static final int MAX_XZ_RADIUS = 4;
    public static final int MAX_Y_RADIUS = 1;
    private Budget() {}
    public record State(int x, int y, int z, int speed) {
        public long spent() { return (long) x + y + z + speed - 1L; }
    }
    public static boolean valid(State s, int power) {
        return power >= 0 && s.x >= 0 && s.x <= MAX_XZ_RADIUS && s.y >= 0 && s.y <= MAX_Y_RADIUS
                && s.z >= 0 && s.z <= MAX_XZ_RADIUS && s.speed >= 1 && s.spent() <= power;
    }
    private static int bounded(int value, int max) { return Math.max(0, Math.min(value, max)); }
    public static State normalize(State s, int power) {
        if (power < 0) throw new IllegalArgumentException("Negative power");
        int speed = 1 + bounded(s.speed <= 1 ? 0 : s.speed - 1, power);
        int left = power - speed + 1;
        int x = bounded(s.x, Math.min(left, MAX_XZ_RADIUS)); left -= x;
        int y = bounded(s.y, Math.min(left, MAX_Y_RADIUS)); left -= y;
        return new State(x, y, bounded(s.z, Math.min(left, MAX_XZ_RADIUS)), speed);
    }
    /** Edit one slider without taking points away from any other slider. */
    public static State edit(State old, State requested, int field, int power) {
        if (!valid(old, power)) old = normalize(old, power);
        return switch (field) {
            case 0 -> new State(old.x, old.y, old.z, 1 + bounded(requested.speed <= 1 ? 0 : requested.speed - 1,
                    power - old.x - old.y - old.z));
            case 1 -> new State(bounded(requested.x, Math.min(MAX_XZ_RADIUS, power - old.y - old.z - old.speed + 1)), old.y, old.z, old.speed);
            case 2 -> new State(old.x, old.y, bounded(requested.z, Math.min(MAX_XZ_RADIUS, power - old.x - old.y - old.speed + 1)), old.speed);
            case 3 -> new State(old.x, bounded(requested.y, Math.min(MAX_Y_RADIUS, power - old.x - old.z - old.speed + 1)), old.z, old.speed);
            default -> throw new IllegalArgumentException("Unknown slider " + field);
        };
    }
}
