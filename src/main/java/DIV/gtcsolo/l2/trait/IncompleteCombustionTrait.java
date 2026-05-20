package DIV.gtcsolo.l2.trait;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.slf4j.Logger;

/**
 * [26] Incomplete Combustion — 体力満タン状態から即死した場合、 復活し続ける。
 *
 * <p>満タン (= 残 HP == max HP) からの 1 撃即死のみ復活。 一度被弾していれば復活しない。
 * 対処: 「一度でも被弾させてから倒す」 。 永久ループ化はしない。
 */
public class IncompleteCombustionTrait extends MobTrait {
    private static final Logger LOGGER = LogUtils.getLogger();

    public IncompleteCombustionTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        float curHp = entity.getHealth();
        float maxHp = entity.getMaxHealth();
        float dmg = event.getAmount();
        boolean isFull = curHp >= maxHp - 0.001f;
        boolean isLethal = dmg >= curHp;

        LOGGER.info("[IncompleteCombustion:{}] hit detected: curHP={}/{}, dmg={}, isFull={}, isLethal={}",
                entity, curHp, maxHp, dmg, isFull, isLethal);

        if (isFull && isLethal) {
            event.setCanceled(true);
            entity.setHealth(maxHp);
            LOGGER.info("[IncompleteCombustion:{}] revival triggered, HP restored to maxHP={}", entity, maxHp);
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public int reviveCount = 0;
    }
}
