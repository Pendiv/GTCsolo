package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * [30] High Altitude (超高度) — 体力 4 倍 + 受けるダメージ 3 倍。
 *
 * <p>HP: MAX_HEALTH に +3.0 ×lv (MULTIPLY_BASE) を加算 (= lv1 で +300%、 全レベル等倍ではない)。
 * <p>仕様文に「4 倍」 とある単純解釈で lv 関係なく一律 4 倍にしてもよいが、 ここでは lv で
 * スケールさせる安全側設計。
 *
 * <p>被ダメ倍率: onHurtByOthers で event.amount を 3.0 倍 (= lv 不変)。
 */
public class HighAltitudeTrait extends MobTrait {

    private static final UUID MOD_HP = UUID.fromString("a1107017-d101-1a13-1110-100110100000");
    private static final double HP_FACTOR_PER_LEVEL = 3.0;
    private static final float DAMAGE_TAKEN_MULT = 3.0f;

    public HighAltitudeTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        AttributeInstance inst = mob.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        if (inst.getModifier(MOD_HP) != null) return;
        inst.addPermanentModifier(new AttributeModifier(
                MOD_HP, "gtcsolo.high_altitude", HP_FACTOR_PER_LEVEL * lv,
                AttributeModifier.Operation.MULTIPLY_BASE));
        mob.setHealth(mob.getMaxHealth());
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        event.setAmount(event.getAmount() * DAMAGE_TAKEN_MULT);
    }
}
