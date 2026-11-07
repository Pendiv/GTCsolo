package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.utils.ResearchManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Persistent manufacturing experience, separate from GregTech's research payload. */
public final class CircuitResearchStorage {
    public static final String EXPERIENCE = "gtcsolo_circuit_experience";
    private static final ResourceLocation ITEM = new ResourceLocation("gtcsolo", "circuit_research_storage");

    private CircuitResearchStorage() {}

    public static boolean canRecord(ItemStack stack) {
        return stack.getCount() == 1 && ITEM.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static long count(ItemStack stack, String recipe) {
        if (!canRecord(stack) || !stack.hasTag()) return 0;
        var identity = ResearchManager.readResearchId(stack);
        if (identity == null || identity.getFirst() == null) return 0;
        var saved = stack.getTag().getCompound(EXPERIENCE);
        if (!matchesResearch(saved, identity.getFirst().registryName.toString(), identity.getSecond())) return 0;
        return Math.max(0, saved.getCompound("recipes").getLong(recipe));
    }

    /** Called once after a completed batch, never for simulated recipes or bonus outputs. */
    public static boolean complete(ItemStack stack, String recipe) {
        if (!canRecord(stack)) return false;
        var identity = ResearchManager.readResearchId(stack);
        if (identity == null || identity.getFirst() == null) return false;
        var root = stack.getOrCreateTag();
        String type = identity.getFirst().registryName.toString();
        var saved = root.getCompound(EXPERIENCE);
        if (!matchesResearch(saved, type, identity.getSecond())) {
            saved = new CompoundTag();
            saved.putString("research_type", type);
            saved.putString("research_id", identity.getSecond());
            root.put(EXPERIENCE, saved);
        }
        var recipes = saved.getCompound("recipes");
        if (!saved.contains("recipes", Tag.TAG_COMPOUND)) saved.put("recipes", recipes);
        long before = Math.max(0, recipes.getLong(recipe));
        if (before == Long.MAX_VALUE) return false;
        recipes.putLong(recipe, before + 1);
        return true;
    }

    private static boolean matchesResearch(CompoundTag saved, String type, String research) {
        return type.equals(saved.getString("research_type")) && research.equals(saved.getString("research_id"));
    }

    /** The scanner passes an already copied tag; preserve all native data and discard only experience. */
    public static CompoundTag clearCopiedExperience(CompoundTag copied) {
        copied.remove(EXPERIENCE);
        return copied;
    }
}
