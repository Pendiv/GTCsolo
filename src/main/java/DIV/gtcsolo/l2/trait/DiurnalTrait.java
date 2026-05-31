package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 昼行性 (Diurnal) — 昼間、 攻撃力・移動速度・ダメージ軽減を獲得し緩やかに回復する。
 * {@link NocturnalTrait} と倍率同一、 発動条件 (昼/夜) のみ反転。
 *
 * <p>昼判定: world day time が [0, 13000) または [23000, 24000)。 20t おきに on/off。
 * <p>buff: ATK +(25 + 25n)%、 SPEED 固定 +25%、 REDUCTION +(10 + 5n)%。 regen N HP/秒。
 */
public class DiurnalTrait extends MobTrait {

    private static final UUID MOD_ATK = UUID.fromString("b28f9a62-2d5f-5b8c-ad1b-8f9a2e3d4c50");
    private static final UUID MOD_SPEED = UUID.fromString("b28f9a62-2d5f-5b8c-ad1b-8f9a2e3d4c51");
    private static final UUID MOD_RED = UUID.fromString("b28f9a62-2d5f-5b8c-ad1b-8f9a2e3d4c52");

    public DiurnalTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 20 != 0) return;
        long t = mob.level().getDayTime() % 24000L;
        boolean day = t < 13000 || t >= 23000;
        L2TraitAttributes.togglePermanent(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.diurnal_atk",
                0.25 + 0.25 * level, AttributeModifier.Operation.MULTIPLY_BASE, day);  // (25 + 25n)%
        L2TraitAttributes.togglePermanent(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, "gtcsolo.diurnal_speed",
                0.25, AttributeModifier.Operation.MULTIPLY_BASE, day);  // 固定 +25%
        L2TraitAttributes.togglePermanent(mob, L2DamageTracker.REDUCTION.get(), MOD_RED, "gtcsolo.diurnal_reduction",
                0.10 + 0.05 * level, AttributeModifier.Operation.ADDITION, day);  // (10 + 5n)%
        if (day && mob.getHealth() < mob.getMaxHealth()) {
            mob.heal((float) level);  // N HP/秒
        }
    }
}
