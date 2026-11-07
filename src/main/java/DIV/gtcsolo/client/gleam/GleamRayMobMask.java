package DIV.gtcsolo.client.gleam;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Sends only position, fixed color and texture coordinates to the native outline shader. */
public final class GleamRayMobMask implements VertexConsumer {
    private final VertexConsumer output;
    private double x, y, z;
    private float u, v;

    public GleamRayMobMask(VertexConsumer output) { this.output = output; }
    @Override public VertexConsumer vertex(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z; return this;
    }
    @Override public VertexConsumer uv(float u, float v) { this.u = u; this.v = v; return this; }
    @Override public void endVertex() { output.vertex(x, y, z).color(255, 64, 64, 255).uv(u, v).endVertex(); }
    @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
    @Override public VertexConsumer uv2(int u, int v) { return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    @Override public void defaultColor(int r, int g, int b, int a) {}
    @Override public void unsetDefaultColor() {}
}
