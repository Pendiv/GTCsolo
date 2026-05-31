package DIV.gtcsolo.l2.trait;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;

/**
 * [17] Endure — 1 度のみ。 致死時に体力 1 で耐え、 5 秒間無敵。
 *
 * <p>UndyingTrait 同パターン: heal → isAlive 判定 → event.cancel の順序が重要
 * (cancel を先にやると vanilla 側の死亡処理タイミングがずれる場合がある)。
 * <p>体力 1 + 無敵付与のため {@code L2EntityUtil.reviveFull} (= フル HP 復活) ではなく専用処理。
 */
public class EndureTrait extends MobTrait {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int INVUL_TICKS = 100;

    public EndureTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.triggered) return;

        // heal を先に適用 (= ForgeEventFactory 経由で他 mod も尊重)、 isAlive ならキャンセル
        float allowed = ForgeEventFactory.onLivingHeal(entity, 1.0f);
        if (allowed <= 0) {
            LOGGER.warn("[Endure] heal blocked by other mod, cannot save {}", entity);
            return;
        }
        entity.setHealth(allowed);
        if (entity.isAlive()) {
            event.setCanceled(true);
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, INVUL_TICKS, 4, false, false));
            data.triggered = true;
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean triggered = false;
    }
}
