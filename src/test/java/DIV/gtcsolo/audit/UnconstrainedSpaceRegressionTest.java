package DIV.gtcsolo.audit;

import DIV.gtcsolo.performance.UnconstrainedPatternCells;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import sun.misc.Unsafe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Free cells accept arbitrary contents without weakening the surrounding structure. */
public final class UnconstrainedSpaceRegressionTest {
    private static final Pattern REQUIRED_AIR = Pattern.compile(
            "\\.where\\(\\s*(['\"])(.)\\1\\s*,\\s*(?:Predicates\\.)?air\\s*\\(\\s*\\)\\s*\\)");
    // Future exceptions must name the source path/symbol and explain why air itself is required.
    private static final Map<String, String> REQUIRED_AIR_REASONS = Map.of();

    private static final class TestLevel extends ServerLevel {
        private BlockState contents;
        private int reads;

        private TestLevel() { super(null, null, null, null, null, null, null, false, 0L, List.of(), false, null); }
        @Override public BlockState getBlockState(BlockPos pos) { reads++; return contents; }
        @Override public boolean isEmptyBlock(BlockPos pos) { reads++; return contents.isAir(); }
    }

    private static final class TestState extends MultiblockState {
        private TestState(TestLevel level) { super(level, BlockPos.ZERO); clean(); }
        @Override public BlockPos getPos() { return BlockPos.ZERO; }
        @Override public BlockState getBlockState() { return world.getBlockState(BlockPos.ZERO); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0 || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
        }
        var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var level = (TestLevel) ((Unsafe) unsafeField.get(null)).allocateInstance(TestLevel.class);
        var state = new TestState(level);
        UnconstrainedPatternCells.captureCanonicalAny(SimplePredicate.ANY);
        var unrestricted = Predicates.any();
        check(UnconstrainedPatternCells.isPureAny(unrestricted), "Use the native, optimizable ANY predicate");
        check(UnconstrainedPatternCells.canSkip(new MultiblockState(level, BlockPos.ZERO), unrestricted), "Native state can skip the free cell");
        for (var block : List.of(Blocks.AIR, Blocks.CAVE_AIR, Blocks.STONE, Blocks.WATER, Blocks.LAVA, Blocks.CHEST)) {
            level.contents = block.defaultBlockState(); level.reads = 0;
            check(unrestricted.test(state), "Any block is allowed in a free cell");
            check(level.reads == 0, "ANY must not inspect world contents");
            check(Predicates.air().test(state) == level.contents.isAir(), "AIR is distinct from ANY");
            check(level.reads > 0, "AIR requires a world query");
        }
        var casing = Predicates.blocks(Blocks.IRON_BLOCK);
        var optionalCasing = casing.or(Predicates.air());
        check(!UnconstrainedPatternCells.isPureAny(optionalCasing), "Optional casing remains a real constraint");
        level.contents = Blocks.STONE.defaultBlockState();
        check(!casing.test(state) && !optionalCasing.test(state), "Wrong casing remains rejected");
        level.contents = Blocks.IRON_BLOCK.defaultBlockState();
        check(casing.test(state) && optionalCasing.test(state), "Correct casing remains accepted");
        level.contents = Blocks.AIR.defaultBlockState();
        check(!casing.test(state) && optionalCasing.test(state), "Only optional casing allows omission");
        var pattern = FactoryBlockPattern.start().aisle("SB")
                .where('S', Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK)))
                .where('B', Predicates.any()).build();
        int filled = 0;
        for (var plane : pattern.getPreview(new int[]{1})) for (var row : plane) for (var cell : row)
            if (!cell.getBlockState().isAir()) filled++;
        check(filled == 1, "Unrestricted cells still preview as empty");

        int files = 0;
        for (var directory : List.of(Path.of("src/main/java/DIV/gtcsolo"), Path.of("run/kubejs/startup_scripts"))) {
            if (!Files.isDirectory(directory)) continue;
            try (var paths = Files.walk(directory)) {
                for (var file : paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".js")).toList()) {
                    files++;
                    String source = Files.readString(file);
                    var matches = REQUIRED_AIR.matcher(source);
                    while (matches.find()) {
                        String cell = file.toString().replace('\\', '/') + ":" + matches.group(2);
                        check(!REQUIRED_AIR_REASONS.getOrDefault(cell, "").isBlank(), "Undocumented air-only structural cell: " + cell);
                    }
                }
            }
        }
        System.out.println("UNCONSTRAINED_SPACE_PASS arbitraryBlocks=true zeroWorldReads=true casingConstraints=true optionalCasing=true emptyPreview=true sourceFiles=" + files);
    }
}
