package DIV.gtcsolo.combat.arrow;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * 特殊な矢システムの Forge イベント配線。
 * <ul>
 *   <li>{@link EntityJoinLevelEvent} → {@link SpecialArrow#onSpawn} (= 出現時、 hitscan 化等)</li>
 *   <li>{@link ProjectileImpactEvent} → onHitEntity / onHitBlock</li>
 *   <li>{@link LivingHurtEvent} → onHurt (= 矢由来ダメージの改変、 防御貫通等)</li>
 *   <li>{@link LivingEvent.LivingTickEvent} → 飛墜の矢の飛行抑止窓を enforce</li>
 * </ul>
 */
public class ArrowEventHandlers {

    /** player persistentData: この gameTime まで飛行無効 (= 飛墜の矢が書き込む)。 */
    public static final String NO_FLY_KEY = "gtcsolo.no_fly_until";

    @SubscribeEvent
    public void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof AbstractArrow arrow && SpecialArrow.isSpecial(arrow)) {
            SpecialArrow.onSpawn(arrow, shooterOf(arrow));
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (arrow.level().isClientSide()) return;
        if (!SpecialArrow.isSpecial(arrow)) return;
        LivingEntity shooter = shooterOf(arrow);
        HitResult hr = event.getRayTraceResult();
        if (hr instanceof EntityHitResult ehr) {
            if (ehr.getEntity() instanceof LivingEntity target) {
                SpecialArrow.onHitEntity(arrow, target, shooter);
            }
        } else if (hr instanceof BlockHitResult bhr) {
            SpecialArrow.onHitBlock(arrow, bhr, shooter);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && SpecialArrow.isSpecial(arrow)) {
            SpecialArrow.onHurt(arrow, event, shooterOf(arrow));
        }
    }

    /** 飛墜の矢: 無効化窓中の player の飛行を毎 tick 抑止する。 */
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (p.level().isClientSide()) return;
        long until = p.getPersistentData().getLong(NO_FLY_KEY);
        if (until <= 0) return;
        if (p.level().getGameTime() >= until) {
            p.getPersistentData().remove(NO_FLY_KEY);
            return;
        }
        // 窓中: エリトラ滑空停止 + 飛行フラグ off。 mayFly は触らない
        // (= creative / MOD 飛行(メカスーツ/エンジェルリング等) の権限を恒久破壊せず、 維持飛行のみ阻む)
        if (p.isFallFlying()) p.stopFallFlying();
        if (p.getAbilities().flying) {
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    @Nullable
    private static LivingEntity shooterOf(AbstractArrow arrow) {
        return arrow.getOwner() instanceof LivingEntity le ? le : null;
    }
}
