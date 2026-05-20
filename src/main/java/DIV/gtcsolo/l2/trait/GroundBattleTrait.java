package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * [01] Ground Battle — 周囲のプレイヤーの浮遊・飛行手段を無効化。
 *
 * <p>毎 tick、 半径 32 ブロック以内のプレイヤーに対し:
 * <ul>
 *   <li>Levitation/Slow Falling 等の浮遊効果を除去</li>
 *   <li>Creative flight フラグを抑制 (= mayFly=true, flying=false)</li>
 *   <li>エリトラ滑空状態を解除</li>
 * </ul>
 */
public class GroundBattleTrait extends MobTrait {

    private static final double RADIUS = 32.0;

    public GroundBattleTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 5 != 0) return; // 5 tick おきに軽量化

        AABB area = mob.getBoundingBox().inflate(RADIUS);
        List<Player> players = mob.level().getEntitiesOfClass(Player.class, area);
        for (Player p : players) {
            if (p.isSpectator() || p.isCreative()) continue;
            p.removeEffect(MobEffects.LEVITATION);
            p.removeEffect(MobEffects.SLOW_FALLING);
            if (p.getAbilities().flying) {
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
            if (p.isFallFlying()) {
                p.stopFallFlying();
            }
        }
    }
}
