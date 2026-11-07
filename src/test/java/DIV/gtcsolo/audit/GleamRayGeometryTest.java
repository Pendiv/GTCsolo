package DIV.gtcsolo.audit;

import DIV.gtcsolo.client.gleam.GleamRayGeometry;
import DIV.gtcsolo.client.gleam.GleamRayGeometry.*;
import DIV.gtcsolo.client.gleam.GleamRayWaterOutline;
import java.util.*;

public final class GleamRayGeometryTest {
    private static final Box CUBE=new Box(0,0,0,1,1,1);
    private static final Neighbors AIR=(x,y,z)->List.of();
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static boolean near(double a,double b){return Math.abs(a-b)<1e-6;}

    public static void main(String[] args) {
        check(GleamRayGeometry.radius(0)==5&&GleamRayGeometry.radius(4)==9,"Effect levels are one based");
        check(GleamRayGeometry.alpha(7)>0&&GleamRayGeometry.alpha(8)==0&&GleamRayGeometry.alpha(15)==0,"Inclusive light-seven boundary");
        check(GleamRayGeometry.alpha(0)>GleamRayGeometry.alpha(7),"Darkness increases visibility");
        var isolated=GleamRayGeometry.exposed(List.of(CUBE),AIR);
        check(isolated.size()==6&&isolated.stream().allMatch(f->f.lines().size()==4),"Cube faces");
        check(GleamRayGeometry.exposed(List.of(CUBE),(x,y,z)->List.of(CUBE)).isEmpty(),"Buried cube has no outline");
        var joined=GleamRayGeometry.exposed(List.of(CUBE),(x,y,z)->x==1&&y==0&&z==0?List.of(CUBE):List.of());
        check(joined.size()==5&&joined.stream().noneMatch(f->f.axis()==0&&f.sign()==1),"Shared full face removed");
        var split=GleamRayGeometry.exposed(List.of(new Box(0,0,0,.5,1,1),new Box(.5,0,0,1,1,1)),AIR);
        check(split.size()==6&&split.stream().allMatch(f->f.lines().size()==4),"No artificial seams between component boxes");
        var partial=GleamRayGeometry.exposed(List.of(CUBE),(x,y,z)->x==1&&y==0&&z==0?List.of(new Box(0,0,0,1,.5,1)):List.of());
        var right=partial.stream().filter(f->f.axis()==0&&f.sign()==1).findFirst().orElseThrow();
        check(right.lines().size()==4&&right.lines().stream().allMatch(l->l.y0()>=.5&&l.y1()>=.5),"Neighbor slab clips only covered area");
        var stairs=GleamRayGeometry.exposed(List.of(new Box(0,0,0,1,.5,1),new Box(0,.5,.5,1,1,1)),AIR);
        var side=stairs.stream().filter(f->f.axis()==0&&f.sign()==1).findFirst().orElseThrow();
        check(side.lines().size()==6,"Stair side is one L-shaped perimeter");
        check(side.lines().stream().noneMatch(l->near(l.y0(),.5)&&near(l.y1(),.5)&&Math.min(l.z0(),l.z1())>=.5),"Stair internal join removed");
        var top=stairs.stream().filter(f->f.axis()==1&&f.sign()==1&&near(f.plane(),.5)).findFirst().orElseThrow();
        check(top.lines().stream().allMatch(l->l.z0()<=.5&&l.z1()<=.5),"Lower stair top stops at upper stair");
        var crossing=GleamRayGeometry.clip(new Line(-2,0,0,2,0,0),0,0,0,1);
        check(crossing!=null&&near(crossing.x0(),-1)&&near(crossing.x1(),1),"Sphere crossing clipped");
        check(GleamRayGeometry.clip(new Line(2,0,0,3,0,0),0,0,0,1)==null,"Out-of-range edge removed");
        var corner=new Line(0,0,0,0,1,0);
        var biased=GleamRayGeometry.offsetTowardsCamera(corner,1,.5,1);
        check(biased.x0()>0&&biased.z0()>0&&biased.x1()>0&&biased.z1()>0,"Concave edge clears both incident faces");
        for(double distance:new double[]{.0001,1,100,1_000_000}) {
            var bias=GleamRayGeometry.offsetTowardsCamera(corner,distance,0,0);
            check(bias.x0()>0&&bias.x0()<distance&&bias.x0()<=.004000001,"Depth bias bounded independently of distance");
            check(bias.x0()<.01,"A separate thin wall still occludes the biased line");
        }
        check(GleamRayGeometry.offsetTowardsCamera(corner,0,0,0).x0()==0,"Camera at endpoint stays finite");
        var water=new GleamRayWaterOutline(-17,33,31);
        double[][] slope={{15,.8+1,15},{15,.2+1,16},{16,.1+1,16},{16,.7+1,15}};
        for(double[] p:slope){water.vertex(p[0],p[1],p[2]);water.endVertex();}
        for(int i=3;i>=0;i--){water.vertex(slope[i][0],slope[i][1],slope[i][2]);water.endVertex();}
        check(water.faces().size()==1,"Native water reverse winding does not duplicate a face");
        var surface=water.faces().get(0);
        check(surface.axis()==1&&surface.sign()==1&&surface.lines().size()==4,"Native water quad perimeter without diagonal");
        var edge=surface.lines().get(0);
        check(near(edge.x0(),0)&&near(edge.z0(),0)&&near(edge.y0(),.8)&&near(edge.y1(),.2),"Chunk-local native vertices retain flowing water height");
        check(new GleamRayWaterOutline(0,0,0).faces().isEmpty(),"Unemitted buried water faces remain absent");
        for(int radius=0;radius<=12;radius++) {
            var cursor=new Cursor(radius);Set<String> seen=new HashSet<>();int last=0;
            while(cursor.next()) {
                int shell=Math.max(Math.abs(cursor.x),Math.max(Math.abs(cursor.y),Math.abs(cursor.z)));
                check(shell>=last&&shell<=radius,"Near-first scan order");last=shell;
                check(seen.add(cursor.x+","+cursor.y+","+cursor.z),"No duplicate scan positions");
            }
            check(seen.size()==(2*radius+1)*(2*radius+1)*(2*radius+1),"Complete bounded scan");
        }
        System.out.println("GLEAM_GEOMETRY_PASS light=7/8 level/range buried/shared/partial/stair faces sphere clipping bounded scan camera bias native water quads");
    }
}
