package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * [52] All or Nothing (一発逆転) — 移動速度 -95%、 攻撃が防御貫通かつダメージ 2 倍。
 *
 * <p>動きの遅い高火力砲台。 間合い管理を要求する。
 * <p>移動速度: MULTIPLY_TOTAL × -0.95 (lv 不変、 常時 95% 減)。
 * <p>攻撃強化: onHurtTarget で `amount = (amount + target_armor) × 2`
 * (= 防御で削られた分を加算で戻して 2 倍 = 防御貫通 + 2x の近似)。
 */
public class AllOrNothingTrait extends MobTrait {

    private static final UUID MOD_SPEED = UUID.fromString("a110a110-d1c0-4f37-9b6a-7d8e1c2a3b4d");
    private static final double SPEED_REDUCTION = -0.95;
    private static final float DAMAGE_MULT = 2.0f;

    public AllOrNothingTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, "gtcsolo.all_or_nothing",
                SPEED_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        LivingHurtEvent event = cache.getLivingHurtEvent();
        if (event == null) return;
        LivingEntity target = traitCache.target;
        if (target == null) return;
        // 防御貫通近似: post-armor の amount に target.armor を足し戻して pre-armor 値に近づける
        double armor = target.getArmorValue();
        double rawApprox = event.getAmount() + armor;
        event.setAmount((float) (rawApprox * DAMAGE_MULT));
    }
}
