package DIV.gtcsolo.machine.mega.dismantle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;

public final class MegaDismantleData extends SavedData {
    public final List<DismantleJob> jobs=new ArrayList<>();
    public static MegaDismantleData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MegaDismantleData::load,MegaDismantleData::new,"gtcsolo_mega_dismantling");
    }
    public static MegaDismantleData load(CompoundTag tag) {
        var data=new MegaDismantleData();var jobs=tag.getList("Jobs",Tag.TAG_COMPOUND);
        for(int i=0;i<jobs.size();i++) data.jobs.add(DismantleJob.load(jobs.getCompound(i)));
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var jobs=new ListTag();this.jobs.forEach(job->jobs.add(job.save()));tag.put("Jobs",jobs);return tag;
    }
}
