package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * [57] Nocturnal (夜行性) — 夜間、 攻撃力・移動速度・ダメージ軽減を獲得し緩やかな回復を得る。
 *
 * <p>夜判定: world day time が [13000, 23000) (= 夜サイクル)。 20t おきに on/off 切替。
 * <p>buff: ATK +50% × lv、 SPEED +30% × lv、 L2DT REDUCTION +0.1 × lv (= 10%/lv 軽減)。
 * <p>regen: 1 秒に maxHP × 1% × lv 回復。
 */
public class NocturnalTrait extends MobTrait {

    private static final UUID MOD_ATK = UUID.fromString("a17e8f51-1c4e-4a7b-9c0a-7e8f1d2c3b41");
    private static final UUID MOD_SPEED = UUID.fromString("a17e8f51-1c4e-4a7b-9c0a-7e8f1d2c3b42");
    private static final UUID MOD_RED = UUID.fromString("a17e8f51-1c4e-4a7b-9c0a-7e8f1d2c3b43");

    public NocturnalTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 20 != 0) return;
        long t = mob.level().getDayTime() % 24000L;
        boolean night = t >= 13000 && t < 23000;
        toggleMod(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.nocturnal_atk",
                0.25 + 0.25 * level, AttributeModifier.Operation.MULTIPLY_BASE, night);  // (25 + 25n)%
        toggleMod(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, "gtcsolo.nocturnal_speed",
                0.25, AttributeModifier.Operation.MULTIPLY_BASE, night);  // 固定 +25%
        toggleMod(mob, L2DamageTracker.REDUCTION.get(), MOD_RED, "gtcsolo.nocturnal_reduction",
                0.10 + 0.05 * level, AttributeModifier.Operation.ADDITION, night);  // (10 + 5n)%
        if (night && mob.getHealth() < mob.getMaxHealth()) {
            mob.heal((float) level);  // 0.5N ハート/秒 = N HP/秒
        }
    }

    private static void toggleMod(LivingEntity mob, Attribute attr, UUID id, String name,
                                   double amount, AttributeModifier.Operation op, boolean on) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (on && cur == null) {
            inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
        } else if (!on && cur != null) {
            inst.removeModifier(id);
        }
    }
}
