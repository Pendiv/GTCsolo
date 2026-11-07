package DIV.gtcsolo.audit;

import DIV.gtcsolo.api.tier.RoomStructureTier;
import DIV.gtcsolo.block.VoltageGlassBlock;
import DIV.gtcsolo.machine.RoomStructures;
import DIV.gtcsolo.machine.RoomStructures.Cell;
import DIV.gtcsolo.machine.RoomStructures.Structure;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Native GT pattern transforms and grade context without starting or changing a game world. */
public final class RoomStructureRegressionTest {
    public static void main(String[] args) throws Exception {
        var stderr = System.err;
        try { runChecks(args); }
        catch (Throwable failure) {
            failure.printStackTrace(stderr);
            throw failure;
        }
    }

    private static void runChecks(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        try { Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        var tierSet = new RoomStructureTier("gtcsolo_room_structure_test");
        int matchPairs = 0;
        for (int casing = GTValues.LV; casing <= GTValues.OpV; casing++) {
            for (int glass = GTValues.LV; glass <= GTValues.OpV; glass++) {
                for (boolean glassFirst : new boolean[] {false, true}) {
                    var context = new PatternMatchContext();
                    assert tierSet.matchTier(context, glassFirst ? glass : casing);
                    assert tierSet.matchTier(context, glassFirst ? casing : glass) == (casing == glass);
                    assert tierSet.tierFromContext(context) == (glassFirst ? glass : casing);
                    assert context.get(tierSet.contextKey()) instanceof Integer;
                    context.reset();
                    assert tierSet.tierFromContext(context) == -1;
                    matchPairs++;
                }
            }
        }
        for (int invalid : new int[] {Integer.MIN_VALUE, GTValues.ULV, GTValues.MAX, Integer.MAX_VALUE}) {
            try { tierSet.matchTier(new PatternMatchContext(), invalid); throw new AssertionError("Invalid room grade accepted"); }
            catch (IllegalArgumentException expected) {}
        }
        ((MappedRegistry<?>) BuiltInRegistries.BLOCK).unfreeze();
        for (int grade = GTValues.LV; grade <= GTValues.OpV; grade++) {
            var glass = new VoltageGlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS), grade);
            glass.getStateDefinition().getPossibleStates().forEach(BlockState::initCache);
            var state = new PatternFixture(Map.of(), glass.defaultBlockState());
            assert tierSet.glassPredicate().test(state);
            assert tierSet.tierFromContext(state.getMatchContext()) == grade;
            assert !tierSet.matchTier(state.getMatchContext(), grade == GTValues.OpV ? GTValues.LV : grade + 1);
            assert tierSet.glassTierOf(glass.defaultBlockState()) == grade;
        }
        Path root = args.length == 0 ? Path.of("") : Path.of(args[0]);
        verifyStructure(root, RoomStructures.FAMING_ROOM, 7, 5, 5,
                Map.of('A', 107, 'B', 1, 'C', 10, 'D', 6, 'E', 6, 'F', 9, 'G', 6, '#', 30));
        verifyStructure(root, RoomStructures.CONSTRUCTION_ROOM, 5, 7, 5,
                Map.of('A', 60, 'B', 30, 'C', 1, 'D', 12, 'E', 16, 'F', 18, 'G', 8, 'H', 1, '#', 29));
        System.out.println("Room structures passed: exact 350 exported cells, dimensions/core/legend, native GT pattern"
                + " matching and outward controller at all four facings, native preview coordinates, "
                + matchPairs + " ordered grade pairs, 13 real voltage-glass predicates, and unsupported-grade rejection.");
        System.out.println("Standalone verification does not register GT casings or vanilla-glass aliases; Forge registration,"
                + " hatch capabilities, JEI rendering and powered recipes require separate runtime validation.");
    }

    private static void verifyStructure(Path root, Structure structure, int width, int height, int depth,
            Map<Character, Integer> expectedCounts) throws Exception {
        assert structure.width() == width && structure.height() == height && structure.depth() == depth;
        assert structure.core().equals(new Cell(0, 0, width / 2));
        var rows = Pattern.compile("\\(([^)]*)\\)").matcher(Files.readString(root.resolve(
                "run/cmdex/export/" + structure.id() + ".txt")));
        int aisleIndex = 0;
        while (rows.find()) {
            var cells = Pattern.compile("\"([^\"]*)\"").matcher(rows.group(1));
            int y = 0;
            while (cells.find()) assert structure.aisles()[aisleIndex][y++].equals(cells.group(1));
            assert y == height;
            aisleIndex++;
        }
        assert aisleIndex == depth;
        assert structure.exportLegend().keySet().equals(expectedCounts.keySet());
        expectedCounts.forEach((symbol, count) -> { assert structure.positions(symbol).size() == count; });
        String[][] detached = structure.aisles();
        detached[0][0] = "mutated";
        assert structure.aisles()[0][0].length() == width;

        var previewBuilder = structure.shapeBuilder();
        var exampleStates = List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.DIRT,
                Blocks.SAND, Blocks.GRAVEL, Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK, Blocks.AIR);
        Map<Character, BlockState> symbols = new HashMap<>();
        int sample = 0;
        for (char symbol : expectedCounts.keySet()) {
            BlockState blockState = exampleStates.get(sample++).defaultBlockState();
            symbols.put(symbol, blockState);
            previewBuilder.where(symbol, blockState);
        }
        var preview = previewBuilder.build().getBlocks();
        assert preview.length == width && preview[0].length == height && preview[0][0].length == depth;
        for (int a = 0; a < depth; a++) for (int y = 0; y < height; y++) for (int column = 0; column < width; column++) {
            var cell = new Cell(a, y, column);
            var pos = structure.previewPosition(cell);
            assert pos.equals(new BlockPos(column, y, a));
            assert preview[pos.getX()][pos.getY()][pos.getZ()].getBlockState() == symbols.get(structure.symbolAt(cell));
        }
        for (Direction front : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            var builder = structure.patternBuilder();
            Map<BlockPos, Character> worldSymbols = new HashMap<>();
            expectedCounts.keySet().forEach(symbol -> {
                for (Cell cell : structure.positions(symbol)) worldSymbols.put(structure.relativePosition(cell, front), symbol);
                var predicate = new TraceabilityPredicate(state -> ((PatternFixture) state).symbol() == symbol, null);
                builder.where(symbol, symbol == structure.coreSymbol() ? predicate.setController() : predicate);
            });
            assert structure.relativePosition(structure.core(), front).equals(BlockPos.ZERO);
            assert !worldSymbols.containsKey(BlockPos.ZERO.relative(front)) : "Controller front is buried";
            var pattern = builder.build();
            var fixture = new PatternFixture(worldSymbols, Blocks.STONE.defaultBlockState());
            assert pattern.checkPatternAt(fixture, BlockPos.ZERO, front, Direction.NORTH, false, true);
            assert fixture.getCache().size() == width * height * depth;
            var broken = new HashMap<>(worldSymbols);
            broken.put(structure.relativePosition(new Cell(depth - 1, height - 1, width - 1), front), '?');
            assert !pattern.checkPatternAt(new PatternFixture(broken, Blocks.STONE.defaultBlockState()),
                    BlockPos.ZERO, front, Direction.NORTH, false, true) : "Broken exported shell was accepted";
        }
    }

    private static final class PatternFixture extends MultiblockState {
        private final Map<BlockPos, Character> symbols;
        private final BlockState blockState;
        private BlockPos current = BlockPos.ZERO;

        private PatternFixture(Map<BlockPos, Character> symbols, BlockState blockState) {
            super(null, BlockPos.ZERO);
            this.symbols = symbols;
            this.blockState = blockState;
        }

        private char symbol() { return symbols.getOrDefault(current, '?'); }
        @Override protected boolean update(BlockPos pos, TraceabilityPredicate predicate) {
            current = pos.immutable();
            this.predicate = predicate;
            setError(null);
            return true;
        }
        @Override public BlockPos getPos() { return current; }
        @Override public BlockState getBlockState() { return blockState; }
        @Override public BlockEntity getTileEntity() { return null; }
    }
}
