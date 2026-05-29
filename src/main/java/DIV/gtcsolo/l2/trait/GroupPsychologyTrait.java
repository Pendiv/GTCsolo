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
 * [08] Group Psychology — 周囲の敵 mob の数に応じて攻撃力が増加。
 *
 * <p>倍率 = 1 + (周囲 mob 数) × 0.1 × level
 * <p>周囲 mob 数は 20 tick おきにキャッシュ更新 (= 毎 tick AABB クエリは重い)。
 */
public class GroupPsychologyTrait extends MobTrait {

    private static final double RADIUS = 16.0;
    private static final int CACHE_INTERVAL = 20;

    public GroupPsychologyTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % CACHE_INTERVAL != 0) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        data.nearbyCount = mob.level().getEntitiesOfClass(Mob.class, area, e -> e != mob).size();
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        LivingHurtEvent event = cache.getLivingHurtEvent();
        if (event == null) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(attacker);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        double mult = 1.0 + data.nearbyCount * 0.06 * level;  // 周囲 1 体につき +6n%
        event.setAmount(event.getAmount() * (float) mult);
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public int nearbyCount = 0;
    }
}
