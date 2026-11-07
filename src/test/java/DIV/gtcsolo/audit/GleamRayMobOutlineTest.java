package DIV.gtcsolo.audit;

import DIV.gtcsolo.client.gleam.GleamRayMobMask;
import DIV.gtcsolo.client.gleam.GleamRayMobRules;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;

public final class GleamRayMobOutlineTest {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static boolean target(int amplifier, int feet, int eyes, double x, double z) {
        return GleamRayMobRules.inRange(amplifier, feet, eyes, 0, 1.6, 0, x, 0, z, x+1, 2, z+1);
    }

    public static void main(String[] args) {
        check(target(4,7,7,9,0), "Level V includes distance 9 and light 7");
        check(!target(4,7,7,9.01,0), "Outside range");
        check(!target(4,8,0,1,0) && !target(4,0,8,1,0), "Bright feet or head excludes mob");
        check(!target(-1,0,0,0,0), "No effect");
        check(!target(4,0,0,8,8), "Spherical rather than cubic range");
        check(target(0,0,0,5,0) && !target(0,0,0,5.01,0), "Level I radius 5");
        check(GleamRayMobRules.inRange(0,0,0,0,0,0,4,-2,-2,15,4,4), "Large mob with nearby visible body");

        Recording mask = new Recording(), normal = new Recording();
        VertexConsumer joined = VertexMultiConsumer.create(new GleamRayMobMask(mask), normal);
        joined.vertex(1,2,3).color(12,80,210,90).uv(.25f,.75f).overlayCoords(4,5).uv2(6,7).normal(0,1,0).endVertex();
        check(mask.vertices==1 && normal.vertices==1, "One model vertex is shared by both passes");
        check(mask.x==normal.x && mask.y==normal.y && mask.z==normal.z && mask.u==normal.u && mask.v==normal.v,
                "Same animated geometry and texture coordinates");
        check(mask.r==255 && mask.g==64 && mask.b==64 && mask.a==255, "Constant red outline");
        check(normal.r==12 && normal.g==80 && normal.b==210 && normal.a==90, "Original appearance unchanged");
        check(mask.extras==0 && normal.extras==3, "Outline vertex format excludes lighting, overlay and normals");
        System.out.println("GLEAM_MOB_PASS range/light boundaries, large bounds, shared native vertices, fixed red and original appearance");
    }

    private static final class Recording implements VertexConsumer {
        double x,y,z;
        float u,v;
        int r,g,b,a,vertices,extras;
        @Override public VertexConsumer vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;this.a=a;return this;}
        @Override public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){extras++;return this;}
        @Override public VertexConsumer uv2(int u,int v){extras++;return this;}
        @Override public VertexConsumer normal(float x,float y,float z){extras++;return this;}
        @Override public void endVertex(){vertices++;}
        @Override public void defaultColor(int r,int g,int b,int a){}
        @Override public void unsetDefaultColor(){}
    }
}
