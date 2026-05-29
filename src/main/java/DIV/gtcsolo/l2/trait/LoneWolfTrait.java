package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 孤軍奮闘 (Lone Wolf) — 周囲に MOB が居ない時パワーアップ。
 *
 * <p>仕様:
 * <ul>
 *   <li>{@link #RADIUS} = 16 ブロック内に他 mob が居ないなら attribute modifier 付与</li>
 *   <li>buff = ATTACK_DAMAGE + MAX_HEALTH に各 +25% × lv (MULTIPLY_BASE)</li>
 *   <li>MAX_HEALTH は toggle 時に HP 割合を保存 (= 往復で激変 / 無料回復しない)</li>
 *   <li>tick で動的判定、 mob が来たら modifier remove</li>
 *   <li>check 間隔 {@link #CHECK_INTERVAL} = 20 tick で thrash 回避</li>
 * </ul>
 */
public class LoneWolfTrait extends MobTrait {

    private static final UUID MOD_ATTACK = UUID.fromString("a8c3f1d2-1a4b-4c7d-9e0f-1a2b3c4d5e6f");
    private static final UUID MOD_HEALTH = UUID.fromString("b9d4e2c3-2b5c-5d8e-af10-2b3c4d5e6f70");
    private static final double RADIUS = 16.0;
    private static final double BUFF_PER_LEVEL = 0.25;
    private static final int CHECK_INTERVAL = 20;

    public LoneWolfTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % CHECK_INTERVAL != 0) return;

        AABB area = mob.getBoundingBox().inflate(RADIUS);
        boolean alone = mob.level().getEntitiesOfClass(Mob.class, area, e -> e != mob).isEmpty();
        applyOrRemove(mob, Attributes.ATTACK_DAMAGE, MOD_ATTACK, "gtcsolo.lone_wolf.attack", alone, BUFF_PER_LEVEL * level);
        // MAX_HEALTH は状態変化時のみ、 HP 割合を保存して toggle
        // (= 往復で HP バーが激変 / 無料回復するのを防ぐ。 最大体力増加時も割合は変えない)
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            boolean has = hp.getModifier(MOD_HEALTH) != null;
            if (alone != has) {
                float ratio = mob.getMaxHealth() > 0 ? mob.getHealth() / mob.getMaxHealth() : 1f;
                if (alone) {
                    hp.addPermanentModifier(new AttributeModifier(MOD_HEALTH, "gtcsolo.lone_wolf.health",
                            BUFF_PER_LEVEL * level, AttributeModifier.Operation.MULTIPLY_BASE));
                } else {
                    hp.removeModifier(MOD_HEALTH);
                }
                mob.setHealth(ratio * mob.getMaxHealth());  // 割合維持
            }
        }
    }

    private void applyOrRemove(LivingEntity mob, net.minecraft.world.entity.ai.attributes.Attribute attr,
                               UUID id, String name, boolean shouldApply, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        boolean has = inst.getModifier(id) != null;
        if (shouldApply && !has) {
            inst.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!shouldApply && has) {
            inst.removeModifier(id);
        }
    }
}
