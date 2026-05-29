package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 時空の献身 (Spacetime Devotion) — 最大 HP +50% を得て、 継続的な割合回復を持つ。 満タン超過分は
 * リンクした時空タイプ味方のうち最も HP の低い対象へ回す。 リンク味方が 50% を下回ると自 HP を消費して
 * 50% へ漸近させ補填する (= 補填量の (450-50N)% を自身が消費)。
 *
 * <p>回復 = 最大 HP × (0.8 + 0.2n)%/秒。 補填は不足分を上限に毎秒一部供給 (= 50% へ漸近)。
 */
public class SpacetimeDevotionTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_HP = UUID.fromString("3e7c2a95-1d4b-4f80-9c26-5a3e1f8c2d70");
    private static final int INTERVAL = 20;
    private static final double LINK_RADIUS = 12.0;
    private static final float REGEN_BASE = 0.008f;            // (0.8 + 0.2n)%/秒
    private static final float REGEN_PER_LEVEL = 0.002f;
    private static final float SUPPLY_PCT_PER_LEVEL = 0.02f;   // 毎秒供給上限 = 最大 HP 2%/level
    private static final float COST_MULT_MIN = 0.5f;           // 自己消費倍率の下限

    public SpacetimeDevotionTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(MOD_HP) == null) {
            hp.addPermanentModifier(new AttributeModifier(MOD_HP, "gtcsolo.spacetime_devotion_hp",
                    0.5, AttributeModifier.Operation.MULTIPLY_BASE));  // 最大 HP +50%
        }
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;

        float regen = mob.getMaxHealth() * (REGEN_BASE + REGEN_PER_LEVEL * level);

        LivingEntity healTarget = null;   // 超過回復の振り向け先 (hp<max で最も低割合)
        LivingEntity needy = null;        // 50% 未満の最困窮リンク先
        float healTargetRatio = Float.MAX_VALUE;
        float needyRatio = Float.MAX_VALUE;
        AABB area = mob.getBoundingBox().inflate(LINK_RADIUS);
        for (LivingEntity ally : mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            if (ally instanceof Player) continue;
            if (!SpacetimeTraits.isSpacetimeMob(ally)) continue;
            float ratio = ally.getHealth() / ally.getMaxHealth();
            if (ally.getHealth() < ally.getMaxHealth() && ratio < healTargetRatio) {
                healTargetRatio = ratio;
                healTarget = ally;
            }
            if (ratio < 0.5f && ratio < needyRatio) {
                needyRatio = ratio;
                needy = ally;
            }
        }

        // 1) 割合回復。 満タンなら超過分をリンク味方へ
        if (mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(regen);
        } else if (healTarget != null) {
            healTarget.heal(regen);
        }

        // 2) 50% 未満のリンク先へ自 HP 供給 (= 不足分を上限に漸近、 補填の (450-50N)% を自身が消費)
        if (needy != null) {
            float deficit = needy.getMaxHealth() * 0.5f - needy.getHealth();
            if (deficit > 0f) {
                float give = Math.min(needy.getMaxHealth() * SUPPLY_PCT_PER_LEVEL * level, deficit);
                float costMult = Math.max(COST_MULT_MIN, 4.5f - 0.5f * level);  // (450-50N)%
                float cost = give * costMult;
                if (mob.getHealth() - cost > 1f) {
                    needy.heal(give);
                    mob.setHealth(mob.getHealth() - cost);
                }
            }
        }
    }
}
