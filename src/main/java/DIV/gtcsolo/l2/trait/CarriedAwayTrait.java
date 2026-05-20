package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * [60] Carried Away (調子に乗る) — プレイヤーから攻撃を受けるまで、 移動速度が大幅に上昇する。
 *
 * <p>初被弾で除去。 modifier の有無が state を兼ねる (= 別途 cap data 不要)。
 * <p>速度上昇 = +50% × lv (MULTIPLY_BASE、 lv1 +50%、 lv5 +250%)。
 */
public class CarriedAwayTrait extends MobTrait {

    private static final UUID MOD_SPEED = UUID.fromString("ca771ed4-ace4-4f37-9b6a-7d8e1c2a3b4c");
    private static final double SPEED_PER_LEVEL = 0.5;

    public CarriedAwayTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        AttributeInstance inst = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;
        if (inst.getModifier(MOD_SPEED) != null) return;
        inst.addPermanentModifier(new AttributeModifier(
                MOD_SPEED, "gtcsolo.carried_away", SPEED_PER_LEVEL * lv,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) return;
        AttributeInstance inst = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;
        AttributeModifier old = inst.getModifier(MOD_SPEED);
        if (old != null) inst.removeModifier(old);
    }
}
