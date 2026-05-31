package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * [60] Carried Away (調子に乗る) — プレイヤーから攻撃を受けるまで、 移動速度が大幅に上昇する。
 *
 * <p>初被弾で除去。 modifier の有無が state を兼ねる (= 別途 cap data 不要)。
 * <p>速度上昇 = (125 + 25n)% (MULTIPLY_BASE、 lv1 +150%、 lv3 +200%)。
 */
public class CarriedAwayTrait extends MobTrait {

    private static final UUID MOD_SPEED = UUID.fromString("ca771ed4-ace4-4f37-9b6a-7d8e1c2a3b4c");
    private static final double SPEED_BASE = 1.25;
    private static final double SPEED_PER_LEVEL = 0.25;  // 移動速度 +(125 + 25n)%

    public CarriedAwayTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, "gtcsolo.carried_away",
                SPEED_BASE + SPEED_PER_LEVEL * lv, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) return;
        L2TraitAttributes.remove(entity, Attributes.MOVEMENT_SPEED, MOD_SPEED);
    }
}
