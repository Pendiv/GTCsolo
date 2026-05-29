package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * [21] Resentment — player 由来ダメージが 50% 未満で死亡すると、 周囲 player に大量のデバフを配布。
 *
 * <p>「楽な討伐」 (= 環境ダメージ・他 mob による削り) を許さない、 正面討伐を促す。
 * <p>配布範囲: 死亡地点 128 ブロック以内。
 */
public class ResentmentTrait extends MobTrait {

    private static final double DEBUFF_RADIUS = 128.0;

    public ResentmentTrait(ChatFormatting style) {
        super(style);
    }

    /** event handler から呼ばれる: 被弾累積 (= player 由来 / その他 を分類) */
    public static void onDamaged(LivingEntity entity, LivingDamageEvent event) {
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        if (!cap.hasTrait(DIV.gtcsolo.l2.ModL2Traits.RESENTMENT.get())) return;
        Data data = cap.getOrCreateData(
                DIV.gtcsolo.l2.ModL2Traits.RESENTMENT.get().getRegistryName(), Data::new);
        float amt = event.getAmount();
        if (event.getSource().getEntity() instanceof Player) {
            data.playerDamage += amt;
        } else {
            data.otherDamage += amt;
        }
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        float total = data.playerDamage + data.otherDamage;
        if (total <= 0) return;
        if (data.playerDamage / total >= 0.5f) return; // player 由来 50% 以上 → 通常討伐

        // デバフ配布: 128 ブロック以内の全 player に
        AABB area = new AABB(entity.blockPosition()).inflate(DEBUFF_RADIUS);
        int duration = (40 + 20 * level) * 20; // (40 + 20n) 秒
        for (Player p : entity.level().getEntitiesOfClass(Player.class, area)) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, 2));
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, duration / 4, 1));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration / 4, 0));
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public float playerDamage = 0;
        @SerialClass.SerialField
        public float otherDamage = 0;
    }
}
