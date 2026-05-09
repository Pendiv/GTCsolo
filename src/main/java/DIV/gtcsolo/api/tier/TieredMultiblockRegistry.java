package DIV.gtcsolo.api.tier;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
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
}
