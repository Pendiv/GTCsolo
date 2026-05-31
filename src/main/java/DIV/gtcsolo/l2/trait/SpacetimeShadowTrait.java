package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * 時空の影 (Spacetime Shadow) — 高速移動・高跳躍・段差踏破・遠距離探知を持ち透明化する。
 * 透明な間は攻撃力 0。 player が接近すると透明が解け攻撃力が戻る。 ステータスは level 非依存。
 *
 * <p>postInit で 移動速度 +150% / 索敵範囲 ×3 / 段差 +3 ({@link ForgeMod#STEP_HEIGHT_ADDITION}) を恒常付与。
 * ジャンプ力は専用 attribute が馬等に限られるため跳躍力上昇 I (Jump Boost) effect で表現し tick で維持。
 * <p>tick で {@link #REVEAL_DIST} 内の player 有無により透明 ON/OFF と攻撃力 0 modifier を切替。
 */
public class SpacetimeShadowTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_SPEED = UUID.fromString("5e7c1a93-6b2d-4f8a-9c3e-1d5b7a2f9e64");
    private static final UUID MOD_RANGE = UUID.fromString("5e7c1a93-6b2d-4f8a-9c3e-1d5b7a2f9e65");
    private static final UUID MOD_ATK_ZERO = UUID.fromString("5e7c1a93-6b2d-4f8a-9c3e-1d5b7a2f9e66");
    private static final UUID MOD_STEP = UUID.fromString("5e7c1a93-6b2d-4f8a-9c3e-1d5b7a2f9e67");
    private static final double SPEED_BONUS = 1.5;       // 移動速度 +150% (固定)
    private static final double RANGE_MULT = 2.0;        // 索敵 ×3 (= +200%)
    private static final double STEP_BONUS = 3.0;        // 段差 +3
    private static final double REVEAL_DIST = 6.0;       // player がこの距離内に来ると露見

    public SpacetimeShadowTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, "gtcsolo.spacetime_shadow_speed",
                SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.FOLLOW_RANGE, MOD_RANGE, "gtcsolo.spacetime_shadow_range",
                RANGE_MULT, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, ForgeMod.STEP_HEIGHT_ADDITION.get(), MOD_STEP, "gtcsolo.spacetime_shadow_step",
                STEP_BONUS, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 5 != 0) return;
        // ジャンプ力 +1 = 跳躍力上昇 I を維持 (跳躍 attribute は馬等限定のため effect で表現)
        mob.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false));
        Player near = mob.level().getNearestPlayer(mob, REVEAL_DIST);
        if (near != null) {
            // 露見: 透明解除 + 攻撃力復帰
            if (mob.isInvisible()) mob.setInvisible(false);
            L2TraitAttributes.remove(mob, Attributes.ATTACK_DAMAGE, MOD_ATK_ZERO);
        } else {
            // 潜伏: 透明化 + 攻撃力 0
            if (!mob.isInvisible()) mob.setInvisible(true);
            L2TraitAttributes.setTransient(mob, Attributes.ATTACK_DAMAGE, MOD_ATK_ZERO, "gtcsolo.spacetime_shadow_atkzero",
                    -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }
}
