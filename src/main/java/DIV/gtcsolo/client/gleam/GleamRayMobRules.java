package DIV.gtcsolo.client.gleam;

public final class GleamRayMobRules {
    private GleamRayMobRules() {}

    public static boolean inRange(int amplifier, int feetLight, int eyeLight,
                                  double x, double y, double z,
                                  double x0, double y0, double z0, double x1, double y1, double z1) {
        if (amplifier < 0 || Math.max(feetLight, eyeLight) > 7) return false;
        double dx = Math.max(0, Math.max(x0-x, x-x1));
        double dy = Math.max(0, Math.max(y0-y, y-y1));
        double dz = Math.max(0, Math.max(z0-z, z-z1));
        double radius = GleamRayGeometry.radius(amplifier);
        return dx*dx + dy*dy + dz*dz <= radius*radius;
    }
}
