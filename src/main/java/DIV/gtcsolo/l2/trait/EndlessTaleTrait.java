package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2EntityUtil;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * [56] Endless Tale (物語は終わらない) — 確率で復活する。 復活毎に確率が永続減衰する。
 *
 * <p>初期確率 = 50 + 22.15n % (= lv1 で 72%、 lv3 で 116% = 確定)。
 * <p>復活成功毎に確率 ×0.8 → -5%。 100% 以上は確定復活。
 * <p>排他: 復活系排他グループ ([17] Endure / [37] Second Chance / 本特性)。
 */
public class EndlessTaleTrait extends MobTrait {

    private static final double BASE_REVIVE_CHANCE = 0.50;
    private static final double CHANCE_PER_LEVEL = 0.2215;   // 初期確率 = 50 + 22.15n %
    private static final double DECAY_MULT = 0.8;            // 復活成功毎に ×0.8
    private static final double DECAY_FLAT = 0.05;           // その後 -5% (固定値)

    public EndlessTaleTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);

        if (!data.initialized) {
            data.currentChance = BASE_REVIVE_CHANCE + CHANCE_PER_LEVEL * level;
            data.initialized = true;
        }
        if (data.currentChance <= 0) return;
        // 100% 以上は確定復活 (nextDouble() < chance、 chance≥1 で常に true)
        if (entity.getRandom().nextDouble() >= data.currentChance) return;

        // フル HP 復活 (= 復活系共通処理)。 成功したら次回確率を減衰
        if (L2EntityUtil.reviveFull(entity, event)) {
            data.currentChance = data.currentChance * DECAY_MULT - DECAY_FLAT;
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean initialized = false;
        @SerialClass.SerialField
        public double currentChance = 0;
    }
}
