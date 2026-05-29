package DIV.gtcsolo.l2.effect;

import DIV.gtcsolo.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 隠蔽 (Concealment) — lvl ごと 1 ハート (= 2 HP) の最大体力を減少させる debuff。
 *
 * <p>仕様 (2026-05-28 確定):
 * <ul>
 *   <li>MAX_HEALTH に -2.0 × (amplifier+1) の ADDITION modifier (= lvl 1 で 1 ハート減)</li>
 *   <li>効果時間中、 <b>MobEffect 由来でない</b> 回復をすると、 回復ハート分 (= 2 HP ごと)
 *       エフェクト level を減少させる。 付与時間 (duration) は変わらない</li>
 *   <li>regeneration 等の MobEffect 回復は除外 (= entity.hasEffect(REGENERATION) 中は skip)</li>
 *   <li>端数 (= 2 HP 未満) は entity persistentData に累積、 次回繰り越し</li>
 * </ul>
 *
 * <p>level 減少は {@link #onHeal} を {@code L2EventHandlers.onHeal} から呼んで実現。
 * amplifier 変更は removeEffect → addEffect (= duration 維持) で行う。
 */
public class ConcealmentEffect extends MobEffect {

    private static final String MAX_HP_MODIFIER_UUID = "c7d2e8a3-4b1f-4c6e-9a05-7d3e1f8c2b4d";
    private static final String HEAL_ACCUM_KEY = "gtcsolo.concealment_heal_accum";

    public ConcealmentEffect() {
        super(MobEffectCategory.HARMFUL, 0x2a1a3a);
        addAttributeModifier(Attributes.MAX_HEALTH, MAX_HP_MODIFIER_UUID, -2.0,
                AttributeModifier.Operation.ADDITION);
    }

    /**
     * MobEffect 由来でない回復が起きた時に呼ばれ、 回復量に応じて隠蔽 level を減らす。
     * 2 HP (= 1 ハート) 回復ごとに amplifier 1 減、 端数は persistentData に累積。
     */
    public static void onHeal(LivingEntity entity, float healAmount) {
        if (entity.level().isClientSide()) return;
        if (healAmount <= 0f) return;
        MobEffectInstance conceal = entity.getEffect(ModEffects.CONCEALMENT.get());
        if (conceal == null) return;
        // MobEffect 由来回復は除外 (= regeneration 等を持っている間の heal は対象外)
        if (entity.hasEffect(MobEffects.REGENERATION)) return;

        var pdata = entity.getPersistentData();
        float accum = pdata.getFloat(HEAL_ACCUM_KEY) + healAmount;
        int reduce = (int) (accum / 2.0f);
        if (reduce > 0) {
            accum -= reduce * 2.0f;
            int newAmp = conceal.getAmplifier() - reduce;
            int dur = conceal.getDuration();
            entity.removeEffect(ModEffects.CONCEALMENT.get());
            if (newAmp >= 0) {
                entity.addEffect(new MobEffectInstance(ModEffects.CONCEALMENT.get(), dur, newAmp));
            } else {
                pdata.putFloat(HEAL_ACCUM_KEY, 0f);
                return;
            }
        }
        pdata.putFloat(HEAL_ACCUM_KEY, accum);
    }
}
