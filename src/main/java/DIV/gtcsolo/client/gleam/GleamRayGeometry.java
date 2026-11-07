package DIV.gtcsolo.client.gleam;

import java.util.*;

/** Rectangular voxel surfaces, with shared and partially covered faces removed. */
public final class GleamRayGeometry {
    private static final double EPSILON = 1.0e-7;
    private static final int MAX_FRAGMENTS = 512;
    private GleamRayGeometry() {}

    public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {
        public double min(int axis) { return axis == 0 ? x0 : axis == 1 ? y0 : z0; }
        public double max(int axis) { return axis == 0 ? x1 : axis == 1 ? y1 : z1; }
        public Box move(int x,int y,int z) { return new Box(x0+x,y0+y,z0+z,x1+x,y1+y,z1+z); }
        boolean extendsOutside() { return x0<0||y0<0||z0<0||x1>1||y1>1||z1>1; }
    }
    public record Line(double x0,double y0,double z0,double x1,double y1,double z1) {}
    public record Face(int axis,int sign,double plane,List<Line> lines) {}
    public interface Neighbors { List<Box> at(int x,int y,int z); }
    private record Rect(double u0,double v0,double u1,double v1) {}
    private record Plane(int axis,int sign,double position) {}
    private record Edge(int direction,double fixed) {}

    public static double radius(int amplifier) { return 5.0 + Math.max(0L, (long) amplifier); }

    public static float alpha(int light) { return light >= 8 ? 0 : Math.min(1, (8-light)/8.0f); }

    /** A bounded camera-facing bias clears both faces of an inside corner. */
    public static Line offsetTowardsCamera(Line line, double x, double y, double z) {
        double a = biasFraction(x-line.x0, y-line.y0, z-line.z0);
        double b = biasFraction(x-line.x1, y-line.y1, z-line.z1);
        return new Line(line.x0+(x-line.x0)*a, line.y0+(y-line.y0)*a, line.z0+(z-line.z0)*a,
                line.x1+(x-line.x1)*b, line.y1+(y-line.y1)*b, line.z1+(z-line.z1)*b);
    }

    private static double biasFraction(double dx, double dy, double dz) {
        double distance = Math.sqrt(dx*dx+dy*dy+dz*dz);
        return distance < EPSILON ? 0 : Math.min(.01, .004/distance);
    }

    public static List<Face> exposed(List<Box> boxes, Neighbors neighbors) {
        Map<Plane,List<Rect>> planes = new LinkedHashMap<>();
        boolean extended = boxes.stream().anyMatch(Box::extendsOutside);
        for (Box box : boxes) for (int axis=0;axis<3;axis++) for (int sign : new int[]{-1,1}) {
            double plane=sign<0?box.min(axis):box.max(axis);
            int u=(axis+1)%3,v=(axis+2)%3;
            List<Rect> fragments=new ArrayList<>();
            fragments.add(new Rect(box.min(u),box.min(v),box.max(u),box.max(v)));
            for(Box other:boxes) if(other!=box) subtractMask(fragments,other,axis,sign,plane);
            if(extended) {
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
                    if(x==0&&y==0&&z==0)continue;
                    for(Box other:neighbors.at(x,y,z))subtractMask(fragments,other.move(x,y,z),axis,sign,plane);
                }
            } else if(Math.abs(plane-(sign<0?0:1))<EPSILON) {
                int x=axis==0?sign:0,y=axis==1?sign:0,z=axis==2?sign:0;
                for(Box other:neighbors.at(x,y,z))subtractMask(fragments,other.move(x,y,z),axis,sign,plane);
            }
            if(!fragments.isEmpty())planes.computeIfAbsent(new Plane(axis,sign,plane),key->new ArrayList<>()).addAll(fragments);
        }
        List<Face> result=new ArrayList<>();
        for(var entry:planes.entrySet()) {
            Plane plane=entry.getKey();
            List<Line> lines=boundary(entry.getValue(),plane);
            if(!lines.isEmpty())result.add(new Face(plane.axis,plane.sign,plane.position,List.copyOf(lines)));
        }
        return List.copyOf(result);
    }

    private static void subtractMask(List<Rect> fragments,Box mask,int axis,int sign,double plane) {
        if(fragments.isEmpty())return;
        boolean covers=sign>0?mask.min(axis)<=plane+EPSILON&&mask.max(axis)>plane+EPSILON
                :mask.max(axis)>=plane-EPSILON&&mask.min(axis)<plane-EPSILON;
        if(!covers)return;
        int u=(axis+1)%3,v=(axis+2)%3;
        List<Rect> next=new ArrayList<>();
        for(Rect face:fragments) {
            double a=Math.max(face.u0,mask.min(u)),b=Math.min(face.u1,mask.max(u));
            double c=Math.max(face.v0,mask.min(v)),d=Math.min(face.v1,mask.max(v));
            if(b-a<=EPSILON||d-c<=EPSILON){next.add(face);continue;}
            add(next,face.u0,face.v0,a,face.v1);add(next,b,face.v0,face.u1,face.v1);
            add(next,a,face.v0,b,c);add(next,a,d,b,face.v1);
        }
        fragments.clear();
        if(next.size()<=MAX_FRAGMENTS)fragments.addAll(next);
    }

    private static void add(List<Rect> out,double a,double b,double c,double d) {
        if(c-a>EPSILON&&d-b>EPSILON)out.add(new Rect(a,b,c,d));
    }

    private static List<Line> boundary(List<Rect> faces,Plane plane) {
        Map<Edge,TreeMap<Double,Integer>> groups=new LinkedHashMap<>();
        for(Rect r:faces) {
            edge(groups,0,r.v0,r.u0,r.u1,1);edge(groups,0,r.v1,r.u0,r.u1,-1);
            edge(groups,1,r.u0,r.v0,r.v1,-1);edge(groups,1,r.u1,r.v0,r.v1,1);
        }
        List<Line> result=new ArrayList<>();
        for(var entry:groups.entrySet()) {
            int winding=0;double start=0;
            for(var point:entry.getValue().entrySet()) {
                int next=winding+point.getValue();
                if(winding==0&&next!=0)start=point.getKey();
                if(winding!=0&&next==0&&point.getKey()-start>EPSILON) {
                    Edge e=entry.getKey();
                    double[] a=position(plane,e.direction==0?start:e.fixed,e.direction==0?e.fixed:start);
                    double[] b=position(plane,e.direction==0?point.getKey():e.fixed,e.direction==0?e.fixed:point.getKey());
                    result.add(new Line(a[0],a[1],a[2],b[0],b[1],b[2]));
                }
                winding=next;
            }
        }
        return result;
    }

    private static void edge(Map<Edge,TreeMap<Double,Integer>> groups,int direction,double fixed,double a,double b,int sign) {
        var points=groups.computeIfAbsent(new Edge(direction,quantize(fixed)),key->new TreeMap<>());
        points.merge(quantize(a),sign,Integer::sum);points.merge(quantize(b),-sign,Integer::sum);
    }
    private static double quantize(double x) { return Math.rint(x*1_000_000)/1_000_000; }
    private static double[] position(Plane plane,double u,double v) {
        double[] xyz=new double[3];xyz[plane.axis]=plane.position;xyz[(plane.axis+1)%3]=u;xyz[(plane.axis+2)%3]=v;return xyz;
    }

    /** Clip a line to the actual spherical range, including lines crossing it from outside. */
    public static Line clip(Line line,double cx,double cy,double cz,double radius) {
        double dx=line.x1-line.x0,dy=line.y1-line.y0,dz=line.z1-line.z0;
        double x=line.x0-cx,y=line.y0-cy,z=line.z0-cz;
        double a=dx*dx+dy*dy+dz*dz;
        if(a<EPSILON)return null;
        double b=2*(x*dx+y*dy+z*dz),c=x*x+y*y+z*z-radius*radius;
        double discriminant=b*b-4*a*c;
        if(discriminant<0)return null;
        double root=Math.sqrt(discriminant),lo=Math.max(0,(-b-root)/(2*a)),hi=Math.min(1,(-b+root)/(2*a));
        if(hi-lo<=EPSILON)return null;
        return new Line(line.x0+dx*lo,line.y0+dy*lo,line.z0+dz*lo,line.x0+dx*hi,line.y0+dy*hi,line.z0+dz*hi);
    }

    /** Nearest shells first, without allocating a radius-cubed list. */
    public static final class Cursor {
        private final int radius;
        private int shell,index;
        public int x,y,z;
        public Cursor(int radius){this.radius=Math.max(0,radius);}
        public boolean next() {
            if(shell>radius)return false;
            if(shell==0){x=y=z=0;shell=1;return true;}
            int width=2*shell+1,inner=width-2,plane=width*width,band=inner*width;
            int n=index;
            if(n<2*plane){x=n<plane?-shell:shell;n%=plane;y=n/width-shell;z=n%width-shell;}
            else if((n-=2*plane)<2*band){y=n<band?-shell:shell;n%=band;x=n/width-shell+1;z=n%width-shell;}
            else{n-=2*band;z=n<inner*inner?-shell:shell;n%=inner*inner;x=n/inner-shell+1;y=n%inner-shell+1;}
            if(++index>=24*shell*shell+2){shell++;index=0;}
            return true;
        }
    }
}
