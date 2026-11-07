package DIV.gtcsolo.client.gleam;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import DIV.gtcsolo.client.gleam.GleamRayGeometry.Face;
import DIV.gtcsolo.client.gleam.GleamRayGeometry.Line;

/** Captures the native fluid renderer's quads without submitting them to the GPU. */
public final class GleamRayWaterOutline implements VertexConsumer {
    private final int originX, originY, originZ;
    private final double[][] quad = new double[4][3];
    private final Set<List<Point>> seen = new HashSet<>();
    private final List<Face> faces = new ArrayList<>();
    private int index;

    private record Point(double x, double y, double z) {}

    public GleamRayWaterOutline(int x, int y, int z) {
        originX = x & 15;
        originY = y & 15;
        originZ = z & 15;
    }

    public List<Face> faces() { return List.copyOf(faces); }

    @Override public VertexConsumer vertex(double x, double y, double z) {
        quad[index][0] = x - originX;
        quad[index][1] = y - originY;
        quad[index][2] = z - originZ;
        return this;
    }

    @Override public void endVertex() {
        if (++index != 4) return;
        index = 0;
        List<Point> key = new ArrayList<>(4);
        for (double[] p : quad) key.add(new Point(p[0], p[1], p[2]));
        key.sort(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y).thenComparingDouble(Point::z));
        if (!seen.add(List.copyOf(key))) return; // Native water can emit both windings of the same face.

        double ax = quad[1][0] - quad[0][0], ay = quad[1][1] - quad[0][1], az = quad[1][2] - quad[0][2];
        double bx = quad[2][0] - quad[0][0], by = quad[2][1] - quad[0][1], bz = quad[2][2] - quad[0][2];
        double[] normal = {ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
        int axis = Math.abs(normal[0]) > Math.abs(normal[1]) ? 0 : 1;
        if (Math.abs(normal[2]) > Math.abs(normal[axis])) axis = 2;
        if (Math.abs(normal[axis]) < 1e-10) return;
        // A sloping top still takes its light from above, not from its steeper horizontal axis.
        if (quad[0][0] != quad[1][0] || quad[0][0] != quad[2][0]) {
            if (quad[0][2] != quad[1][2] || quad[0][2] != quad[2][2]) axis = 1;
        }
        List<Line> lines = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            double[] a = quad[i], b = quad[(i + 1) % 4];
            lines.add(new Line(a[0], a[1], a[2], b[0], b[1], b[2]));
        }
        faces.add(new Face(axis, normal[axis] >= 0 ? 1 : -1,
                (quad[0][axis] + quad[1][axis] + quad[2][axis] + quad[3][axis]) / 4,
                List.copyOf(lines)));
    }

    @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer uv(float u, float v) { return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
    @Override public VertexConsumer uv2(int u, int v) { return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    @Override public void defaultColor(int r, int g, int b, int a) {}
    @Override public void unsetDefaultColor() {}
}
