package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 時空の骨を拾う者 (Spacetime Bone Picker) — 近隣の時空タイプ mob が死ぬたびに、 攻撃力・最大 HP が
 * 上昇し最大 HP の一定割合を回復する。 効果は累積する (= 拾った骨の数 = stack)。
 *
 * <p>stack ごとに 攻撃力 +5n% / 最大 HP +10% (MULTIPLY_BASE)、 死亡 1 回につき最大 HP の 10% 回復。
 * <p>最大 HP 増加時は HP 割合を保存する (= バーが激変しない)。
 */
public class SpacetimeBonePickerTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_ATK = UUID.fromString("9f3a6c2e-4d1b-4a8f-bc57-2e9d1f6a3b84");
    private static final UUID MOD_HP = UUID.fromString("9f3a6c2e-4d1b-4a8f-bc57-2e9d1f6a3b85");
    private static final String COUNT_KEY = "gtcsolo.spacetime_bone_picker_count";
    private static final double RADIUS = 16.0;
    private static final double ATK_PER_STACK_PER_LEVEL = 0.05;  // +5n%/stack
    private static final double HP_PER_STACK = 0.10;             // 最大 HP +10%/stack
    private static final float HEAL_PCT = 0.10f;                 // 拾うたび最大 HP 10% 回復
    private static final int COUNT_CAP = 40;

    public SpacetimeBonePickerTrait(ChatFormatting style) {
        super(style);
    }

    /** どの mob でも死んだら呼ばれる。 死者が時空 mob なら近隣の本 trait 保持者を buff。 */
    public static void onAnyDeath(LivingEntity dead) {
        if (dead.level().isClientSide()) return;
        if (!SpacetimeTraits.isSpacetimeMob(dead)) return;
        AABB area = dead.getBoundingBox().inflate(RADIUS);
        for (LivingEntity e : dead.level().getEntitiesOfClass(LivingEntity.class, area, x -> x != dead && x.isAlive())) {
            if (!MobTraitCap.HOLDER.isProper(e)) continue;
            int lv = MobTraitCap.HOLDER.get(e).getTraitLevel(ModL2Traits.SPACETIME_BONE_PICKER.get());
            if (lv <= 0) continue;
            applyGain(e, lv);
        }
    }

    private static void applyGain(LivingEntity mob, int level) {
        var pdata = mob.getPersistentData();
        int count = Math.min(pdata.getInt(COUNT_KEY) + 1, COUNT_CAP);
        pdata.putInt(COUNT_KEY, count);
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            atk.removeModifier(MOD_ATK);
            atk.addPermanentModifier(new AttributeModifier(MOD_ATK, "gtcsolo.spacetime_bone_picker_atk",
                    count * ATK_PER_STACK_PER_LEVEL * level, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            float ratio = mob.getMaxHealth() > 0 ? mob.getHealth() / mob.getMaxHealth() : 1f;
            hp.removeModifier(MOD_HP);
            hp.addPermanentModifier(new AttributeModifier(MOD_HP, "gtcsolo.spacetime_bone_picker_hp",
                    count * HP_PER_STACK, AttributeModifier.Operation.MULTIPLY_BASE));
            mob.setHealth(ratio * mob.getMaxHealth());  // 割合維持
        }
        mob.heal(mob.getMaxHealth() * HEAL_PCT);
    }
}
