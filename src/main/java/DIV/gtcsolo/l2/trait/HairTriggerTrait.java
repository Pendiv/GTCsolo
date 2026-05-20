package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

/**
 * [33] Hair Trigger (即断即決) — クリーパー専用。 起爆カウントダウン (maxSwellTime) を短縮する。
 *
 * <p>実装: 私有 final field である maxSwellTime に直接触らず、 {@link Creeper#setSwellDir}
 * 経由で swell カウンタの加速量を増やす (= 同じ maxSwellTime に早く到達する)。
 * <p>加速量 = 1 + lv (= lv1 で 2 倍速、 lv5 で 6 倍速)。
 * <p>排他: [40] Lovesick と排他 (= datapack 側 or 効果側で排他制御)。
 */
public class HairTriggerTrait extends TypedMobTrait {

    public HairTriggerTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidTick(LivingEntity mob, int lv) {
        if (mob.level().isClientSide()) return;
        if (!(mob instanceof Creeper c)) return;
        if (c.getSwellDir() <= 0) return; // 着火中のみ加速 (= 通常状態の swell 減衰挙動は崩さない)
        c.setSwellDir(1 + lv);
    }
}
