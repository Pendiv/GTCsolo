package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.ExplosionEvent;

/**
 * [38] Full Tank (燃料満タン) — クリーパー専用。 爆発威力増 + 火を撒く (fire=true)。
 *
 * <p>{@link ExplosionEvent.Start} を listen し、 source が full_tank 持ちクリーパーなら
 * 元爆発を cancel して、 半径拡大 + fire=true で再 trigger。
 * <p>新半径 = {@link #BASE_RADIUS} + {@link #RADIUS_PER_LEVEL} × lv (= lv1 で 5、 lv5 で 13)。
 * <p>無限再帰防止: ThreadLocal の reentry flag。
 */
public class FullTankTrait extends TypedMobTrait {

    private static final float BASE_RADIUS = 3.0f;     // vanilla creeper の標準
    private static final float RADIUS_PER_LEVEL = 2.0f;
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> false);

    public FullTankTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    /** {@link DIV.gtcsolo.l2.L2EventHandlers#onExplosionStart} から呼ばれる */
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (REENTRY.get()) return;
        Explosion ex = event.getExplosion();
        Entity src = ex.getDirectSourceEntity();
        if (!(src instanceof Creeper c)) return;
        if (!MobTraitCap.HOLDER.isProper(c)) return;
        int lv = MobTraitCap.HOLDER.get(c).getTraitLevel(ModL2Traits.FULL_TANK.get());
        if (lv <= 0) return;

        event.setCanceled(true);
        float newRadius = BASE_RADIUS + RADIUS_PER_LEVEL * lv;
        REENTRY.set(true);
        try {
            event.getLevel().explode(c, c.getX(), c.getY(), c.getZ(),
                    newRadius, true, Level.ExplosionInteraction.MOB);
        } finally {
            REENTRY.set(false);
        }
    }
}
