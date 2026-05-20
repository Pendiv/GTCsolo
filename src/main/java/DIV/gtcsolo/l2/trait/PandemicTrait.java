package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [24] Pandemic — 接触で他 mob に同 trait を感染させる、 失効ポイントで自然収束。
 *
 * <p>仕様 (確定版):
 * <ul>
 *   <li>攻撃力倍率: 1 + 0.1 × rank</li>
 *   <li>感染: tick で近接 mob に setTrait (= ランクは親のまま)</li>
 *   <li>「より強いランク優先」 ルールはこの trait 専用 — 弱いランクは上書きしない</li>
 *   <li>失効ポイント: 毎 tick (感染数 + 1)² 加算、 6000 で失効</li>
 *   <li>失効後: 感染しない、 攻撃力 buff のみ残存</li>
 * </ul>
 */
public class PandemicTrait extends MobTrait {

    private static final double CONTACT_RADIUS = 2.5;
    private static final int INFECTION_INTERVAL = 20;
    private static final long EXPIRY_THRESHOLD = 6000;

    public PandemicTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);

        // 失効ポイント累積 (= 感染数 + 1)²
        long inc = (long) (data.infectedCount + 1) * (data.infectedCount + 1);
        data.expiryPoints += inc;

        // 失効判定
        if (!data.expired && data.expiryPoints >= EXPIRY_THRESHOLD) {
            data.expired = true;
        }

        // 感染 (= 失効してなければ近接 mob に伝播)
        if (data.expired) return;
        if (mob.tickCount % INFECTION_INTERVAL != 0) return;
        AABB area = mob.getBoundingBox().inflate(CONTACT_RADIUS);
        for (Mob other : mob.level().getEntitiesOfClass(Mob.class, area, e -> e != mob)) {
            tryInfect(other, level);
        }
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        LivingHurtEvent event = cache.getLivingHurtEvent();
        if (event != null) {
            double mult = 1.0 + 0.1 * level;
            event.setAmount(event.getAmount() * (float) mult);
        }
        // ターゲットが mob なら感染試行 (= player ターゲットには感染しない、 mob 限定)
        if (traitCache.target instanceof Mob m) {
            tryInfect(m, level);
        }
    }

    /** 「より強いランク優先」 で setTrait。 既に同 trait 持ちで弱いランクなら上書きしない */
    private void tryInfect(Mob target, int parentRank) {
        if (!MobTraitCap.HOLDER.isProper(target)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(target);
        Integer existing = cap.traits.get(this);
        if (existing != null && existing >= parentRank) return; // より強い既存ランクを保持
        cap.setTrait(this, parentRank);
        // 子の感染数カウントは継承時にリセット (= 子は新たに 0 から)
        cap.getOrCreateData(getRegistryName(), Data::new).infectedCount = 0;
        // 親の感染カウント +1
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public int infectedCount = 0;
        @SerialClass.SerialField
        public long expiryPoints = 0;
        @SerialClass.SerialField
        public boolean expired = false;
    }
}
