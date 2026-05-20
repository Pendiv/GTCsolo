package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * [62] Pure Heart (純情) — 有害状態異常 (デバフ) を一切受け付けない。
 *
 * <p>実装: {@link MobEffectEvent.Applicable} で対象 entity の trait を確認し、
 * 付与しようとしている effect が {@link MobEffectCategory#HARMFUL} なら DENY する。
 * Event handler は {@link DIV.gtcsolo.l2.L2EventHandlers} 経由で登録。
 */
public class PureHeartTrait extends MobTrait {

    public PureHeartTrait(ChatFormatting style) {
        super(style);
    }

    /** {@link DIV.gtcsolo.l2.L2EventHandlers#onEffectApplicable} から呼ばれる */
    public static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        if (!cap.hasTrait(ModL2Traits.PURE_HEART.get())) return;
        MobEffectInstance ins = event.getEffectInstance();
        if (ins == null) return;
        if (ins.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
    }
}
