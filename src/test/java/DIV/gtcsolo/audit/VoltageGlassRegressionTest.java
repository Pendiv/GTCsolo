package DIV.gtcsolo.audit;

import DIV.gtcsolo.block.VoltageGlassBlock;
import DIV.gtcsolo.registry.VoltageGlass;
import com.gregtechceu.gtceu.api.GTValues;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Actual glass states; no game world, resource reload, or Forge registration is simulated. */
public final class VoltageGlassRegressionTest {
    public static void main(String[] args) throws Throwable {
        var stderr = System.err;
        try { runChecks(); }
        catch (Throwable failure) {
            // Bootstrap redirects stderr through Minecraft logging; keep failures visible in this standalone test.
            failure.printStackTrace(stderr);
            throw failure;
        }
    }

    private static void runChecks() {
        SharedConstants.tryDetectVersion();
        try { Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        ((MappedRegistry<?>) BuiltInRegistries.BLOCK).unfreeze();
        var glasses = new VoltageGlassBlock[GTValues.OpV + 1];
        for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {
            var glass = new VoltageGlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS), tier);
            glasses[tier] = glass;
            glass.getStateDefinition().getPossibleStates().forEach(state -> state.initCache());
            var state = glass.defaultBlockState();
            assert glass.getTier() == tier && VoltageGlassBlock.tierOf(state) == tier;
            assert !state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL;
            assert state.getLightEmission() == 0 && !state.canOcclude();
            assert state.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == 0;
            assert state.propagatesSkylightDown(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assert !state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assert !state.isRedstoneConductor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assert !state.isSuffocating(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assert !state.isViewBlocking(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            for (Direction face : Direction.values()) {
                assert glass.skipRendering(state, state, face) : "same-grade internal face must be hidden";
                assert !glass.skipRendering(state, Blocks.GLASS.defaultBlockState(), face) : "ordinary glass boundary must remain";
            }
            assert VoltageGlass.ENTRIES.get(tier - 1).tier() == tier;
            assert VoltageGlass.ENTRIES.get(tier - 1).block().getId().getPath().equals(
                    "voltage_glass_" + GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT));
        }
        for (int a = GTValues.LV; a <= GTValues.OpV; a++) {
            for (int b = GTValues.LV; b <= GTValues.OpV; b++) {
                assert glasses[a].skipRendering(glasses[a].defaultBlockState(), glasses[b].defaultBlockState(), Direction.NORTH)
                        == (a == b) : "different grades must not lose their boundary faces";
            }
        }
        assert VoltageGlass.ENTRIES.size() == 13;
        assert VoltageGlassBlock.tierOf(Blocks.GLASS.defaultBlockState()) == -1;
        assert VoltageGlassBlock.tierOf(Blocks.AIR.defaultBlockState()) == -1;
        for (int tier : new int[]{Integer.MIN_VALUE, 0, 14, Integer.MAX_VALUE}) {
            try { VoltageGlass.get(tier); throw new AssertionError("invalid tier accepted"); }
            catch (IllegalArgumentException expected) {}
        }
        try { VoltageGlass.between(GTValues.HV, GTValues.LV); throw new AssertionError("reversed range accepted"); }
        catch (IllegalArgumentException expected) {}
        try { VoltageGlass.atLeast(GTValues.MAX); throw new AssertionError("unsupported MAX accepted"); }
        catch (IllegalArgumentException expected) {}
        System.out.println("Voltage glass passed: 13 GT tiers, immutable state identity, 169 grade boundaries,"
                + " same-grade face culling, ordinary glass exclusion, transparent unlit blocks and range rejection.");
    }
}
