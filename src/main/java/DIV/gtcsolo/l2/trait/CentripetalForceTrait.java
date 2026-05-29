package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 求心力 (Centripetal Force) — クリーパー専用。
 * 爆発のプロセス中 (= swell > 0)、 範囲内の player を緩やかに self 方向へ引き寄せる。
 *
 * <p>仕様:
 * <ul>
 *   <li>swell ≤ 0 (= 着火前 / 解除済) は no-op</li>
 *   <li>範囲 = (3 + 2N) ブロック (level で拡大)</li>
 *   <li>引き寄せ強度 = {@link #PULL} 固定 (そこそこ、 逃げにくくする程度)</li>
 *   <li>player 方向ベクトル正規化 × pull を addDeltaMovement</li>
 * </ul>
 */
public class CentripetalForceTrait extends TypedMobTrait {

    private static final double PULL = 0.06;  // 吸引強度は固定 (そこそこ、 逃げにくくする程度)

    public CentripetalForceTrait(ChatFormatting style) {
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
        if (c.getSwellDir() <= 0) return;

        double radius = 3.0 + 2.0 * lv;  // (3 + 2N) ブロック吸引範囲
        AABB area = mob.getBoundingBox().inflate(radius);
        List<Player> players = mob.level().getEntitiesOfClass(Player.class, area);
        for (Player p : players) {
            if (p.isSpectator() || p.isCreative()) continue;
            Vec3 dir = mob.position().subtract(p.position());
            double dist = dir.length();
            if (dist < 0.5 || dist > radius) continue;
            Vec3 vel = dir.normalize().scale(PULL);
            p.setDeltaMovement(p.getDeltaMovement().add(vel));
            p.hurtMarked = true;
        }
    }
}
