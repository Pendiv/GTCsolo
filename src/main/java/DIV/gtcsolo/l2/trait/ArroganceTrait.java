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
 *   <li>tick: 20 tick (= 1 秒) おきに maxHP × 7% を自己回復 (= level 非依存の固定値)</li>
 *   <li>player から被弾すると lastPlayerHit を更新、 (7 - level) 秒は回復停止 (= 高 level ほど早く再開)</li>
 *   <li>LivingHealEvent listener (= {@link DIV.gtcsolo.l2.L2EventHandlers}) で cap.hasTrait チェック</li>
 * </ul>
 */
public class ArroganceTrait extends MobTrait {

    public static final int HEAL_INTERVAL = 20;
    /** 1 秒あたり maxHP × 7% を回復 (= level 非依存の固定値) */
    public static final double HEAL_PCT = 0.07;

    /** 被弾後の回復停止時間 = (7 - level) 秒、 最低 1 秒 (= 高 level ほど早く再開) */
    private static int blockTicks(int level) {
        return Math.max(20, (7 - level) * 20);
    }

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
        if (now - data.lastPlayerHitTick < blockTicks(level)) return;
        if (mob.getHealth() >= mob.getMaxHealth()) return;
        float heal = (float) (mob.getMaxHealth() * HEAL_PCT);
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
        int lv = cap.getTraitLevel(DIV.gtcsolo.l2.ModL2Traits.ARROGANCE.get());
        if (lv <= 0) return false;
        Data data = cap.getOrCreateData(DIV.gtcsolo.l2.ModL2Traits.ARROGANCE.get().getRegistryName(), Data::new);
        long now = entity.level().getGameTime();
        return (now - data.lastPlayerHitTick) < blockTicks(lv);
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public long lastPlayerHitTick = -10000;
    }
}
