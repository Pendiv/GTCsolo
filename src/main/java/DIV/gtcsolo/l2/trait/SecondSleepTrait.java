package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * 2 度寝 (Second Sleep) — 倒されたあと、 ランダムな時間後に復活する。 1 回限り。
 *
 * <p>仕様:
 * <ul>
 *   <li>onDeath で event cancel、 setHealth(maxHealth) で復活、 一旦 invulnerable 化</li>
 *   <li>復活までの delay は {@link #DELAY_MIN}〜{@link #DELAY_MAX} tick uniform</li>
 *   <li>state: revived (= 一度 trigger 済かどうか)、 永続化</li>
 *   <li>delay 中の見え方 = 即座に setHealth 復元 + setInvulnerable で「うたた寝」 風</li>
 * </ul>
 *
 * <p>注: L2H の RebirthTrait と類似だが、 ランダム delay + 一度限り の点で挙動が異なる。
 */
public class SecondSleepTrait extends MobTrait {

    private static final int DELAY_MIN = 200;   // 10 秒
    private static final int DELAY_MAX = 2000;  // 100 秒

    public SecondSleepTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        super.onDeath(level, entity, event);
        if (entity.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.revived) return;

        int delay = DELAY_MIN + entity.getRandom().nextInt(DELAY_MAX - DELAY_MIN + 1);
        data.revived = true;
        data.reviveAtTick = entity.tickCount + delay;
        event.setCanceled(true);
        entity.setHealth(entity.getMaxHealth());
        entity.setInvulnerable(true);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (!data.revived) return;
        if (data.reviveAtTick > 0 && mob.tickCount >= data.reviveAtTick) {
            mob.setInvulnerable(false);
            data.reviveAtTick = 0;
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean revived = false;
        @SerialClass.SerialField
        public int reviveAtTick = 0;
    }
}
