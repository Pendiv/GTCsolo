package DIV.gtcsolo.machine.mega.dismantle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/** A durable snapshot of matching blocks, not a bounding-box deletion instruction. */
public final class DismantleJob {
    public interface Access {
        boolean loaded(long position);
        String block(long position);
        boolean permitted(long position);
        boolean remove(long position);
    }

    public final UUID id, owner;
    public final String dimension;
    public final long core;
    public final long[] positions;
    public final int[] materials;
    public final List<String> palette;
    public int cursor, removed, skipped;

    public DismantleJob(UUID id,UUID owner,String dimension,long core,long[] positions,int[] materials,List<String> palette) {
        new ResourceLocation(dimension);
        if(positions.length!=materials.length || positions.length<1 || positions.length>2_000_000 || palette.isEmpty())
            throw new IllegalArgumentException("Invalid dismantling snapshot");
        for(String block:palette) {
            new ResourceLocation(block);
            if(block.equals("minecraft:air")) throw new IllegalArgumentException("Air is not a dismantling target");
        }
        for(int material:materials) if(material<0 || material>=palette.size()) throw new IllegalArgumentException("Invalid material index");
        this.id=id;this.owner=owner;this.dimension=dimension;this.core=core;
        this.positions=positions;this.materials=materials;this.palette=List.copyOf(palette);
    }

    public boolean done() { return cursor==positions.length; }

    public void step(Access access,int maximum,long deadline) {
        for(int processed=0;!done() && processed<maximum && System.nanoTime()<deadline;processed++) {
            long position=positions[cursor];
            if(!access.loaded(position)) return;
            String expected=palette.get(materials[cursor]);
            if(!expected.equals(access.block(position)) || !access.permitted(position)
                    || !expected.equals(access.block(position))) skipped++;
            else if(access.remove(position)) removed++;
            else skipped++;
            cursor++;
        }
    }

    public CompoundTag save() {
        var tag=new CompoundTag();tag.putInt("Version",1);tag.putUUID("Id",id);tag.putUUID("Owner",owner);
        tag.putString("Dimension",dimension);tag.putLong("Core",core);tag.putLongArray("Positions",positions);
        tag.putIntArray("Materials",materials);var names=new ListTag();palette.forEach(s->names.add(StringTag.valueOf(s)));
        tag.put("Palette",names);tag.putInt("Cursor",cursor);tag.putInt("Removed",removed);tag.putInt("Skipped",skipped);return tag;
    }

    public static DismantleJob load(CompoundTag tag) {
        if(tag.getInt("Version")!=1) throw new IllegalArgumentException("Unknown dismantling snapshot version");
        var names=tag.getList("Palette",Tag.TAG_STRING);var palette=new java.util.ArrayList<String>();
        for(int i=0;i<names.size();i++) palette.add(names.getString(i));
        var job=new DismantleJob(tag.getUUID("Id"),tag.getUUID("Owner"),tag.getString("Dimension"),tag.getLong("Core"),
                tag.getLongArray("Positions"),tag.getIntArray("Materials"),palette);
        job.cursor=tag.getInt("Cursor");job.removed=tag.getInt("Removed");job.skipped=tag.getInt("Skipped");
        if(job.cursor<0 || job.cursor>job.positions.length || job.removed<0 || job.skipped<0
                || (long)job.removed+job.skipped!=job.cursor) throw new IllegalArgumentException("Invalid dismantling progress");
        return job;
    }
}
