package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.VoidAbsorberMachine;
import DIV.gtcsolo.mixin.PatternPreviewSafetyMixin;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.world.level.Level;
import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;

/** Native GT preview and coordinate checks, without running the active game or a GL context. */
public final class VoidAbsorberPatternTest {
    public static void main(String[] args) throws Exception {
        if (!VoidAbsorberPatternTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0 || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        var data = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        var layers = data.getAsJsonArray("aisles");
        int repeatIndex = data.get("repeat_index").getAsInt();
        var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.BACK, RelativeDirection.DOWN);
        for (int i = 0; i < layers.size(); i++) {
            String[] rows = new String[11];
            for (int row = 0; row < 11; row++) rows[row] = layers.get(i).getAsJsonArray().get(row).getAsString();
            builder.aisle(rows);
            if (i == repeatIndex) builder.setRepeatable(0, 12);
        }
        var foundation = VoidAbsorberMachine.foundationPredicate();
        assert foundation.limited.size() == 1 && foundation.limited.get(0).minCount == 5;
        builder.where('R', foundation).where('#', Predicates.any())
                .where('Q', VoidAbsorberMachine.repeatedCasingPredicate(Blocks.IRON_BLOCK))
                .where('S', Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK)));
        for (char c : "FPTC".toCharArray()) builder.where(c, Predicates.blocks(Blocks.IRON_BLOCK));
        BlockPattern pattern = builder.build();
        assert pattern.aisleRepetitions[repeatIndex][0] == 0 && pattern.aisleRepetitions[repeatIndex][1] == 12;
        var transform = BlockPattern.class.getDeclaredMethod("setActualRelativeOffset", int.class, int.class, int.class,
                Direction.class, Direction.class, boolean.class);
        transform.setAccessible(true);
        var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        // Only height accessors are used. No server/world constructor, chunk IO or game process is run.
        FloorOnlyLevel level = (FloorOnlyLevel) unsafe.allocateInstance(FloorOnlyLevel.class);
        PreviewLevel previewLevel = (PreviewLevel) unsafe.allocateInstance(PreviewLevel.class);
        int displayFormed = 0;
        int cells = 0;
        int formed = 0;
        for (int repeat = 0; repeat <= 12; repeat++) {
            int[] repetitions = new int[layers.size()];
            Arrays.fill(repetitions, 1);
            repetitions[repeatIndex] = repeat;
            var preview = pattern.getPreview(repetitions);
            assert preview.length == 11 && preview[0].length == 8 + repeat && preview[0][0].length == 11;
            assert preview[5][4 + repeat][0].getBlockState().is(Blocks.DIAMOND_BLOCK);
            int bedrock = 0;
            for (int x = 0; x < 11; x++) for (int y = 0; y < 8 + repeat; y++) for (int z = 0; z < 11; z++) {
                var state = preview[x][y][z].getBlockState();
                if (state.is(Blocks.BEDROCK)) { bedrock++; assert y == 0; }
                if (state.is(Blocks.DEEPSLATE)) assert y == 0;
                assert !state.is(Blocks.GRASS_BLOCK);
                cells++;
            }
            assert bedrock == 5;
            for (Direction facing : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos bottom = (BlockPos) transform.invoke(pattern, 0, 5, 4 + repeat, facing, Direction.UP, false);
                assert bottom.getY() == -(4 + repeat);
                assert bottom.getX() == facing.getOpposite().getStepX() * 5;
                assert bottom.getZ() == facing.getOpposite().getStepZ() * 5;
                // Check vanilla/custom height ranges, then translate the same valid structure above their floors.
                for (int minimum : new int[]{-64, 0, -128}) {
                    assert bottom.getY() + minimum + 4 + repeat == minimum;
                    level.minimum = minimum;
                    BlockPos core = new BlockPos(80, minimum + 4 + repeat, -48);
                    var blocks = new HashMap<BlockPos, BlockState>();
                    var feet = new ArrayList<BlockPos>();
                    for (int x = 0; x < 11; x++) for (int y = 0; y < 8 + repeat; y++) for (int z = 0; z < 11; z++) {
                        var state = preview[x][y][z].getBlockState();
                        if (state.isAir()) continue;
                        BlockPos delta = (BlockPos) transform.invoke(pattern, x - 5, z, 4 + repeat - y, facing, Direction.UP, false);
                        BlockPos pos = core.offset(delta);
                        blocks.put(pos, state);
                        if (y == 0) feet.add(pos);
                    }
                    assert pattern.checkPatternAt(new TestState(level, core, blocks), core, facing, Direction.UP, false, false)
                            : "valid pattern rejected: repetitions=" + repeat + ", minimum=" + minimum + ", facing=" + facing;
                    assert pattern.getFormedRepetitionCount()[repeatIndex] == repeat;
                    formed++;
                    for (int bedrockCount : new int[]{0, 4, 5}) {
                        for (int i = 0; i < feet.size(); i++) blocks.put(feet.get(i),
                                (i < bedrockCount ? Blocks.BEDROCK : Blocks.DEEPSLATE).defaultBlockState());
                        boolean valid = pattern.checkPatternAt(new TestState(level, core, blocks), core, facing, Direction.UP, false, false);
                        assert valid == (bedrockCount >= 5) : "bedrock minimum bypass";
                    }
                    var raised = new HashMap<BlockPos, BlockState>();
                    blocks.forEach((pos, state) -> raised.put(pos.above(), state));
                    assert pattern.checkPatternAt(new TestState(level, core.above(), raised), core.above(), facing, Direction.UP, false, true)
                            : "block-valid foundation incorrectly rejected above world minimum";
                    formed++;
                    var invalidFoundation = new HashMap<BlockPos, BlockState>(blocks);
                    invalidFoundation.put(feet.get(feet.size() - 1), Blocks.STONE.defaultBlockState());
                    assert !pattern.checkPatternAt(new TestState(level, core, invalidFoundation), core, facing, Direction.UP, false, true);

                    for (int displayFloor : new int[]{50, 96}) {
                        previewLevel.minimum = minimum;
                        int shift = displayFloor - minimum;
                        var floating = new HashMap<BlockPos, BlockState>();
                        blocks.forEach((pos, state) -> floating.put(pos.above(shift), state));
                        BlockPos displayCore = core.above(shift);
                        var displayed = new TestState(previewLevel, displayCore, floating);
                        assert pattern.checkPatternAt(displayed, displayCore, facing, Direction.UP, false, true)
                                : "translated JEI template failed at height " + displayFloor;
                        assert pattern.getFormedRepetitionCount()[repeatIndex] == repeat;
                        Map<BlockPos, TraceabilityPredicate> clickable = displayed.getMatchContext().get("predicates");
                        assert clickable != null && clickable.keySet().containsAll(floating.keySet()) : "missing clickable parts";
                        displayFormed++;
                        BlockPos removedBedrock = floating.entrySet().stream().filter(e -> e.getValue().is(Blocks.BEDROCK))
                                .map(Map.Entry::getKey).findFirst().orElseThrow();
                        floating.put(removedBedrock, Blocks.DEEPSLATE.defaultBlockState());
                        assert !pattern.checkPatternAt(new TestState(previewLevel, displayCore, floating),
                                displayCore, facing, Direction.UP, false, true) : "preview bypassed five-bedrock requirement";
                    }
                }
            }
        }
        var selection = PatternPreviewSafetyMixin.class.getDeclaredMethod("gtcsolo$selectAvailablePredicate", Map.class, Object.class);
        selection.setAccessible(true);
        var mixin = new PatternPreviewSafetyMixin() {};
        assert selection.invoke(mixin, null, BlockPos.ZERO) == null : "invalid preview click still throws";
        assert selection.invoke(mixin, Map.of(BlockPos.ZERO, foundation), BlockPos.ZERO) == foundation;
        assert selection.invoke(mixin, Map.of(), BlockPos.ZERO) == null;
        System.out.println("PASS: 13 native GT previews, " + formed + " world formations at floor and above, " + displayFormed
                + " translated display formations with clickable maps; bedrock 0/4/5, wrong foundation block and null-map selection; " + cells + " preview cells checked.");
    }

    private static final class FloorOnlyLevel extends ServerLevel {
        int minimum;
        Map<BlockPos, BlockState> blocks;
        @Override public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
        private FloorOnlyLevel() { super(null, null, null, null, null, null, null, false, 0L, List.of(), false, null); }
        @Override public int getMinBuildHeight() { return minimum; }
        @Override public int getMaxBuildHeight() { return minimum + 384; }
    }

    private static final class PreviewLevel extends TrackedDummyWorld {
        int minimum;
        Map<BlockPos, BlockState> blocks;
        private PreviewLevel() { super((Level) null); }
        @Override public int getMinBuildHeight() { return minimum; }
        @Override public int getMaxBuildHeight() { return minimum + 384; }
        @Override public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
    }

    private static final class TestState extends MultiblockState {
        private final Map<BlockPos, BlockState> blocks;
        private BlockPos current;
        TestState(Level level, BlockPos core, Map<BlockPos, BlockState> blocks) {
            super(level, core);
            this.blocks = blocks;
            if (level instanceof FloorOnlyLevel floor) floor.blocks = blocks;
            if (level instanceof PreviewLevel preview) preview.blocks = blocks;
        }
        @Override protected boolean update(BlockPos pos, TraceabilityPredicate predicate) {
            current = pos;
            this.predicate = predicate;
            error = null;
            // Match native Level.isLoaded's out-of-height rejection; the fixture's chunks are loaded.
            return pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight();
        }
        @Override public BlockPos getPos() { return current; }
        @Override public BlockState getBlockState() { return blocks.getOrDefault(current, Blocks.AIR.defaultBlockState()); }
        @Override public BlockEntity getTileEntity() { return null; }
    }
}
