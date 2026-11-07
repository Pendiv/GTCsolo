package DIV.gtcsolo.machine.wen;

import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;

/** Bounded cuboid discovery; the native matcher still validates every shell and cell position. */
public final class WENStorageStructure {
    public static final int MIN_SIZE = 3, MAX_SIZE = 15;
    public static final Shape DEFAULT = new Shape(1, 1, 0, 2, 0, 2);

    private WENStorageStructure() {}

    public record Shape(int left, int right, int down, int up, int front, int back) {
        public int width() {
            return left + right + 1;
        }

        public int height() {
            return down + up + 1;
        }

        public int depth() {
            return front + back + 1;
        }

        public boolean valid() {
            return left >= 0
                    && right >= 0
                    && down >= 0
                    && up >= 0
                    && front >= 0
                    && back >= 0
                    && width() >= MIN_SIZE
                    && width() <= MAX_SIZE
                    && height() >= MIN_SIZE
                    && height() <= MAX_SIZE
                    && depth() >= MIN_SIZE
                    && depth() <= MAX_SIZE
                    && (left == 0 || right == 0 || down == 0 || up == 0 || front == 0 || back == 0);
        }

        public boolean shell(int x, int y, int z) {
            return x == -left || x == right || y == -down || y == up || z == -front || z == back;
        }

        public boolean contains(
                BlockPos origin, Direction facing, Direction upwards, BlockPos position) {
            return contains(origin, facing, upwards, false, position);
        }

        public boolean contains(
                BlockPos origin,
                Direction facing,
                Direction upwards,
                boolean flipped,
                BlockPos position) {
            BlockPos delta = position.subtract(origin);
            int x = dot(delta, RIGHT.getRelativeFacing(facing, upwards, flipped));
            int y = dot(delta, UP.getRelativeFacing(facing, upwards, flipped));
            int z = dot(delta, BACK.getRelativeFacing(facing, upwards, flipped));
            return x >= -left && x <= right && y >= -down && y <= up && z >= -front && z <= back;
        }
    }

    private static int dot(BlockPos delta, Direction direction) {
        return delta.getX() * direction.getStepX()
                + delta.getY() * direction.getStepY()
                + delta.getZ() * direction.getStepZ();
    }

    public static BlockPos position(
            BlockPos origin, Direction facing, Direction upwards, int x, int y, int z) {
        return position(origin, facing, upwards, false, x, y, z);
    }

    public static BlockPos position(
            BlockPos origin,
            Direction facing,
            Direction upwards,
            boolean flipped,
            int x,
            int y,
            int z) {
        return origin.relative(RIGHT.getRelativeFacing(facing, upwards, flipped), x)
                .relative(UP.getRelativeFacing(facing, upwards, flipped), y)
                .relative(BACK.getRelativeFacing(facing, upwards, flipped), z);
    }

    public static Shape detect(
            BlockPos origin,
            Direction facing,
            Direction upwards,
            Predicate<BlockPos> loaded,
            Function<BlockPos, BlockState> states,
            Predicate<BlockState> structural) {
        return detect(origin, facing, upwards, false, loaded, states, structural);
    }

    public static Shape detect(
            BlockPos origin,
            Direction facing,
            Direction upwards,
            boolean flipped,
            Predicate<BlockPos> loaded,
            Function<BlockPos, BlockState> states,
            Predicate<BlockState> structural) {
        int left =
                extent(
                        origin,
                        LEFT.getRelativeFacing(facing, upwards, flipped),
                        loaded,
                        states,
                        structural);
        int right =
                extent(
                        origin,
                        RIGHT.getRelativeFacing(facing, upwards, flipped),
                        loaded,
                        states,
                        structural);
        int down =
                extent(
                        origin,
                        DOWN.getRelativeFacing(facing, upwards, flipped),
                        loaded,
                        states,
                        structural);
        int up =
                extent(
                        origin,
                        UP.getRelativeFacing(facing, upwards, flipped),
                        loaded,
                        states,
                        structural);
        int front =
                extent(
                        origin,
                        FRONT.getRelativeFacing(facing, upwards, flipped),
                        loaded,
                        states,
                        structural);
        int back =
                extent(
                        origin,
                        BACK.getRelativeFacing(facing, upwards, flipped),
                        loaded,
                        states,
                        structural);
        Shape shape = new Shape(left, right, down, up, front, back);
        return shape.valid() ? shape : null;
    }

    private static int extent(
            BlockPos origin,
            Direction direction,
            Predicate<BlockPos> loaded,
            Function<BlockPos, BlockState> states,
            Predicate<BlockState> structural) {
        for (int distance = 1; distance <= MAX_SIZE; distance++) {
            BlockPos position = origin.relative(direction, distance);
            if (!loaded.test(position)) return -1;
            if (!structural.test(states.apply(position))) return distance - 1;
        }
        return -1;
    }

    public static List<Block> cells() {
        return List.of(
                ModBlocks.WEN_BASIC_ENERGY_CELL.get(),
                GTBlocks.BATTERY_LAPOTRONIC_EV.get(),
                GTBlocks.BATTERY_LAPOTRONIC_IV.get(),
                GTBlocks.BATTERY_LAPOTRONIC_LuV.get(),
                GTBlocks.BATTERY_LAPOTRONIC_ZPM.get(),
                GTBlocks.BATTERY_LAPOTRONIC_UV.get(),
                GTBlocks.BATTERY_ULTIMATE_UHV.get());
    }

    public static BlockPattern pattern(MultiblockMachineDefinition definition, Shape shape) {
        return pattern(
                definition.getBlock(),
                shape,
                blocks(ModBlocks.WEN_MAINSTORAGE_CASING.get())
                        .or(blocks(ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get()))
                        .or(blocks(ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get()))
                        .or(
                                blocks(ModBlocks.WEN_DATA_MONITOR.get())
                                        .setMinGlobalLimited(1)
                                        .setMaxGlobalLimited(1)),
                blocks(cells().toArray(Block[]::new)));
    }

    public static BlockPattern pattern(
            Block core, Shape shape, TraceabilityPredicate wall, TraceabilityPredicate cell) {
        if (!shape.valid())
            throw new IllegalArgumentException("Invalid WEN shell dimensions or core position");
        var builder = FactoryBlockPattern.start(RIGHT, UP, BACK);
        for (int z = -shape.front(); z <= shape.back(); z++) {
            String[] rows = new String[shape.height()];
            for (int y = -shape.down(); y <= shape.up(); y++) {
                StringBuilder row = new StringBuilder(shape.width());
                for (int x = -shape.left(); x <= shape.right(); x++)
                    row.append(x == 0 && y == 0 && z == 0 ? 'C' : shape.shell(x, y, z) ? 'W' : 'S');
                rows[y + shape.down()] = row.toString();
            }
            builder.aisle(rows);
        }
        return builder.where('C', controller(blocks(core)))
                .where('W', wall)
                .where('S', cell)
                .build();
    }

    public static List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition) {
        var result = new ArrayList<MultiblockShapeInfo>();
        // Representative face, floor, ceiling, corner and larger layouts; never enumerate every
        // cuboid.
        for (Shape shape :
                List.of(
                        DEFAULT,
                        new Shape(1, 1, 1, 1, 0, 2),
                        new Shape(1, 1, 2, 0, 1, 1),
                        new Shape(1, 1, 0, 2, 1, 1),
                        new Shape(0, 2, 0, 2, 0, 2),
                        new Shape(2, 2, 2, 2, 0, 4),
                        new Shape(4, 4, 4, 4, 0, 8),
                        new Shape(7, 7, 7, 7, 0, 14))) {
            int[] repetitions = new int[shape.depth()];
            java.util.Arrays.fill(repetitions, 1);
            result.add(new MultiblockShapeInfo(pattern(definition, shape).getPreview(repetitions)));
        }
        return result;
    }
}
