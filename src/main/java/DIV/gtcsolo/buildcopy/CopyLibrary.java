package DIV.gtcsolo.buildcopy;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-owned library: items/blocks carry only an immutable UUID, never the potentially large snapshot. */
public final class CopyLibrary extends SavedData {
    private final Map<UUID, CompoundTag> layouts = new HashMap<>();
    public static CopyLibrary get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                CopyLibrary::load, CopyLibrary::new, "gtcsolo_copies");
    }
    private static CopyLibrary load(CompoundTag tag) {
        var library = new CopyLibrary();
        var layouts = tag.getCompound("Layouts");
        for (String key : layouts.getAllKeys()) {
            try { library.layouts.put(UUID.fromString(key), layouts.getCompound(key).copy()); }
            catch (IllegalArgumentException ex) { Gtcsolo.LOGGER.warn("Ignoring invalid copy identifier {}", key); }
        }
        return library;
    }
    public UUID add(CopyLayout layout) {
        UUID id = UUID.randomUUID();
        layouts.put(id, layout.save(true));
        setDirty();
        return id;
    }
    public CopyLayout get(UUID id) {
        var tag = layouts.get(id);
        return tag == null ? null : CopyLayout.load(tag, true);
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var data = new CompoundTag();
        layouts.forEach((id, layout) -> data.put(id.toString(), layout.copy()));
        tag.put("Layouts", data);
        return tag;
    }
}
