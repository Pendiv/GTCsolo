package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 観客効果 (Audience Effect) — 周囲に player がいない間、 30 秒ごとに強化 stack を獲得する。
 *
 * <p>仕様:
 * <ul>
 *   <li>{@link #RADIUS} = 24 ブロック内に player 不在で stack 蓄積、 player が来たら stack=0</li>
 *   <li>{@link #GAIN_INTERVAL} = 600t (30 秒) ごとに 1 stack、 上限 (3 + n) stack</li>
 *   <li>stack ごとに ATTACK_DAMAGE / MAX_HEALTH / JUMP_STRENGTH を +(10 + 2n)% (MULTIPLY_BASE)</li>
 *   <li>MAX_HEALTH は HP 割合保存で更新。 JUMP_STRENGTH は当該 attribute を持つ mob (馬等) のみ有効</li>
 * </ul>
 */
public class AudienceEffectTrait extends MobTrait {

    private static final UUID MOD_ATTACK = UUID.fromString("c1e4a3d2-3c6d-6e9f-bf21-3c4d5e6f7081");
    private static final UUID MOD_HEALTH = UUID.fromString("d2f5b4e3-4d7e-7fa0-cf32-4d5e6f708192");
    private static final UUID MOD_JUMP = UUID.fromString("e3061c5f-5e8f-8fb1-df43-5e6f708192a3");
    private static final double RADIUS = 24.0;
    private static final int CHECK_INTERVAL = 20;
    private static final int GAIN_INTERVAL = 600;  // 30 秒で 1 stack

    public AudienceEffectTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % CHECK_INTERVAL != 0) return;
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);

        boolean unwatched = mob.level().getNearestPlayer(mob, RADIUS) == null;
        int maxStacks = 3 + level;

        if (!unwatched) {
            if (data.stacks != 0) {
                data.stacks = 0;
                applyStacks(mob, level, 0);
            }
            data.lastGain = mob.tickCount;
            return;
        }
        if (mob.tickCount - data.lastGain >= GAIN_INTERVAL && data.stacks < maxStacks) {
            data.stacks++;
            data.lastGain = mob.tickCount;
            applyStacks(mob, level, data.stacks);
        }
    }

    private void applyStacks(LivingEntity mob, int level, int stacks) {
        double amount = stacks * (0.10 + 0.02 * level);  // stack 数 × (10 + 2n)%
        setMod(mob, Attributes.ATTACK_DAMAGE, MOD_ATTACK, "gtcsolo.audience_effect.attack", amount);
        setMod(mob, Attributes.JUMP_STRENGTH, MOD_JUMP, "gtcsolo.audience_effect.jump", amount);
        setHealthMod(mob, amount);  // MAX_HEALTH は HP 割合保存
    }

    private static void setMod(LivingEntity mob, Attribute attr, UUID id, String name, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        if (inst.getModifier(id) != null) inst.removeModifier(id);
        if (amount > 0) {
            inst.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void setHealthMod(LivingEntity mob, double amount) {
        AttributeInstance inst = mob.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        float ratio = mob.getMaxHealth() > 0 ? mob.getHealth() / mob.getMaxHealth() : 1f;
        if (inst.getModifier(MOD_HEALTH) != null) inst.removeModifier(MOD_HEALTH);
        if (amount > 0) {
            inst.addPermanentModifier(new AttributeModifier(MOD_HEALTH, "gtcsolo.audience_effect.health",
                    amount, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        mob.setHealth(ratio * mob.getMaxHealth());
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField public int stacks = 0;
        @SerialClass.SerialField public int lastGain = 0;
    }
}
