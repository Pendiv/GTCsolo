package DIV.gtcsolo.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 時空の潜航 / 超越者 の「他 mob の AI ターゲットから除外」。
 *
 * <p>mob の索敵 (NearestAttackableTargetGoal 等) は {@link TargetingConditions#test} を通る。
 * target が除外対象 (= 潜航/超越者 保持) なら false を返し、 どの mob からも狙われなくする。
 * <p>保持 mob 自身の索敵 (= 自分が attacker、 player が target) には影響しない (= 攻撃は継続)。
 * player の手動攻撃も TargetingConditions を経由しないため通常通り当たる。
 */
@Mixin(TargetingConditions.class)
public class TargetingConditionsMixin {

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$excludeSpacetime(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target != null && DIV.gtcsolo.l2.SpacetimeTraits.isAiTargetExcluded(target)) {
            cir.setReturnValue(false);
        }
    }
}
