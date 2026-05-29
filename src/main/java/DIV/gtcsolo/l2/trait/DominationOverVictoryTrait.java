package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import DIV.gtcsolo.l2.util.L2EntityUtil;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [09] Domination over Victory — ボス専用。 与ダメージの一部が防御貫通の確定ダメージになる。
 * 割合は本体の体力が少ないほど大きくなる。
 *
 * <p>追い詰められたボスほど苛烈に。 終盤の圧力源。
 *
 * <p>実装: 防御計算後に純粋ダメージを (与ダメ × 追加率) で加算。
 * 追加率 = (HP を 25% 喪失するごとに 1 段) × 10n%
 *   (例: lv1 で hp 75% 残 → 1 段 → +10%、 hp 0% → 4 段 → +40%、 lv3 で hp 0% → +120%)
 *
 * <p>仕様上ボス専用 = {@link TypedMobTrait#isValidTarget} で {@link L2EntityUtil#isBoss} を必須化。
 */
public class DominationOverVictoryTrait extends TypedMobTrait {

    private static final double HP_TIER = 0.25;          // 25% 刻み
    private static final double PIERCE_PER_TIER = 0.10;  // 1 段につき 10n%

    public DominationOverVictoryTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return L2EntityUtil.isBoss(mob);
    }

    @Override
    protected void onValidHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onValidHurtTarget(level, attacker, cache, traitCache); // 既定 cascade を保持
        LivingHurtEvent event = cache.getLivingHurtEvent();
        if (event == null) return;
        float maxHp = attacker.getMaxHealth();
        if (maxHp <= 0) return;
        double hpRatio = attacker.getHealth() / maxHp;
        int tiers = (int) ((1.0 - hpRatio) / HP_TIER);  // 25% 喪失ごとに 1 段 (0〜4)
        double pierce = tiers * PIERCE_PER_TIER * level;
        event.setAmount(event.getAmount() * (float) (1.0 + pierce));
    }
}
