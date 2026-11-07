package DIV.gtcsolo.client.gleam;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.UtilityEffects;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class GleamRayRenderer {
    private static final int SCAN_BUDGET = 4096;
    private static final int EDGE_BUDGET = 131072;
    private static final int BOX_BUDGET = 128;
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(16384));
    private static final Map<Long,CachedBlock> CACHE = new LinkedHashMap<>();
    private static ClientLevel cachedLevel;
    private static GleamRayGeometry.Cursor cursor;
    private static BlockPos scanOrigin;
    private static Vec3 scanLook = Vec3.ZERO;
    private static long lastTick = Long.MIN_VALUE, finishedAt;
    private static double scanRadius;
    private static int cachedEdges;
    private static Matrix4f worldView;
    private static ClientLevel viewLevel;

    private record LitFace(GleamRayGeometry.Face face,float alpha,boolean water) {}
    private record CachedBlock(int x,int y,int z,AABB bounds,List<LitFace> faces,int edges) {}
    private GleamRayRenderer() {}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!mc.player.hasEffect(UtilityEffects.GLEAM_RAY.get()))clear();
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            var mc=Minecraft.getInstance();
            if(mc.player!=null&&mc.player.hasEffect(UtilityEffects.GLEAM_RAY.get())) {
                if(worldView==null)worldView=new Matrix4f();
                worldView.set(event.getPoseStack().last().pose());viewLevel=mc.level;
            } else {worldView=null;viewLevel=null;}
            return;
        }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||worldView==null||viewLevel!=mc.level)return;
        var effect=mc.player.getEffect(UtilityEffects.GLEAM_RAY.get());
        if(effect==null){clear();return;}
        Vec3 center=mc.player.getEyePosition(event.getPartialTick());
        double radius=GleamRayGeometry.radius(effect.getAmplifier());
        double searchRadius=Math.min(radius+2,mc.options.getEffectiveRenderDistance()*16.0+16);
        if(cachedLevel!=mc.level){clearCache();cachedLevel=mc.level;}
        if(lastTick!=mc.level.getGameTime()) {
            lastTick=mc.level.getGameTime();
            update(event,center,searchRadius,mc.player.getLookAngle());
        }
        if(CACHE.isEmpty())return;
        Vec3 camera=event.getCamera().getPosition();
        // Forge 47.4.15 supplies the projection stack at AFTER_LEVEL. Use the captured world view.
        PoseStack pose=new PoseStack();
        pose.mulPoseMatrix(worldView);
        pose.pushPose();
        try {
            VertexConsumer vertices=BUFFER.getBuffer(Lines.TYPE);
            for(CachedBlock block:CACHE.values()) {
                if(!event.getFrustum().isVisible(block.bounds)||!intersects(block.x,block.y,block.z,center,radius+1))continue;
                pose.pushPose();
                pose.translate(block.x-camera.x,block.y-camera.y,block.z-camera.z);
                try {
                for(LitFace lit:block.faces) {
                    var face=lit.face;
                    double cameraAxis=face.axis()==0?camera.x-block.x:face.axis()==1?camera.y-block.y:camera.z-block.z;
                    if(!lit.water&&(cameraAxis-face.plane())*face.sign()<=0)continue;
                    for(var line:face.lines()) {
                        var clipped=GleamRayGeometry.clip(line,center.x-block.x,center.y-block.y,center.z-block.z,radius);
                        if(clipped==null)continue;
                        double mx=(clipped.x0()+clipped.x1())*.5+block.x-center.x;
                        double my=(clipped.y0()+clipped.y1())*.5+block.y-center.y;
                        double mz=(clipped.z0()+clipped.z1())*.5+block.z-center.z;
                        float fade=(float)Math.min(1,Math.max(0,(radius-Math.sqrt(mx*mx+my*my+mz*mz))/.6));
                        float alpha=lit.alpha*fade*.85f;
                        if(alpha<.015f)continue;
                        var biased=GleamRayGeometry.offsetTowardsCamera(clipped,camera.x-block.x,camera.y-block.y,camera.z-block.z);
                        emit(vertices,pose.last(),biased,alpha,lit.water);
                    }
                }
                } finally { pose.popPose(); }
            }
            BUFFER.endBatch(Lines.TYPE);
        } finally { pose.popPose(); }
    }

    private static void update(RenderLevelStageEvent event,Vec3 center,double radius,Vec3 look) {
        Iterator<CachedBlock> entries=CACHE.values().iterator();
        while(entries.hasNext()) {
            CachedBlock block=entries.next();
            if(!intersects(block.x,block.y,block.z,center,radius)||!event.getFrustum().isVisible(block.bounds)) {
                cachedEdges-=block.edges;entries.remove();
            }
        }
        boolean moved=scanOrigin==null||Vec3.atCenterOf(scanOrigin).distanceToSqr(center)>Math.pow(Math.max(2,radius*.2),2);
        boolean turned=look.dot(scanLook)<.94;
        if(moved||turned||scanRadius!=radius||(cursor==null&&lastTick-finishedAt>=5)) {
            scanOrigin=BlockPos.containing(center);scanRadius=radius;scanLook=look;
            cursor=new GleamRayGeometry.Cursor((int)Math.ceil(radius));
        }
        if(cursor==null)return;
        var pos=new BlockPos.MutableBlockPos();
        long deadline=System.nanoTime()+2_000_000;
        for(int visited=0;visited<SCAN_BUDGET&&System.nanoTime()<deadline;visited++) {
            if(!cursor.next()){cursor=null;finishedAt=lastTick;break;}
            int x=scanOrigin.getX()+cursor.x,y=scanOrigin.getY()+cursor.y,z=scanOrigin.getZ()+cursor.z;
            if(!intersects(x,y,z,center,radius)||y<cachedLevel.getMinBuildHeight()||y>=cachedLevel.getMaxBuildHeight())continue;
            AABB bounds=new AABB(x,y,z,x+1,y+1,z+1).inflate(.5);
            if(!event.getFrustum().isVisible(bounds))continue;
            long key=BlockPos.asLong(x,y,z);
            CachedBlock old=CACHE.remove(key);if(old!=null)cachedEdges-=old.edges;
            if(cachedEdges>=EDGE_BUDGET||!cachedLevel.getChunkSource().hasChunk(x>>4,z>>4))continue;
            pos.set(x,y,z);
            List<GleamRayGeometry.Box> boxes=shape(pos);
            Map<Integer,List<GleamRayGeometry.Box>> neighbors=new HashMap<>();
            List<LitFace> faces=new ArrayList<>();int edges=0;
            int ownLight=cachedLevel.getMaxLocalRawBrightness(pos);
            for(var face:GleamRayGeometry.exposed(boxes,(dx,dy,dz)->neighbors.computeIfAbsent((dx+1)*9+(dy+1)*3+dz+1,
                    ignored->shape(new BlockPos(x+dx,y+dy,z+dz))))) {
                var direction=Direction.fromAxisAndDirection(Direction.Axis.values()[face.axis()],
                        face.sign()>0?Direction.AxisDirection.POSITIVE:Direction.AxisDirection.NEGATIVE);
                float alpha=GleamRayGeometry.alpha(Math.max(ownLight,cachedLevel.getMaxLocalRawBrightness(pos.relative(direction))));
                if(alpha>0){faces.add(new LitFace(face,alpha,false));edges+=face.lines().size();}
            }
            var state=cachedLevel.getBlockState(pos);
            var fluid=state.getFluidState();
            if(fluid.is(FluidTags.WATER)&&ownLight<=7) {
                var water=new GleamRayWaterOutline(x,y,z);
                Minecraft.getInstance().getBlockRenderer().renderLiquid(pos,cachedLevel,water,state,fluid);
                for(var face:water.faces()) {
                    var direction=Direction.fromAxisAndDirection(Direction.Axis.values()[face.axis()],
                            face.sign()>0?Direction.AxisDirection.POSITIVE:Direction.AxisDirection.NEGATIVE);
                    float alpha=GleamRayGeometry.alpha(Math.max(ownLight,cachedLevel.getMaxLocalRawBrightness(pos.relative(direction))));
                    if(alpha>0){faces.add(new LitFace(face,alpha,true));edges+=face.lines().size();}
                }
            }
            if(edges>0&&cachedEdges+edges<=EDGE_BUDGET) {
                CACHE.put(key,new CachedBlock(x,y,z,bounds,List.copyOf(faces),edges));cachedEdges+=edges;
            }
        }
    }

    private static List<GleamRayGeometry.Box> shape(BlockPos pos) {
        if(pos.getY()<cachedLevel.getMinBuildHeight()||pos.getY()>=cachedLevel.getMaxBuildHeight()
                ||!cachedLevel.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))return List.of();
        var state=cachedLevel.getBlockState(pos);
        if(state.isAir()||state.getRenderShape()==RenderShape.INVISIBLE
                ||state.getCollisionShape(cachedLevel,pos,CollisionContext.empty()).isEmpty())return List.of();
        var shape=state.getShape(cachedLevel,pos,CollisionContext.empty());
        var boxes=shape.toAabbs();
        if(boxes.size()>BOX_BUDGET)return List.of();
        List<GleamRayGeometry.Box> result=new ArrayList<>(boxes.size());
        for(AABB box:boxes)result.add(new GleamRayGeometry.Box(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ));
        return result;
    }

    private static boolean intersects(int x,int y,int z,Vec3 center,double radius) {
        double dx=Math.max(0,Math.max(x-center.x,center.x-x-1));
        double dy=Math.max(0,Math.max(y-center.y,center.y-y-1));
        double dz=Math.max(0,Math.max(z-center.z,center.z-z-1));
        return dx*dx+dy*dy+dz*dz<=radius*radius;
    }

    private static void emit(VertexConsumer out,PoseStack.Pose pose,GleamRayGeometry.Line line,float alpha,boolean water) {
        float red=water?82/255f:1, green=water?217/255f:1;
        out.vertex(pose.pose(),(float)line.x0(),(float)line.y0(),(float)line.z0())
                .color(red,green,1f,alpha).endVertex();
        out.vertex(pose.pose(),(float)line.x1(),(float)line.y1(),(float)line.z1())
                .color(red,green,1f,alpha).endVertex();
    }

    private static void clear() {
        clearCache();worldView=null;viewLevel=null;
    }

    private static void clearCache() {
        CACHE.clear();cachedEdges=0;cachedLevel=null;cursor=null;scanOrigin=null;scanLook=Vec3.ZERO;
        lastTick=Long.MIN_VALUE;finishedAt=0;scanRadius=0;
    }

    private static final class Lines extends RenderType {
        // Vanilla's expanded-line shader shrinks view-space depth; that can reveal lines behind thin walls.
        private static final RenderType TYPE=create("gtcsolo_gleam_ray",DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.DEBUG_LINES,16384,false,false,CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER).setLineState(new LineStateShard(OptionalDouble.of(1.5)))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setOutputState(MAIN_TARGET).createCompositeState(false));
        private Lines(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorted,Runnable setup,Runnable clear) {
            super(name,format,mode,size,crumbling,sorted,setup,clear);
        }
    }
}
