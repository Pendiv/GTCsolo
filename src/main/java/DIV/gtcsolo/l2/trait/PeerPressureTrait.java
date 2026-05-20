package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * [36] Peer Pressure (同調圧力) — クリーパー専用。 自身が着火したら、 player 近傍の
 * 他クリーパーも同時に起爆させる。
 *
 * <p>tick で「自身が新たに着火した瞬間 (= 前 tick 未着火 → 今 tick 着火)」 を検知し、
 * player 半径 {@link #SHARE_RADIUS} 内の他クリーパー全てを ignite。
 * <p>[32] Chain Detonation が「爆発ダメで連鎖」 なのに対し、 こちらは「player 近接条件の同時起爆」。
 */
public class PeerPressureTrait extends TypedMobTrait {

    private static final double SHARE_RADIUS = 12.0;

    public PeerPressureTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidTick(LivingEntity mob, int lv) {
        if (mob.level().isClientSide()) return;
        if (!(mob instanceof Creeper self)) return;
        if (self.getSwellDir() <= 0) return; // 着火中のみ
        // swell 開始直後 (= swell counter < 5) のときに 1 度だけ伝染
        if (self.tickCount % 5 != 0) return; // 5t 間隔 throttle
        Player p = mob.level().getNearestPlayer(mob, SHARE_RADIUS);
        if (p == null) return;
        AABB area = new AABB(
                p.getX() - SHARE_RADIUS, p.getY() - SHARE_RADIUS, p.getZ() - SHARE_RADIUS,
                p.getX() + SHARE_RADIUS, p.getY() + SHARE_RADIUS, p.getZ() + SHARE_RADIUS);
        for (Creeper c : mob.level().getEntitiesOfClass(Creeper.class, area, e -> e != self)) {
            if (c.getSwellDir() <= 0) {
                c.ignite();
            }
        }
    }
}
