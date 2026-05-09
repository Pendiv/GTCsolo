package DIV.gtcsolo.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * StarLocus / DecayingStarLocus の共通基底。
 * NBT "Trace" で 8 軌跡を識別する仕組みを共通化する。
 */
public abstract class AbstractLocusItem extends Item {

    public static final String NBT_KEY = "Trace";

    public static final String SUN          = "sun";
    public static final String BROWN_DWARF  = "brown_dwarf";
    public static final String KOI74        = "koi74";
    public static final String R_ANDROMEDAE = "r_andromedae";
    public static final String HD101065     = "hd101065";
    public static final String CEMP_R       = "cemp_r";
    public static final String NEUTRON_STAR = "neutron_star";
    public static final String BLACK_HOLE   = "black_hole";

    public static final Set<String> VALID_TRACES = Set.of(
            SUN, BROWN_DWARF, KOI74, R_ANDROMEDAE,
            HD101065, CEMP_R, NEUTRON_STAR, BLACK_HOLE
    );

    public AbstractLocusItem(Properties props) {
        super(props);
    }

    public static String getTrace(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY, Tag.TAG_STRING)) return null;
        return tag.getString(NBT_KEY);
    }

    public static void setTrace(ItemStack stack, String traceKey) {
        stack.getOrCreateTag().putString(NBT_KEY, traceKey);
    }

    public static boolean isEmpty(ItemStack stack) {
        return getTrace(stack) == null;
    }

    public static boolean isValidTrace(String traceKey) {
        return traceKey != null && VALID_TRACES.contains(traceKey);
    }

    public static ItemStack of(Item item, String traceKey) {
        ItemStack s = new ItemStack(item);
        if (traceKey != null) setTrace(s, traceKey);
        return s;
    }

    /** 翻訳キーの prefix (例: "item.gtcsolo.star_locus") */
    protected abstract String getTranslationKeyPrefix();

    @Override
    public Component getName(ItemStack stack) {
        String trace = getTrace(stack);
        String key = (trace == null)
                ? getTranslationKeyPrefix() + ".empty"
                : getTranslationKeyPrefix() + "." + trace;
        return Component.translatable(key);
    }
}
