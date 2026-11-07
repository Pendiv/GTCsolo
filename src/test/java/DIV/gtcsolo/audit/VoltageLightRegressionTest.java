package DIV.gtcsolo.audit;

import DIV.gtcsolo.block.torch.*;
import DIV.gtcsolo.registry.VoltageTorches;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;

/** Native block states and the shared ward resolver; no client/server world is started. */
public final class VoltageLightRegressionTest {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        try { Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        // This disposable JVM has no Forge registration phase. Open only its block holder registry
        // so the real block constructors can run after vanilla bootstrap, as they do during registration.
        ((MappedRegistry<?>) BuiltInRegistries.BLOCK).unfreeze();
        int states = 0;
        for (VoltageTorchTier tier : VoltageTorchTier.values()) {
            var lantern = new VoltageLanternBlock(BlockBehaviour.Properties.copy(Blocks.LANTERN), tier);
            lantern.getStateDefinition().getPossibleStates().forEach(state -> state.initCache());
            for (boolean hanging : new boolean[]{false, true}) {
                for (boolean waterlogged : new boolean[]{false, true}) {
                    var state = lantern.defaultBlockState().setValue(LanternBlock.HANGING, hanging)
                            .setValue(LanternBlock.WATERLOGGED, waterlogged);
                    assert VoltageTorches.tier(state) == tier.level() : "water/hanging must preserve ward";
                    assert state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL;
                    assert state.getLightEmission() == 15;
                    assert state.getFluidState().is(Fluids.WATER) == waterlogged;
                    assert lantern.getTicker(null, state, null) == null : "ward anchors must not tick";
                    states++;
                }
            }
            var pumpkin = new VoltageJackOLanternBlock(BlockBehaviour.Properties.copy(Blocks.JACK_O_LANTERN), tier);
            pumpkin.getStateDefinition().getPossibleStates().forEach(state -> state.initCache());
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = pumpkin.defaultBlockState().setValue(VoltageJackOLanternBlock.FACING, facing);
                assert VoltageTorches.tier(state) == tier.level();
                assert state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL;
                assert state.getLightEmission() == 15;
                assert state.rotate(Rotation.CLOCKWISE_90).getValue(VoltageJackOLanternBlock.FACING) == facing.getClockWise();
                assert state.rotate(Rotation.CLOCKWISE_90).rotate(Rotation.COUNTERCLOCKWISE_90).equals(state);
                assert state.mirror(Mirror.LEFT_RIGHT).mirror(Mirror.LEFT_RIGHT).equals(state);
                assert pumpkin.getTicker(null, state, null) == null;
                states++;
            }
            var torch = new VoltageTorchBlock(BlockBehaviour.Properties.copy(Blocks.TORCH), tier);
            var wall = new VoltageWallTorchBlock(BlockBehaviour.Properties.copy(Blocks.WALL_TORCH), tier);
            assert VoltageTorches.tier(torch.defaultBlockState()) == tier.level();
            assert VoltageTorches.tier(wall.defaultBlockState()) == tier.level();
            assert torch.defaultBlockState().getLightEmission() == 14;
        }
        assert VoltageTorches.tier(Blocks.LANTERN.defaultBlockState()) == 0;
        assert VoltageTorches.tier(Blocks.JACK_O_LANTERN.defaultBlockState()) == 0;
        assert VoltageTorches.tier(Blocks.TORCH.defaultBlockState()) == 0;
        assert VoltageTorches.tier(Blocks.AIR.defaultBlockState()) == 0;
        assert VoltageTorches.ENTRIES.size() == 14 && VoltageTorches.LIGHTS.size() == 28;
        assert VoltageTorches.LIGHTS.stream().map(entry -> entry.block().getId()).distinct().count() == 28;
        System.out.println("Voltage lights passed: " + states + " native states; hanging/waterlogged, facing/rotation,"
                + " light levels, shared ward identity, non-ticking anchors, and ordinary lights excluded.");
    }
}
