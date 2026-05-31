package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 時空の共鳴 (Spacetime Resonance) — 周囲の時空タイプ mob に攻撃力上昇と継続回復を付与する。
 * 対象がさらに共鳴を持つ場合は効果が大きく増す (= 共鳴同士の相乗)。
 *
 * <p>1 秒おきに範囲内の時空 mob (= player 除く) へ:
 * <ul>
 *   <li>共鳴を持たない時空 mob: 攻撃力 +2.5N% (MULTIPLY_BASE)、 0.5 ハート/秒 回復</li>
 *   <li>共鳴を持つ時空 mob (自身除く): 攻撃力 +12.5(N+1)% (MULTIPLY_BASE)、 最大 HP 0.38%/秒 回復</li>
 * </ul>
 * <p>攻撃力は固定 UUID の transient modifier を毎秒付け替える (= 範囲外へ出ると最後値が残る点に注意)。
 */
public class SpacetimeResonanceTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_ATK = UUID.fromString("6f1d4a82-2c5e-4b39-9a07-3e8c1f6d2b95");
    private static final int INTERVAL = 20;
    private static final double RADIUS = 8.0;

    public SpacetimeResonanceTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        for (LivingEntity ally : mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            if (ally instanceof Player) continue;
            if (!SpacetimeTraits.isSpacetimeMob(ally)) continue;
            boolean hasResonance = MobTraitCap.HOLDER.isProper(ally)
                    && MobTraitCap.HOLDER.get(ally).getTraitLevel(ModL2Traits.SPACETIME_RESONANCE.get()) > 0;
            double atkPct;
            float heal;
            if (hasResonance) {
                atkPct = 0.125 * (level + 1);             // 12.5(N+1)%
                heal = ally.getMaxHealth() * 0.0038f;      // 最大 HP 0.38%/秒
            } else {
                atkPct = 0.025 * level;                    // 2.5N%
                heal = 1.0f;                               // 0.5 ハート/秒
            }
            L2TraitAttributes.setTransient(ally, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.spacetime_resonance",
                    atkPct, AttributeModifier.Operation.MULTIPLY_BASE);
            if (ally.getHealth() < ally.getMaxHealth()) ally.heal(heal);
        }
    }
}
