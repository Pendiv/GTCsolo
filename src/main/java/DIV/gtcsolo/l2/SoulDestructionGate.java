package DIV.gtcsolo.l2;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.world.entity.LivingEntity;

/**
 * SoulDestruction trait の totem bypass 判定 helper。
 *
 * <p>mixin から直接 {@code ModL2Traits.SOUL_DESTRUCTION.get()} を呼ぶと、
 * mixin parse 時に ModL2Traits の static init が走る → DeferredRegister.create
 * 経由で L2H 内部レジストリへアクセス → mixin 段階では L2H mod 未 load なのでクラッシュ。
 *
 * <p>そこで mixin はこの helper 経由 (= 通常クラスとして lazy load) で trait 判定する。
 * helper のメソッド呼び出しは実 runtime まで ModL2Traits の class init を遅延させる。
 */
public final class SoulDestructionGate {
    private SoulDestructionGate() {}

    /**
     * @return 攻撃者が SoulDestruction trait を持っているなら true (= totem 発火 skip)
     */
    public static boolean shouldBypassTotem(LivingEntity attacker) {
        if (attacker == null) return false;
        // 注: HOLDER.get() は cap 不在 entity に対して Optional.get() で例外を投げる
        //     先に isProper() でガードしないとクラッシュする
        if (!MobTraitCap.HOLDER.isProper(attacker)) return false;
        MobTraitCap cap = MobTraitCap.HOLDER.get(attacker);
        return cap.hasTrait(ModL2Traits.SOUL_DESTRUCTION.get());
    }
}
