package DIV.gtcsolo.audit;

import DIV.gtcsolo.performance.PatternCoordinateTransform;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Compares the real, unmodified GT private transform against the scalar candidate. */
public final class PatternCoordinateTransformRegressionTest {
    public static void main(String[] args) throws Exception {
        if (!PatternCoordinateTransformRegressionTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        Method nativeOffset = BlockPattern.class.getDeclaredMethod("setActualRelativeOffset", int.class, int.class,
                int.class, Direction.class, Direction.class, boolean.class);
        nativeOffset.setAccessible(true);
        List<int[]> vectors = new ArrayList<>(List.of(new int[]{0, 0, 0}, new int[]{1, 2, 3},
                new int[]{-1, -2, -3}, new int[]{-62, -8, -2}, new int[]{57, 92, 86},
                new int[]{30_000_000, -64, -30_000_000}, new int[]{-30_000_000, 320, 30_000_000},
                new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, -1}, new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE},
                new int[]{Integer.MAX_VALUE, -1, Integer.MIN_VALUE}));
        Random random = new Random(0x164_20_01L);
        for (int i = 0; i < 10; i++) vectors.add(new int[]{random.nextInt(), random.nextInt(), random.nextInt()});
        int compared = 0;
        for (RelativeDirection a : RelativeDirection.values()) {
            for (RelativeDirection b : RelativeDirection.values()) {
                for (RelativeDirection c : RelativeDirection.values()) {
                    RelativeDirection[] axes = {a, b, c};
                    var pattern = new BlockPattern(new TraceabilityPredicate[1][1][1], axes,
                            new int[][]{{1, 1}}, new int[5]);
                    for (Direction front : Direction.values()) {
                        for (Direction up : Direction.Plane.HORIZONTAL) {
                            for (boolean flipped : new boolean[]{false, true}) {
                                assert PatternCoordinateTransform.supports(front, up);
                                for (int[] vector : vectors) {
                                    BlockPos expected = (BlockPos) nativeOffset.invoke(pattern,
                                            vector[0], vector[1], vector[2], front, up, flipped);
                                    BlockPos actual = PatternCoordinateTransform.offset(axes,
                                            vector[0], vector[1], vector[2], front, up, flipped);
                                    assert actual.equals(expected) : a + "/" + b + "/" + c + " " + front + "/" + up
                                            + " flipped=" + flipped + " expected=" + expected + " actual=" + actual;
                                    assert actual.getClass() == BlockPos.class && actual != expected;
                                    BlockPos again = PatternCoordinateTransform.offset(axes,
                                            vector[0], vector[1], vector[2], front, up, flipped);
                                    assert again != actual && again.equals(actual) : "do not introduce mutable/shared positions";
                                    compared++;
                                }
                            }
                        }
                    }
                    // The native constructor retains its axes array; the candidate must observe a later mutation.
                    axes[0] = axes[0] == RelativeDirection.UP ? RelativeDirection.DOWN : RelativeDirection.UP;
                    BlockPos expected = (BlockPos) nativeOffset.invoke(pattern, -7, 11, 19,
                            Direction.EAST, Direction.WEST, true);
                    assert PatternCoordinateTransform.offset(axes, -7, 11, 19,
                            Direction.EAST, Direction.WEST, true).equals(expected);
                }
            }
        }
        assert !PatternCoordinateTransform.supports(Direction.NORTH, Direction.UP);
        assert !PatternCoordinateTransform.supports(Direction.NORTH, Direction.DOWN);
        assert !PatternCoordinateTransform.supports(null, Direction.NORTH);
        assert !PatternCoordinateTransform.supports(Direction.NORTH, null);
        System.out.println("Coordinate projection passed " + compared + " native comparisons: 216 axis combinations,"
                + " 48 front/up/flip states, boundaries, negative and int-overflow coordinates,"
                + " axes mutation and fresh immutable result identity.");
    }
}
