package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 時空の覇者 (Spacetime Conqueror) — 近くに他の時空タイプ mob が存在できない。 代わりに全ステータスが大幅上昇。
 *
 * <p>攻撃力 +(75+25N)% / 防御力(ARMOR) +(225+75N)% / 移動速度 +(25+6.25n)% (MULTIPLY_BASE)、
 * 防具値 +10(N-1) 実数 (ADDITION)、 ダメージ軽減 +20% 固定 (L2DT REDUCTION ADDITION)。
 * <p>「存在できない」 = 範囲内の他時空 mob を毎秒 discard で表現。
 */
public class SpacetimeConquerorTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_ATK = UUID.fromString("7a2e9c41-3b6d-4f18-8e57-1d9a2c6f3b41");
    private static final UUID MOD_SPEED = UUID.fromString("7a2e9c41-3b6d-4f18-8e57-1d9a2c6f3b42");
    private static final UUID MOD_ARMOR_PCT = UUID.fromString("7a2e9c41-3b6d-4f18-8e57-1d9a2c6f3b43");
    private static final UUID MOD_ARMOR_FLAT = UUID.fromString("7a2e9c41-3b6d-4f18-8e57-1d9a2c6f3b44");
    private static final UUID MOD_RED = UUID.fromString("7a2e9c41-3b6d-4f18-8e57-1d9a2c6f3b45");
    private static final int SCAN_INTERVAL = 20;
    private static final double SCAN_RADIUS = 16.0;

    public SpacetimeConquerorTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.conqueror_atk",
                0.75 + 0.25 * lv, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.ARMOR, MOD_ARMOR_PCT, "gtcsolo.conqueror_armor_pct",
                2.25 + 0.75 * lv, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, "gtcsolo.conqueror_speed",
                0.25 + 0.0625 * lv, AttributeModifier.Operation.MULTIPLY_BASE);
        if (lv > 1) {
            L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.ARMOR, MOD_ARMOR_FLAT, "gtcsolo.conqueror_armor_flat",
                    10.0 * (lv - 1), AttributeModifier.Operation.ADDITION);  // 防具値 +10(N-1) 実数
        }
        L2TraitAttributes.addPermanentIfAbsent(mob, L2DamageTracker.REDUCTION.get(), MOD_RED, "gtcsolo.conqueror_reduction",
                0.20, AttributeModifier.Operation.ADDITION);  // 固定 20% 軽減
        mob.setHealth(mob.getMaxHealth());
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % SCAN_INTERVAL != 0) return;
        AABB area = mob.getBoundingBox().inflate(SCAN_RADIUS);
        for (LivingEntity other : mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            if (SpacetimeTraits.isSpacetimeMob(other)) {
                other.discard();
            }
        }
    }
}
