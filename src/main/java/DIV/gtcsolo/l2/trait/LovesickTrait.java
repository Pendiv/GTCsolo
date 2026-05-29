package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * [40] Lovesick (恋煩い) — クリーパー専用。 着火した瞬間からプレイヤー方向へ低速移動する。
 *
 * <p>「着火後逃げれば不発」 の前提を崩す。
 * <p>追尾速度 = player 移動速度 × 0.2N。 max_rank 3。
 * <p>排他: [33] Hair Trigger と排他 (= datapack 側で配布絞り推奨)。
 */
public class LovesickTrait extends TypedMobTrait {

    private static final double SEARCH_RADIUS = 24.0;

    public LovesickTrait(ChatFormatting style) {
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
        if (c.getSwellDir() <= 0) return; // 着火中のみ
        Player p = mob.level().getNearestPlayer(mob, SEARCH_RADIUS);
        if (p == null) return;
        Vec3 dir = p.position().subtract(mob.position()).normalize();
        double pSpeed = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double speed = pSpeed * 0.2 * lv;  // player 移動速度 × 0.2N
        c.setDeltaMovement(c.getDeltaMovement().add(dir.x * speed, 0, dir.z * speed));
    }
}
