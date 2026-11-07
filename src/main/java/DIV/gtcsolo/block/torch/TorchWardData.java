package DIV.gtcsolo.block.torch;

import DIV.gtcsolo.registry.VoltageTorches;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension persistent wards. An unloaded source remains effective without loading its chunk. */
public final class TorchWardData extends SavedData {
    private static final String NAME = "gtcsolo_voltage_torches";
    private final TorchWardIndex index = new TorchWardIndex();

    public static TorchWardData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TorchWardData::load, TorchWardData::new, NAME);
    }

    public void put(BlockPos position, int tier) {
        if (index.put(position.getX(), position.getY(), position.getZ(), tier)) setDirty();
    }

    public void remove(BlockPos position) {
        if (index.remove(position.getX(), position.getY(), position.getZ())) setDirty();
    }

    public boolean blocksSpawn(ServerLevel level, double x, double y, double z) {
        for (var ward : index.covering(x, y, z)) {
            var p = ward.position();
            // getChunkNow never requests an unloaded source chunk or changes its tickets.
            LevelChunk chunk = level.getChunkSource().getChunkNow(p.x() >> 4, p.z() >> 4);
            if (chunk == null) return true;
            BlockPos position = new BlockPos(p.x(), p.y(), p.z());
            int actual = VoltageTorches.tier(chunk.getBlockState(position));
            if (actual == 0) {
                remove(position);
                continue;
            }
            if (actual != ward.tier()) {
                put(position, actual);
                if (!new TorchWardIndex.Ward(p, actual).contains(x, y, z)) continue;
            }
            return true;
        }
        return false;
    }

    /** Repair persisted entries after chunk restoration/editing; block entities also re-register on load. */
    public void reconcile(LevelChunk chunk) {
        for (var ward : index.inChunk(chunk.getPos().x, chunk.getPos().z)) {
            var p = ward.position();
            BlockPos position = new BlockPos(p.x(), p.y(), p.z());
            int tier = VoltageTorches.tier(chunk.getBlockState(position));
            if (tier == 0) remove(position);
            else put(position, tier);
        }
        for (BlockPos position : chunk.getBlockEntitiesPos()) {
            int tier = VoltageTorches.tier(chunk.getBlockState(position));
            if (tier != 0) put(position, tier);
        }
    }

    public static TorchWardData load(CompoundTag tag) {
        TorchWardData data = new TorchWardData();
        ListTag wards = tag.getList("Torches", Tag.TAG_COMPOUND);
        for (int i = 0; i < wards.size(); i++) {
            CompoundTag entry = wards.getCompound(i);
            int tier = entry.getInt("Tier");
            if (!entry.contains("Pos", Tag.TAG_LONG)
                    || tier < VoltageTorchTier.MIN_LEVEL || tier > VoltageTorchTier.MAX_LEVEL) {
                data.setDirty();
                continue;
            }
            BlockPos position = BlockPos.of(entry.getLong("Pos"));
            data.index.put(position.getX(), position.getY(), position.getZ(), tier);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag wards = new ListTag();
        for (var ward : index.all()) {
            var p = ward.position();
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", BlockPos.asLong(p.x(), p.y(), p.z()));
            entry.putInt("Tier", ward.tier());
            wards.add(entry);
        }
        tag.put("Torches", wards);
        return tag;
    }
}
