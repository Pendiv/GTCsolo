package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.water.WaterServerMachine;
import DIV.gtcsolo.machine.water.WaterServerStructure;
import DIV.gtcsolo.machine.water.WaterServerInteractions;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import sun.misc.Unsafe;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Actual GT matching, coordinate conversion and reservoir APIs; no running world or player UI. */
public final class WaterServerRegressionTest {
    public static void main(String[] args) throws Exception {
        if (!WaterServerRegressionTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0 || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
        }
        var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var level = (TestLevel) ((Unsafe) unsafeField.get(null)).allocateInstance(TestLevel.class);
        var positions = WaterServerStructure.CORE_POSITIONS;
        assert positions.size() == 21;
        BlockPattern[] patterns = new BlockPattern[positions.size()];
        for (int i = 0; i < patterns.length; i++) patterns[i] = WaterServerStructure.pattern(
                Blocks.DIAMOND_BLOCK, Blocks.OAK_PLANKS, Blocks.IRON_BLOCK, i);
        var transform = BlockPattern.class.getDeclaredMethod("setActualRelativeOffset", int.class, int.class, int.class,
                Direction.class, Direction.class, boolean.class);
        transform.setAccessible(true);
        int formations = 0, membershipChecks = 0;
        for (int index = 0; index < patterns.length; index++) {
            var pattern = patterns[index];
            var localCore = positions.get(index);
            var preview = pattern.getPreview(new int[]{1, 1, 1});
            assert preview.length == 3 && preview[0].length == 4 && preview[0][0].length == 3;
            assert preview[localCore.getX()][localCore.getY()][localCore.getZ()].getBlockState().is(Blocks.DIAMOND_BLOCK);
            for (Direction facing : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                for (int minimum : new int[]{-64, 0}) {
                    level.minimum = minimum;
                    BlockPos core = new BlockPos(30, minimum + localCore.getY(), -14);
                    var blocks = new HashMap<BlockPos, BlockState>();
                    BlockPos anotherWall = null;
                    int walls = 0, steel = 0, glass = 0;
                    for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 3; z++) {
                        var state = preview[x][y][z].getBlockState();
                        BlockPos delta = (BlockPos) transform.invoke(pattern, x-localCore.getX(), y-localCore.getY(),
                                z-localCore.getZ(), facing, Direction.UP, false);
                        BlockPos pos = core.offset(delta);
                        blocks.put(pos, state);
                        boolean woodPosition = state.is(Blocks.OAK_PLANKS) || state.is(Blocks.DIAMOND_BLOCK);
                        assert WaterServerStructure.containsWall(core, facing, index, pos) == woodPosition;
                        membershipChecks++;
                        if (state.is(Blocks.OAK_PLANKS)) { walls++; anotherWall = pos; }
                        if (state.is(Blocks.IRON_BLOCK)) steel++;
                        if (state.is(Blocks.GLASS)) glass++;
                    }
                    assert walls == 20 && steel == 4 && glass == 9;
                    assert !WaterServerStructure.containsWall(core, facing, index, core.offset(10,0,0));
                    int matches = 0;
                    for (int candidate = 0; candidate < patterns.length; candidate++) {
                        if (patterns[candidate].checkPatternAt(new TestState(level, core, blocks), core, facing, Direction.UP, false, false)) {
                            assert candidate == index; matches++;
                        }
                    }
                    assert matches == 1 : "ambiguous/missing actual controller position";
                    formations++;
                    blocks.put(anotherWall, Blocks.DIAMOND_BLOCK.defaultBlockState());
                    for (BlockPattern candidate : patterns)
                        assert !candidate.checkPatternAt(new TestState(level,core,blocks),core,facing,Direction.UP,false,false)
                                : "two controllers accepted in one structure";
                }
            }
        }

        IMachineBlockEntity holder = (IMachineBlockEntity) Proxy.newProxyInstance(IMachineBlockEntity.class.getClassLoader(),
                new Class[]{IMachineBlockEntity.class}, (proxy, method, values) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                });
        var tank = new NotifiableFluidTank(new MetaMachine(holder), 1, WaterServerMachine.CAPACITY, IO.OUT)
                .setFilter(stack -> stack.getFluid() == Fluids.WATER);
        assert tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0;
        var storage = tank.getStorages()[0];
        assert storage.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0;
        for (int tick = 0; tick < 400; tick++) assert WaterServerMachine.produce(storage, true) == 500;
        assert tank.getFluidInTank(0).getAmount() == 200_000;
        assert WaterServerMachine.produce(storage, true) == 0;
        assert tank.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 1000;
        assert tank.getFluidInTank(0).getAmount() == 200_000;
        assert tank.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000;
        assert WaterServerMachine.produce(storage, false) == 0;
        assert tank.getFluidInTank(0).getAmount() == 199_000;
        assert WaterServerMachine.produce(storage, true) == 500;
        assert WaterServerMachine.produce(storage, true) == 500;
        for (int bottle = 0; bottle < 4; bottle++) assert tank.drain(250, IFluidHandler.FluidAction.EXECUTE).getAmount() == 250;
        assert tank.getFluidInTank(0).getAmount() == 199_000;
        var bucket = new ItemStack(Items.BUCKET);
        // Exercise native container handlers directly. Capability-token transformation/attachment
        // requires a Forge launch and is deliberately outside this standalone fixture.
        var bucketHandler = new FluidBucketWrapper(bucket);
        assert FluidUtil.tryFluidTransfer(bucketHandler, tank, Integer.MAX_VALUE, true).getAmount() == 1000;
        assert bucketHandler.getContainer().is(Items.WATER_BUCKET);
        assert tank.getFluidInTank(0).getAmount() == 198_000;
        assert bucketHandler.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.SIMULATE)==0;
        assert new FluidBucketWrapper(new ItemStack(Items.LAVA_BUCKET))
                .fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.SIMULATE)==0;
        var cell = new FluidHandlerItemStack(new ItemStack(Items.PAPER), 8000);
        assert FluidUtil.tryFluidTransfer(cell, tank, Integer.MAX_VALUE, true).getAmount() == 8000;
        assert cell.getFluidInTank(0).getAmount() == 8000 && tank.getFluidInTank(0).getAmount() == 190_000;
        assert FluidUtil.tryFluidTransfer(cell, tank, Integer.MAX_VALUE, true).isEmpty();
        assert tank.getFluidInTank(0).getAmount() == 190_000;
        var bottle = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        assert WaterServerInteractions.acceptsWater(new ItemStack(Items.GLASS_BOTTLE));
        assert com.gregtechceu.gtceu.common.fluid.potion.PotionFluidHelper.getFluidFromPotionItem(bottle, 250).getAmount() == 250;
        System.out.println("PASS: " + formations + " native formations, duplicate-core rejection, " + membershipChecks
                + " wall coordinates; 500 mB/t, 200000 mB cap, output-only/water-only storage, bucket/cell/bottle transfers.");
    }

    private static final class TestLevel extends ServerLevel {
        int minimum;
        private TestLevel() { super(null,null,null,null,null,null,null,false,0L,List.of(),false,null); }
        @Override public int getMinBuildHeight() { return minimum; }
        @Override public int getMaxBuildHeight() { return minimum + 384; }
    }
    private static final class TestState extends MultiblockState {
        private final Map<BlockPos,BlockState> blocks;
        private BlockPos current;
        TestState(TestLevel level, BlockPos core, Map<BlockPos,BlockState> blocks) { super(level,core); this.blocks=blocks; }
        @Override protected boolean update(BlockPos position, TraceabilityPredicate predicate) {
            current=position; this.predicate=predicate; error=null;
            return position.getY()>=world.getMinBuildHeight() && position.getY()<world.getMaxBuildHeight();
        }
        @Override public BlockPos getPos() { return current; }
        @Override public BlockState getBlockState() { return blocks.getOrDefault(current,Blocks.AIR.defaultBlockState()); }
        @Override public BlockEntity getTileEntity() { return null; }
    }
}
