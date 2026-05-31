package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * [30] High Altitude (超高度) — 体力大幅増 + 受けるダメージ増。
 *
 * <p>HP: MAX_HEALTH に (3 + N) の MULTIPLY_BASE modifier (= base × (1 + 3 + N) = 実質 (4 + N) 倍)。
 * <p>被ダメ倍率: onHurtByOthers で event.amount を (300 + 50n)% 倍 (= lv でスケール)。
 */
public class HighAltitudeTrait extends MobTrait {

    private static final UUID MOD_HP = UUID.fromString("a1107017-d101-1a13-1110-100110100000");

    public HighAltitudeTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.MAX_HEALTH, MOD_HP, "gtcsolo.high_altitude",
                3.0 + lv, AttributeModifier.Operation.MULTIPLY_BASE);  // MULTIPLY_BASE (3 + N) → 実質 (4 + N) 倍
        mob.setHealth(mob.getMaxHealth());
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        event.setAmount(event.getAmount() * (float) (3.0 + 0.5 * level));  // 被ダメ ×(300 + 50n)%
    }
}
