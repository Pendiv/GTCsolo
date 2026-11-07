package DIV.gtcsolo.audit;

import DIV.gtcsolo.client.performance.MegaWorldPreview;
import DIV.gtcsolo.machine.mega.MegaPatternResource;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

import java.lang.reflect.Method;

/** The isolated runner supplies only the accessor bridge; the tested driver and native renderer are real. */
public final class MegaWorldPreviewTransformRegressionTest {
    public static void main(String[] arguments) throws Exception {
        Method rotate = MultiblockInWorldPreviewRenderer.class.getDeclaredMethod("rotateByFrontAxis", BlockPos.class, Direction.class, Rotation.class);
        rotate.setAccessible(true);
        var headers = new MegaPatternResource.Header[]{
                new MegaPatternResource.Header(120, 101, 89, 62, 8, 2),
                new MegaPatternResource.Header(189, 225, 182, 59, 6, 11),
                new MegaPatternResource.Header(195, 93, 157, 103, 6, 3)};
        int comparisons = 0;
        for (var header : headers) for (Direction front : Direction.values()) for (Direction up : Direction.values()) {
            Rotation frontRotation = switch (front) {
                case SOUTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.CLOCKWISE_90;
                case EAST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
            Rotation upRotation = switch (up) {
                case EAST -> Rotation.CLOCKWISE_90;
                case SOUTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
            for (BlockPos origin : new BlockPos[]{BlockPos.ZERO, new BlockPos(-126, -49, -80),
                    new BlockPos(30_000_000, -2048, -30_000_000), new BlockPos(Integer.MAX_VALUE, Integer.MIN_VALUE, -1)}) {
                for (int x : new int[]{0, header.anchorX(), header.width() - 1})
                    for (int y : new int[]{0, header.anchorY(), header.height() - 1})
                        for (int z : new int[]{0, header.anchorZ(), header.depth() - 1}) {
                            var local = new BlockPos(x, y, z);
                            var expectedOffset = local.subtract(new BlockPos(header.anchorX(), header.anchorY(), header.anchorZ())).rotate(frontRotation);
                            var expected = origin.offset((BlockPos) rotate.invoke(null, expectedOffset, front, upRotation));
                            var actual = MegaWorldPreview.worldPosition(local, header, origin, front, up);
                            if (!expected.equals(actual)) throw new AssertionError(front + "/" + up + ": " + actual + " != " + expected);
                            if (local.equals(new BlockPos(header.anchorX(), header.anchorY(), header.anchorZ())) && !actual.equals(origin))
                                throw new AssertionError("Controller anchor moved");
                            if (actual instanceof BlockPos.MutableBlockPos) throw new AssertionError("Escaping position must be immutable");
                            comparisons++;
                        }
            }
        }
        System.out.println("PASS MegaWorldPreviewTransformRegressionTest: " + comparisons + " comparisons against native rotation");
    }
}
