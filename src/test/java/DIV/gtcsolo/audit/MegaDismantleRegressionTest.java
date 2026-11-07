package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.mega.dismantle.DismantleJob;
import DIV.gtcsolo.machine.mega.dismantle.DismantleShape;
import DIV.gtcsolo.machine.mega.dismantle.MegaDismantleData;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public final class MegaDismantleRegressionTest {
    public static void run() throws Exception {
        var pattern=FactoryBlockPattern.start(RelativeDirection.RIGHT,RelativeDirection.UP,RelativeDirection.BACK)
                .aisle("CS#"," A ").aisle("CCC","###")
                .where('S',Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK)))
                .where('C',Predicates.blocks(Blocks.STONE).or(Predicates.blocks(Blocks.GLASS)))
                .where('A',Predicates.air()).where('#',Predicates.any()).where(' ',Predicates.any()).build();
        var shape=new DismantleShape(pattern);
        assert shape.width==3 && shape.height==2 && shape.depth==2;
        assert shape.allowed(0).equals(Set.of(Blocks.STONE,Blocks.GLASS));
        assert shape.allowed(1).equals(Set.of(Blocks.DIAMOND_BLOCK));
        assert shape.allowed(2).isEmpty() && shape.allowed(3).isEmpty() && shape.allowed(4).isEmpty();
        assert DismantleShape.candidateBlocks(Predicates.any().or(Predicates.blocks(Blocks.GOLD_BLOCK))).isEmpty();
        var offset=BlockPattern.class.getDeclaredMethod("setActualRelativeOffset",int.class,int.class,int.class,Direction.class,Direction.class,boolean.class);
        offset.setAccessible(true);var core=new BlockPos(243,70,-811);
        for(var front:Direction.values()) for(var up:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST})
            for(boolean flipped:new boolean[]{false,true}) for(int index=0;index<shape.volume();index++) {
                int x=index%3,y=index/3%2,z=index/6;
                var expected=core.offset((BlockPos)offset.invoke(pattern,x-1,y,z,front,up,flipped));
                assert shape.position(index,core,front,up,flipped).equals(expected);
            }

        long[] positions={10,11,12,13,14};int[] materials={0,1,2,1,1};
        var job=new DismantleJob(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",10,positions,materials,
                List.of("minecraft:diamond_block","minecraft:stone","minecraft:chest"));
        var world=new HashMap<Long,String>();
        world.put(10L,"minecraft:diamond_block");world.put(11L,"minecraft:stone");world.put(12L,"minecraft:chest");
        world.put(13L,"minecraft:stone");world.put(14L,"minecraft:beacon");world.put(99L,"minecraft:gold_block");
        boolean[] loaded={false};var removed=new ArrayList<Long>();
        DismantleJob.Access access=new DismantleJob.Access() {
            public boolean loaded(long p) { return p!=12 || loaded[0]; }
            public String block(long p) { return world.getOrDefault(p,"minecraft:air"); }
            public boolean permitted(long p) { return p!=13; }
            public boolean remove(long p) { removed.add(p);world.remove(p);return true; }
        };
        job.step(access,512,System.nanoTime()-1);assert job.cursor==0;
        job.step(access,1,Long.MAX_VALUE);assert job.cursor==1 && job.removed==1;
        job.step(access,512,Long.MAX_VALUE);assert job.cursor==2 && job.removed==2;
        var saved=new MegaDismantleData();saved.jobs.add(job);
        var encoded=saved.save(new CompoundTag());
        var bytes=new java.io.ByteArrayOutputStream();net.minecraft.nbt.NbtIo.writeCompressed(encoded,bytes);
        var decoded=net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        var restored=MegaDismantleData.load(decoded).jobs.get(0);
        assert restored.cursor==2 && restored.owner.equals(job.owner) && restored.id.equals(job.id);
        world.put(10L,"minecraft:diamond_block"); // Do not revisit a position processed before saving.
        loaded[0]=true;restored.step(access,512,Long.MAX_VALUE);
        assert restored.done() && restored.removed==3 && restored.skipped==2;
        assert removed.equals(List.of(10L,11L,12L));
        assert world.get(10L).equals("minecraft:diamond_block") && world.get(13L).equals("minecraft:stone");
        assert world.get(14L).equals("minecraft:beacon") && world.get(99L).equals("minecraft:gold_block");

        var race=new DismantleJob(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",20,new long[]{20},new int[]{0},List.of("minecraft:stone"));
        String[] block={"minecraft:stone"};
        race.step(new DismantleJob.Access() {
            public boolean loaded(long p) { return true; }
            public String block(long p) { return block[0]; }
            public boolean permitted(long p) { block[0]="minecraft:beacon";return true; }
            public boolean remove(long p) { throw new AssertionError("Protection callback replacement must survive"); }
        },512,Long.MAX_VALUE);
        assert race.skipped==1;
        boolean failed=false;var corrupt=job.save();corrupt.putInt("Cursor",999);
        try { DismantleJob.load(corrupt); } catch(IllegalArgumentException expected) { failed=true; }
        assert failed;

        var inventory=new CustomItemStackHandler(1);var huge=new ItemStack(Items.DIAMOND);huge.setCount(Integer.MAX_VALUE);
        inventory.setStackInSlot(0,huge);int[] changes={0};inventory.setOnContentsChanged(()->changes[0]++);
        inventory.clear();assert inventory.getSlots()==1 && inventory.getStackInSlot(0).isEmpty() && changes[0]==1;
        System.out.println("PASS dismantling: native candidates/ANY/air exclusion, all native orientations, bounded work, unloaded pause, persistent resume, protection/replacement exclusions and constant-time local stack deletion. Live Forge break/drop events not exercised.");
    }
}
