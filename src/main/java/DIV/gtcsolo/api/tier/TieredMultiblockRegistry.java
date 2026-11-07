package DIV.gtcsolo.api.tier;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;
import java.util.WeakHashMap;

/**
 * tiered マルチブロックの登録レジストリ。
 *
 * 役割: PatternPreviewWidgetMixin が「この definition は tiered か?」を
 * O(1) で判定するための static map。tiered の場合に対応する {@link TieredBlockSet} を返す。
 * 未登録 (= 通常の MB) は null を返すので mixin 側はそのまま素通りできる。
 *
 * <p>WeakHashMap で持つことで MB 定義が解放されたら参照も切れる (再リロード対応)。
 */
public final class TieredMultiblockRegistry {

    private static final Map<MultiblockMachineDefinition, TieredBlockSet> REGISTRY = new WeakHashMap<>();
    private static final Map<MultiblockMachineDefinition, List<Integer>> PREVIEW_TIERS = new WeakHashMap<>();

    private TieredMultiblockRegistry() {}

    /** 登録: マルチブロック定義 ↔ tier セット。init 時に呼ぶ。 */
    public static void register(MultiblockMachineDefinition def, TieredBlockSet set) {
        if (def == null || set == null) return;
        REGISTRY.put(def, set);
    }

    /** 未登録なら null。tiered ならその tier セット。 */
    @Nullable
    public static TieredBlockSet get(MultiblockMachineDefinition def) {
        return REGISTRY.get(def);
    }

    /** Grade labels for composite structures whose predicates share a numeric tier. */
    public static void registerPreviewTiers(MultiblockMachineDefinition def, List<Integer> tiers) {
        if (def != null) PREVIEW_TIERS.put(def, List.copyOf(tiers));
    }

    public static List<Integer> getPreviewTiers(MultiblockMachineDefinition def) {
        List<Integer> explicit = PREVIEW_TIERS.get(def);
        if (explicit != null) return explicit;
        TieredBlockSet set = get(def);
        return set == null ? List.of() : List.copyOf(set.getTiers());
    }
}
