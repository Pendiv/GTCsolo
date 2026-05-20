package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;

/**
 * [03] Arrogance — 強力な自己回復を持つが、 プレイヤーから攻撃を受けると一定時間回復を停止。
 *
 * <p>放置すれば全快、 殴り続ければ削れる。 「手を止めさせない圧力」。
 *
 * <p>仕様:
 * <ul>
 *   <li>tick: 20 tick おきに maxHP × 2% × level を自己回復</li>
 *   <li>player から被弾すると lastPlayerHit を更新、 100 tick (5秒) は回復停止</li>
 *   <li>LivingHealEvent listener (= {@link DIV.gtcsolo.l2.L2EventHandlers}) で cap.hasTrait チェック</li>
 * </ul>
 */
public class ArroganceTrait extends MobTrait {

    public static final int HEAL_BLOCK_TICKS = 100;
    public static final int HEAL_INTERVAL = 20;
    /** lv1 で 1 秒あたり maxHP × 5%、 lv5 で 25% (= 4 秒で全回復) */
    public static final double HEAL_PCT_PER_LEVEL = 0.05;

    public ArroganceTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % HEAL_INTERVAL != 0) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        long now = mob.level().getGameTime();
        if (now - data.lastPlayerHitTick < HEAL_BLOCK_TICKS) return;
        if (mob.getHealth() >= mob.getMaxHealth()) return;
        float heal = (float) (mob.getMaxHealth() * HEAL_PCT_PER_LEVEL * level);
        mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + heal));
    }

    @Override
    public void onAttackedByOthers(int level, LivingEntity entity, LivingAttackEvent event) {
        if (entity.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        data.lastPlayerHitTick = entity.level().getGameTime();
    }

    /** 外部 event handler から呼ばれる: 回復阻害判定 */
    public static boolean shouldBlockHeal(LivingEntity entity) {
        if (!MobTraitCap.HOLDER.isProper(entity)) return false;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        if (!cap.hasTrait(DIV.gtcsolo.l2.ModL2Traits.ARROGANCE.get())) return false;
        Data data = cap.getOrCreateData(DIV.gtcsolo.l2.ModL2Traits.ARROGANCE.get().getRegistryName(), Data::new);
        long now = entity.level().getGameTime();
        return (now - data.lastPlayerHitTick) < HEAL_BLOCK_TICKS;
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public long lastPlayerHitTick = -10000;
    }
}
