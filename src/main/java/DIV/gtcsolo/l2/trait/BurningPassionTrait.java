package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * [59] Burning Passion (燃え盛る情熱) — 一度炎を受けると以後永続燃焼。
 * 燃焼中は継続回復・与ダメ強化・炎耐性。
 *
 * <p>state: cap data の {@code ignited} フラグ (= 永続化)。
 * <p>初回 IS_FIRE 被弾で ignited=true。 以降 tick で fire ticks 維持、 周期回復、 ATK modifier 付与。
 * <p>炎耐性: ignited 後の IS_FIRE 被弾を cancel。
 *
 * <p>L2H FIERY trait と機能一部重複 — 排他は運用判断 (今は実装上は両立可)。
 */
public class BurningPassionTrait extends MobTrait {

    private static final UUID MOD_ATK = UUID.fromString("b08a51e2-fa11-4f7b-9c0a-7e8f1d2c3b51");

    public BurningPassionTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!MobTraitCap.HOLDER.isProper(entity)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        DamageSource src = event.getSource();
        boolean fireDamage = src.is(DamageTypeTags.IS_FIRE);

        if (!data.ignited) {
            if (fireDamage) {
                data.ignited = true;
                applyAtkBuff(entity, level);
            }
            return;
        }

        // 既に ignited: 以降の fire damage は完全無効化
        if (fireDamage) {
            event.setCanceled(true);
        }
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (!data.ignited) return;

        // 永続燃焼の visual を維持 (= 5 秒以上残り火 tick を保つ)
        if (mob.getRemainingFireTicks() < 100) {
            mob.setRemainingFireTicks(200);
        }
        // 周期回復 (1 秒に 1 度)
        if (mob.tickCount % 20 == 0) {
            mob.heal(mob.getMaxHealth() * 0.0035f * level);  // 0.35n% /秒 永続回復
        }
        // ATK buff の付け直し保証 (= chunk reload 後に modifier が消える場合の保険。 冪等)
        applyAtkBuff(mob, level);
    }

    private static void applyAtkBuff(LivingEntity mob, int level) {
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.burning_passion",
                0.10 + 0.05 * level, AttributeModifier.Operation.MULTIPLY_BASE);  // +(10 + 5n)%
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean ignited = false;
    }
}
