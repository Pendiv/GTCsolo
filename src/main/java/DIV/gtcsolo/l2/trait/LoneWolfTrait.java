package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 *   <li>MAX_HEALTH は HP 割合を保存して toggle (= 往復で激変 / 無料回復しない)</li>
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
        double buff = BUFF_PER_LEVEL * level;
        L2TraitAttributes.togglePermanent(mob, Attributes.ATTACK_DAMAGE, MOD_ATTACK, "gtcsolo.lone_wolf.attack",
                buff, AttributeModifier.Operation.MULTIPLY_BASE, alone);
        // MAX_HEALTH は HP 割合を保存して更新 (= 往復で HP バーが激変 / 無料回復するのを防ぐ。 最大体力増加時も割合維持)
        L2TraitAttributes.setMaxHealthMultPreservingRatio(mob, MOD_HEALTH, "gtcsolo.lone_wolf.health",
                alone ? buff : 0);
    }
}
