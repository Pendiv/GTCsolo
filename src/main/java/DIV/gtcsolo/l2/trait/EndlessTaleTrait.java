package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * [56] Endless Tale (物語は終わらない) — 確率で復活する。 復活毎に確率が永続 10% 減少。
 *
 * <p>初期確率 = {@link #BASE_REVIVE_CHANCE} + lv × {@link #CHANCE_PER_LEVEL}
 * (= lv1 で 62%、 lv5 で 110%、 100%超分はスタックして連続成功)。
 * <p>復活毎に永続 {@link #DECAY_PER_REVIVE} 減算。
 * <p>排他: 復活系排他グループ ([17] Endure / [37] Second Chance / 本特性)。
 * <p>実装: Endure と同じ pattern (= heal → setHealth → isAlive → event.cancel)。
 */
public class EndlessTaleTrait extends MobTrait {

    private static final double BASE_REVIVE_CHANCE = 0.50;
    private static final double CHANCE_PER_LEVEL = 0.12;
    private static final double DECAY_PER_REVIVE = 0.10;
    private static final float REVIVE_HEAL_RATIO = 1.0f; // = フル HP で復活

    public EndlessTaleTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);

        // 現在確率 = max(0, base + lv×bonus - revives×decay) — 100% 超分はスタック (= roll 複数回)
        double chance = BASE_REVIVE_CHANCE + CHANCE_PER_LEVEL * level - DECAY_PER_REVIVE * data.revives;
        if (chance <= 0) return;
        // 100% 超分は確定 + 余剰でもう一度 roll
        double rng = entity.getRandom().nextDouble();
        if (rng >= chance) return;

        // 復活処理 (= UndyingTrait 同 pattern)
        float healAmount = entity.getMaxHealth() * REVIVE_HEAL_RATIO;
        float allowed = ForgeEventFactory.onLivingHeal(entity, healAmount);
        if (allowed <= 0) return;
        entity.setHealth(allowed);
        if (entity.isAlive()) {
            event.setCanceled(true);
            data.revives++;
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public int revives = 0;
    }
}
