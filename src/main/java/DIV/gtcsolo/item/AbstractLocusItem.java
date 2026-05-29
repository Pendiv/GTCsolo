package DIV.gtcsolo.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashSet;
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

    // mutable 化 (= KubeJS から軌跡を追加/削除可能にする、 LinkedHashSet で挿入順保持)。
    // 直接書き換えではなく [[registerTrace]] / [[unregisterTrace]] 経由を推奨。
    public static final Set<String> VALID_TRACES = new LinkedHashSet<>();

    static {
        VALID_TRACES.add(SUN);
        VALID_TRACES.add(BROWN_DWARF);
        VALID_TRACES.add(KOI74);
        VALID_TRACES.add(R_ANDROMEDAE);
        VALID_TRACES.add(HD101065);
        VALID_TRACES.add(CEMP_R);
        VALID_TRACES.add(NEUTRON_STAR);
        VALID_TRACES.add(BLACK_HOLE);
    }

    /**
     * 新規 trace を登録する (= KubeJS から星を追加する経路)。
     * 重複は無害 (LinkedHashSet.add は既存ならスキップ)。 これ単体では StarForge ロジックに登録されない、
     * 別途 {@code StarForgeTraceData.register} で TraceInfo を入れる必要あり。
     *
     * @return 新規追加なら true、 既存なら false
     */
    public static boolean registerTrace(String traceKey) {
        if (traceKey == null || traceKey.isEmpty()) return false;
        return VALID_TRACES.add(traceKey);
    }

    /** trace を削除する (= 組み込み軌跡を無効化したい時用)。 */
    public static boolean unregisterTrace(String traceKey) {
        return VALID_TRACES.remove(traceKey);
    }

    /** 外部から読み取り専用で trace 一覧を取りたい場合のビュー。 */
    public static Set<String> validTracesView() {
        return Collections.unmodifiableSet(VALID_TRACES);
    }

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
