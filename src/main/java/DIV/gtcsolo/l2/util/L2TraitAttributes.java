package DIV.gtcsolo.l2.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * L2H MobTrait が AttributeModifier を扱う際の共通ヘルパ。
 *
 * <p>各 trait に散在していた {@code getAttribute → null 判定 → getModifier → addXxxModifier} の
 * 定型を集約する。 modifier の付与流儀は次の規約に統一する:
 * <ul>
 *   <li><b>permanent</b> (= NBT 保存): postInit で一度だけ付ける恒久バフ。 reload 後も保持され、
 *       postInit 再実行時は UUID guard で多重を防ぐ。 → {@link #addPermanentIfAbsent}</li>
 *   <li><b>transient</b> (= reload で消滅): 毎 tick / 動的に付け替えるバフ。 NBT を汚さず remove 漏れの
 *       残留も起きない。 → {@link #setTransient}</li>
 * </ul>
 *
 * <p>{@code amount == 0} を渡した set 系は modifier 除去として扱う (= 「この modifier を amount にする、
 * 0 なら無し」 という直感的な意味)。
 */
public final class L2TraitAttributes {

    private L2TraitAttributes() {}

    /**
     * 未付与の時だけ permanent で付与する (= 冪等)。 既存値は更新しない。
     * postInit の恒久バフ向け (reload 時の再実行で多重付与されない)。
     */
    public static void addPermanentIfAbsent(LivingEntity e, Attribute attr, UUID id, String name,
                                            double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null || inst.getModifier(id) != null) return;
        inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    /**
     * permanent modifier を amount で付け直す (= remove → add)。 累積/更新するバフ向け。
     * {@code amount == 0} なら除去のみ。
     */
    public static void setPermanent(LivingEntity e, Attribute attr, UUID id, String name,
                                    double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    /**
     * transient modifier を amount で付け直す (= remove → add)。 毎 tick 付け替える動的バフ向け。
     * reload で自動消滅し NBT を汚さない。 {@code amount == 0} なら除去のみ。
     */
    public static void setTransient(LivingEntity e, Attribute attr, UUID id, String name,
                                    double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addTransientModifier(new AttributeModifier(id, name, amount, op));
    }

    /** modifier を除去 (無ければ no-op)。 */
    public static void remove(LivingEntity e, Attribute attr, UUID id) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }

    /**
     * on/off で permanent modifier を付け外す (= 状態変化時のみ作用、 冪等)。
     * 時間帯バフ (Nocturnal/Diurnal) 等の「条件成立中だけ有効」 なバフ向け。
     */
    public static void togglePermanent(LivingEntity e, Attribute attr, UUID id, String name,
                                       double amount, AttributeModifier.Operation op, boolean on) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        boolean has = inst.getModifier(id) != null;
        if (on && !has) {
            inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
        } else if (!on && has) {
            inst.removeModifier(id);
        }
    }

    /**
     * MAX_HEALTH の MULTIPLY_BASE modifier を、 <b>HP 割合を保存したまま</b> amount に更新する。
     * (= 最大体力が増減しても見た目の HP 割合を維持し、 往復による無料回復/激減を防ぐ)
     *
     * <p>{@code amount == 0} なら modifier 除去。 既に目的値なら何もしない (= 冪等、 毎 tick 呼んでも安全)。
     */
    public static void setMaxHealthMultPreservingRatio(LivingEntity e, UUID id, String name, double amount) {
        AttributeInstance inst = e.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        double cur = existing == null ? 0.0 : existing.getAmount();
        if (cur == amount) return;  // 既に目的値 (0 同士含む) — 巻き戻し・再設定しない
        float ratio = e.getMaxHealth() > 0 ? e.getHealth() / e.getMaxHealth() : 1f;
        inst.removeModifier(id);
        if (amount != 0.0) {
            inst.addPermanentModifier(new AttributeModifier(id, name, amount,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        e.setHealth(ratio * e.getMaxHealth());
    }
}
