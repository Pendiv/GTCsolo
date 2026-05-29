package DIV.gtcsolo.l2;

import DIV.gtcsolo.registry.ModEffects;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 時空の消滅の totem 介入ロジック。 {@code PlayerTotemMixin} の
 * {@code checkTotemDeathProtection} 介入から呼ばれる。
 *
 * <ul>
 *   <li>{@link #hasCertainKill}: 攻撃者が確殺を持つなら totem を不発化 (= 殺害貫通)</li>
 *   <li>{@link #onTotemSaved}: 消滅 mob が player を totem で取り逃したら確殺 + 発光を付与</li>
 * </ul>
 */
public final class SpacetimeAnnihilationGate {

    // 1.20.1 は無限 duration (-1) 非対応のため、 実質永続の大きな有限値を使う。
    private static final int PERSIST_DURATION = 1_000_000;

    private SpacetimeAnnihilationGate() {}

    public static boolean hasCertainKill(LivingEntity attacker) {
        return attacker.hasEffect(ModEffects.CERTAIN_KILL.get());
    }

    public static void onTotemSaved(LivingEntity attacker, LivingEntity victim) {
        if (attacker.level().isClientSide()) return;
        if (!(victim instanceof Player)) return;
        if (!MobTraitCap.HOLDER.isProper(attacker)) return;
        if (MobTraitCap.HOLDER.get(attacker).getTraitLevel(ModL2Traits.SPACETIME_ANNIHILATION.get()) <= 0) return;
        if (attacker.hasEffect(ModEffects.CERTAIN_KILL.get())) return;
        attacker.addEffect(new MobEffectInstance(ModEffects.CERTAIN_KILL.get(), -1, 0, false, true));
        attacker.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, true));
    }
}
