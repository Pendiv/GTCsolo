package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;

/**
 * 蓄積 (Accumulation) — クリーパー専用。 爆発ダメージを受ける度、 自身の爆発威力と範囲が増大する。
 *
 * <p>仕様:
 * <ul>
 *   <li>onValidHurtByOthers で IS_EXPLOSION ダメージを検知 → hitCount++ (= 永続 state)</li>
 *   <li>有効 stack = hitCount + lv (= レベルごとに 1 回被爆済み扱い)</li>
 *   <li>自身が爆発する時、 半径 = BASE_RADIUS × (1 + 0.10 × stack) ({@link #MAX_RADIUS} cap)。
 *       ※ ダメージ +15%/stack は vanilla 仕様で半径に連動 (独立制御は未実装)</li>
 *   <li>cancel + 拡大半径で再 explode (= FullTank と同じ手法、 REENTRY flag で無限再帰防止)</li>
 * </ul>
 *
 * <p>注: FullTank も同じ ExplosionEvent.Start を使う。 両 trait が同一 Creeper に付くと
 * 二重 cancel/re-explode の競合可能性あり (= 稀ケース、 通常は排他運用想定)。
 */
public class AccumulationTrait extends TypedMobTrait {

    private static final float BASE_RADIUS = 3.0f;
    private static final float RANGE_PER_HIT = 0.10f;   // 爆発受ける度 範囲 +10%
    private static final float MAX_RADIUS = 20.0f;
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> false);

    public AccumulationTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypeTags.IS_EXPLOSION)) return;
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        data.hitCount++;
    }

    /** {@link DIV.gtcsolo.l2.L2EventHandlers#onExplosionStart} から呼ばれる */
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (REENTRY.get()) return;
        Explosion ex = event.getExplosion();
        Entity src = ex.getDirectSourceEntity();
        if (!(src instanceof Creeper c)) return;
        if (!MobTraitCap.HOLDER.isProper(c)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(c);
        int lv = cap.getTraitLevel(ModL2Traits.ACCUMULATION.get());
        if (lv <= 0) return;
        Data data = cap.getOrCreateData(ModL2Traits.ACCUMULATION.get().getRegistryName(), Data::new);
        int stacks = data.hitCount + lv;  // レベルごとに 1 回被爆済み扱い

        float newRadius = Math.min(MAX_RADIUS, BASE_RADIUS * (1.0f + RANGE_PER_HIT * stacks));
        event.setCanceled(true);
        REENTRY.set(true);
        try {
            event.getLevel().explode(c, c.getX(), c.getY(), c.getZ(),
                    newRadius, Level.ExplosionInteraction.MOB);
        } finally {
            REENTRY.set(false);
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public int hitCount = 0;
    }
}
