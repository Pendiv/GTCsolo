package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * [35] Proximity Fuse (時限爆弾) — クリーパー専用。 視認・遮蔽を問わず、 近くに一定時間
 * プレイヤーが居続けたら着火する。
 *
 * <p>tick で範囲内 player の滞在時間をカウント、 閾値到達で {@link Creeper#ignite()}。
 * <p>滞在時間閾値 = {@link #BASE_DWELL_TICKS} - {@link #DWELL_REDUCTION_PER_LEVEL} × lv
 * (= lv1 で 5 秒、 lv5 で 3 秒)、 範囲 = lv 不変の 8 ブロック。
 * <p>state: static Map で mob UUID → 累計滞在 tick。 範囲外に出たら 0 リセット。
 */
public class ProximityFuseTrait extends TypedMobTrait {

    private static final Map<UUID, Integer> DWELL = new HashMap<>();
    private static final double RADIUS = 8.0;
    private static final int BASE_DWELL_TICKS = 120;        // 滞在閾値 = (120 - 20n) tick
    private static final int DWELL_REDUCTION_PER_LEVEL = 20;

    public ProximityFuseTrait(ChatFormatting style) {
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
        if (c.getSwellDir() > 0) return; // 既に着火中
        Player p = mob.level().getNearestPlayer(mob, RADIUS);
        UUID id = mob.getUUID();
        if (p == null || p.distanceTo(mob) > RADIUS) {
            DWELL.remove(id);
            return;
        }
        int cur = DWELL.getOrDefault(id, 0) + 1;
        int threshold = Math.max(20, BASE_DWELL_TICKS - DWELL_REDUCTION_PER_LEVEL * lv);
        if (cur >= threshold) {
            c.ignite();
            DWELL.remove(id);
        } else {
            DWELL.put(id, cur);
        }
    }
}
