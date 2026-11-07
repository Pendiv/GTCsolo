package DIV.gtcsolo.buildcopy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Immutable, palette-compressed block snapshot. Preview serialization deliberately omits BE data. */
public final class CopyLayout {
    public static final int MAX_BLOCKS = 32_768;
    public static final int MAX_PALETTE = 4_096;
    public static final int MAX_SIDE = 128;
    public final String name;
    public final BlockPos size, anchor;
    public final Direction facing;
    private final List<BlockState> palette;
    private final int[] indices;
    private final Map<Integer, CompoundTag> entities;

    public CopyLayout(String name, BlockPos size, BlockPos anchor, Direction facing,
                      List<BlockState> palette, int[] indices, Map<Integer, CompoundTag> entities) {
        int volume = checkedVolume(size);
        if (name.isBlank() || name.length() > 80 || facing.getAxis() == Direction.Axis.Y
                || palette.isEmpty() || palette.size() > MAX_PALETTE || indices.length != volume
                || !inside(anchor, size)) throw new IllegalArgumentException("Invalid copy layout");
        for (int index : indices) if (index < 0 || index >= palette.size())
            throw new IllegalArgumentException("Invalid palette index");
        this.name = name;
        this.size = size.immutable();
        this.anchor = anchor.immutable();
        this.facing = facing;
        this.palette = List.copyOf(palette);
        this.indices = indices.clone();
        Map<Integer, CompoundTag> detached = new HashMap<>();
        entities.forEach((index, data) -> {
            if (index < 0 || index >= volume || !state(index).hasBlockEntity())
                throw new IllegalArgumentException("Invalid block entity index");
            detached.put(index, data.copy());
        });
        this.entities = Collections.unmodifiableMap(detached);
    }

    public static int checkedVolume(BlockPos size) {
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1
                || size.getX() > MAX_SIDE || size.getY() > MAX_SIDE || size.getZ() > MAX_SIDE)
            throw new IllegalArgumentException("Copy dimensions exceed limit");
        long volume = (long) size.getX() * size.getY() * size.getZ();
        if (volume > MAX_BLOCKS) throw new IllegalArgumentException("Copy volume exceeds limit");
        return (int) volume;
    }

    private static boolean inside(BlockPos pos, BlockPos size) {
        return pos.getX() >= 0 && pos.getY() >= 0 && pos.getZ() >= 0
                && pos.getX() < size.getX() && pos.getY() < size.getY() && pos.getZ() < size.getZ();
    }

    public static int encodedBytes(CompoundTag tag) throws java.io.IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var output = new java.io.DataOutputStream(bytes)) { NbtIo.write(tag, output); }
        return bytes.size();
    }

    public int volume() { return indices.length; }
    public BlockState state(int index) { return palette.get(indices[index]); }
    public BlockState rotatedState(int index, Rotation rotation) {
        var state = state(index);
        var result = state.rotate(rotation);
        // GT's native rotate handles the front, but vertical extended-facing machines
        // also need their horizontal "upwards" direction rotated around the world Y axis.
        if (state.getBlock() instanceof com.gregtechceu.gtceu.api.block.MetaMachineBlock machine
                && machine.getRotationState() != com.gregtechceu.gtceu.api.data.RotationState.NONE
                && state.getValue(machine.getRotationState().property).getAxis() == Direction.Axis.Y
                && state.hasProperty(com.gregtechceu.gtceu.api.block.IMachineBlock.UPWARDS_FACING_PROPERTY)) {
            result = result.setValue(com.gregtechceu.gtceu.api.block.IMachineBlock.UPWARDS_FACING_PROPERTY,
                    rotation.rotate(state.getValue(com.gregtechceu.gtceu.api.block.IMachineBlock.UPWARDS_FACING_PROPERTY)));
        }
        return result;
    }
    public CompoundTag entityData(int index) {
        var data = entities.get(index);
        return data == null ? null : data.copy();
    }
    public int index(BlockPos local) {
        return (local.getY() * size.getZ() + local.getZ()) * size.getX() + local.getX();
    }
    public BlockPos local(int index) {
        return new BlockPos(index % size.getX(), index / (size.getX() * size.getZ()),
                (index / size.getX()) % size.getZ());
    }
    public BlockPos target(int index, BlockPos origin, Rotation rotation) {
        return origin.offset(local(index).subtract(anchor).rotate(rotation));
    }
    public Rotation rotationTo(Direction target) {
        if (target == facing) return Rotation.NONE;
        if (target == facing.getClockWise()) return Rotation.CLOCKWISE_90;
        if (target == facing.getOpposite()) return Rotation.CLOCKWISE_180;
        return Rotation.COUNTERCLOCKWISE_90;
    }

    public CompoundTag save(boolean includeEntities) {
        var tag = new CompoundTag();
        tag.putInt("Version", 1);
        tag.putString("Name", name);
        tag.put("Size", NbtUtils.writeBlockPos(size));
        tag.put("Anchor", NbtUtils.writeBlockPos(anchor));
        tag.putInt("Facing", facing.get3DDataValue());
        var states = new ListTag();
        palette.forEach(state -> states.add(NbtUtils.writeBlockState(state)));
        tag.put("Palette", states);
        tag.putIntArray("Blocks", indices);
        if (includeEntities) {
            var list = new ListTag();
            entities.forEach((index, data) -> {
                var entry = new CompoundTag();
                entry.putInt("Index", index);
                entry.put("Data", data.copy());
                list.add(entry);
            });
            tag.put("Entities", list);
        }
        return tag;
    }

    public static CopyLayout load(CompoundTag tag, boolean includeEntities) {
        if (tag.getInt("Version") != 1) throw new IllegalArgumentException("Unsupported copy format");
        var size = NbtUtils.readBlockPos(tag.getCompound("Size"));
        checkedVolume(size);
        var rawPalette = tag.getList("Palette", Tag.TAG_COMPOUND);
        if (rawPalette.size() > MAX_PALETTE) throw new IllegalArgumentException("Too many block states");
        var states = new ArrayList<BlockState>();
        for (var element : rawPalette) {
            var stateTag = (CompoundTag) element;
            var id = ResourceLocation.tryParse(stateTag.getString("Name"));
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id))
                throw new IllegalArgumentException("Missing block: " + stateTag.getString("Name"));
            states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag));
        }
        var entities = new HashMap<Integer, CompoundTag>();
        if (includeEntities) for (var element : tag.getList("Entities", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            if (entities.put(entry.getInt("Index"), entry.getCompound("Data")) != null)
                throw new IllegalArgumentException("Duplicate block entity");
        }
        return new CopyLayout(tag.getString("Name"), size, NbtUtils.readBlockPos(tag.getCompound("Anchor")),
                Direction.from3DDataValue(tag.getInt("Facing")), states, tag.getIntArray("Blocks"), entities);
    }
}
