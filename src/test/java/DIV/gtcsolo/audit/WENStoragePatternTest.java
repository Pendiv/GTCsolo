package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.wen.WENStorageStructure;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import sun.misc.Unsafe;

import java.util.*;
import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/** Native GT matching and orientation, including every small shell position and large-shell boundaries. */
public final class WENStoragePatternTest {
    public static void main(String[] args) throws Exception {
        if (!WENStoragePatternTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0 || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
        }
        var field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        TestLevel level = (TestLevel) ((Unsafe) field.get(null)).allocateInstance(TestLevel.class);
        var transform = BlockPattern.class.getDeclaredMethod("setActualRelativeOffset", int.class, int.class, int.class,
                Direction.class, Direction.class, boolean.class);
        transform.setAccessible(true);
        int formations = 0, positions = 0;
        for (int[] size : List.of(new int[]{3,3,3}, new int[]{4,5,6}, new int[]{15,15,15})) {
            int w=size[0], h=size[1], d=size[2];
            for (int x=0;x<w;x++) for(int y=0;y<h;y++) for(int z=0;z<d;z++) {
                if (x>0&&x<w-1&&y>0&&y<h-1&&z>0&&z<d-1) continue;
                if(w==15 && (!boundaryOrMiddle(x,w)||!boundaryOrMiddle(y,h)||!boundaryOrMiddle(z,d)))continue;
                var shape = new WENStorageStructure.Shape(x,w-x-1,y,h-y-1,z,d-z-1);
                var wall=blocks(Blocks.COBBLESTONE).or(blocks(Blocks.REDSTONE_BLOCK).setMinGlobalLimited(1).setMaxGlobalLimited(1));
                var pattern=WENStorageStructure.pattern(Blocks.DIAMOND_BLOCK,shape,wall,blocks(Blocks.IRON_BLOCK));
                int[] repeats=new int[d];Arrays.fill(repeats,1);
                var preview=pattern.getPreview(repeats);
                assert preview.length==w && preview[0].length==h && preview[0][0].length==d;
                for(Direction facing:Direction.values()) for(Direction upwards:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) for(boolean flipped:new boolean[]{false,true}) {
                    BlockPos core=new BlockPos(40,100,40);
                    Map<BlockPos,BlockState> blocks=new HashMap<>();
                    BlockPos monitor=null, anotherWall=null, internal=null;
                    for(int ix=0;ix<w;ix++)for(int iy=0;iy<h;iy++)for(int iz=0;iz<d;iz++) {
                        BlockPos delta=(BlockPos)transform.invoke(pattern,ix-x,iy-y,iz-z,facing,upwards,flipped);
                        BlockPos p=core.offset(delta);BlockState state=preview[ix][iy][iz].getBlockState();
                        blocks.put(p,state);
                        assert WENStorageStructure.position(core,facing,upwards,flipped,ix-x,iy-y,iz-z).equals(p);
                        assert shape.contains(core,facing,upwards,flipped,p);positions++;
                        if(state.is(Blocks.REDSTONE_BLOCK))monitor=p;
                        if(state.is(Blocks.COBBLESTONE))anotherWall=p;
                        if(state.is(Blocks.IRON_BLOCK))internal=p;
                    }
                    assert monitor!=null && anotherWall!=null && internal!=null;
                    var detected=WENStorageStructure.detect(core,facing,upwards,flipped,p->true,
                            p->blocks.getOrDefault(p,Blocks.AIR.defaultBlockState()),WENStoragePatternTest::structural);
                    assert shape.equals(detected):"wrong bounds "+shape+" / "+facing+" / "+detected;
                    assert pattern.checkPatternAt(new TestState(level,core,blocks),core,facing,upwards,flipped,false):"native match failed";
                    assert !shape.contains(core,facing,upwards,flipped,core.offset(100,100,100));
                    var denied=WENStorageStructure.detect(core,facing,upwards,flipped,p->!p.equals(core.relative(facing)),
                            p->{assert !p.equals(core.relative(facing)):"read unloaded position";return blocks.getOrDefault(p,Blocks.AIR.defaultBlockState());},WENStoragePatternTest::structural);
                    assert denied==null:"unloaded ray accepted";
                    blocks.put(monitor,Blocks.COBBLESTONE.defaultBlockState());
                    assert !pattern.checkPatternAt(new TestState(level,core,blocks),core,facing,upwards,flipped,false):"missing monitor accepted";
                    blocks.put(monitor,Blocks.REDSTONE_BLOCK.defaultBlockState());
                    blocks.put(anotherWall,Blocks.REDSTONE_BLOCK.defaultBlockState());
                    assert !pattern.checkPatternAt(new TestState(level,core,blocks),core,facing,upwards,flipped,false):"second monitor accepted";
                    blocks.put(anotherWall,Blocks.DIAMOND_BLOCK.defaultBlockState());
                    assert !pattern.checkPatternAt(new TestState(level,core,blocks),core,facing,upwards,flipped,false):"second core accepted";
                    blocks.put(anotherWall,Blocks.COBBLESTONE.defaultBlockState());
                    blocks.put(internal,Blocks.AIR.defaultBlockState());
                    assert !pattern.checkPatternAt(new TestState(level,core,blocks),core,facing,upwards,flipped,false):"empty cell accepted";
                    formations++;
                }
            }
        }
        assert !new WENStorageStructure.Shape(1,1,1,1,1,1).valid():"interior controller accepted";
        assert !new WENStorageStructure.Shape(0,15,0,2,0,2).valid():"oversize accepted";
        assert !new WENStorageStructure.Shape(0,1,0,2,0,2).valid():"undersize accepted";
        int[] reads={0};
        var endless=WENStorageStructure.detect(BlockPos.ZERO,Direction.NORTH,Direction.NORTH,p->true,
                p->{reads[0]++;return Blocks.COBBLESTONE.defaultBlockState();},WENStoragePatternTest::structural);
        assert endless==null && reads[0]<=90:"unbounded search";
        System.out.println("WEN_PATTERN_PASS formations="+formations+" coordinates="+positions+" sixFacings=true fourRolls=true mirrored=true shellAnywhere=true monitorExactlyOne=true duplicateCoreRejected=true boundedUnloadedSafe=true");
    }
    private static boolean boundaryOrMiddle(int i,int size){return i==0||i==size/2||i==size-1;}
    private static boolean structural(BlockState state){return state.is(Blocks.COBBLESTONE)||state.is(Blocks.REDSTONE_BLOCK)||state.is(Blocks.IRON_BLOCK);}
    private static final class TestLevel extends ServerLevel {
        private TestLevel(){super(null,null,null,null,null,null,null,false,0L,List.of(),false,null);}
        @Override public int getMinBuildHeight(){return -64;}
        @Override public int getMaxBuildHeight(){return 320;}
    }
    private static final class TestState extends MultiblockState {
        private final Map<BlockPos,BlockState> blocks;private BlockPos current;
        TestState(TestLevel level,BlockPos core,Map<BlockPos,BlockState> blocks){super(level,core);this.blocks=blocks;}
        @Override protected boolean update(BlockPos p,TraceabilityPredicate predicate){current=p;this.predicate=predicate;error=null;return p.getY()>=-64&&p.getY()<320;}
        @Override public BlockPos getPos(){return current;}
        @Override public BlockState getBlockState(){return blocks.getOrDefault(current,Blocks.AIR.defaultBlockState());}
        @Override public BlockEntity getTileEntity(){return null;}
    }
}
