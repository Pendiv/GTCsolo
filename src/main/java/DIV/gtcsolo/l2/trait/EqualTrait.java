package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [19] Equal — プレイヤーをターゲット確定した瞬間から 5 秒間無敵。
 *
 * <p>ターゲット確定検知は {@link DIV.gtcsolo.l2.L2EventHandlers#onChangeTarget} (= LivingChangeTargetEvent)
 * からタイマー開始。 ここでは被弾時の無敵判定のみ。
 */
public class EqualTrait extends MobTrait {

    public static final int INVUL_TICKS = 100; // 5 sec

    public EqualTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        long now = entity.level().getGameTime();
        if (now - data.targetLockTick < INVUL_TICKS) {
            event.setCanceled(true);
        }
    }

    /** event handler から呼ばれる: ターゲット確定タイマー開始 */
    public static void onTargetLocked(LivingEntity entity) {
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        if (!cap.hasTrait(DIV.gtcsolo.l2.ModL2Traits.EQUAL.get())) return;
        Data data = cap.getOrCreateData(DIV.gtcsolo.l2.ModL2Traits.EQUAL.get().getRegistryName(), Data::new);
        long now = entity.level().getGameTime();
        // 既にタイマー中なら再起動しない (= 連続ターゲット切り替えで無敵延長を防ぐ)
        if (now - data.targetLockTick < INVUL_TICKS) return;
        data.targetLockTick = now;
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public long targetLockTick = -10000;
    }
}
