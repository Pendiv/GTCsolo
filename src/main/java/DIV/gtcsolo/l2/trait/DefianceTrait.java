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
 * [47] Defiance (反発精神) — 攻撃を受けるたびに ATTACK_DAMAGE が増加。 lv で増加量と max stack 増大。
 *
 * <p>1 stack = +1.0 ATK (ADDITION)、 max stack = 5 × lv (= lv1 で +5、 lv5 で +25 ATK 上限)。
 * <p>State は AttributeModifier の amount に埋め込み、 別途 cap data 不要。
 */
public class DefianceTrait extends MobTrait {

    private static final UUID MOD_ATK = UUID.fromString("de1f1a17-c0de-7a17-9000-d1cea1100001");
    private static final double ATK_PER_STACK = 1.0;
    private static final int MAX_STACK_PER_LEVEL = 5;

    public DefianceTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (event.getAmount() <= 0) return;
        AttributeInstance inst = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (inst == null) return;
        double max = ATK_PER_STACK * MAX_STACK_PER_LEVEL * level;
        AttributeModifier old = inst.getModifier(MOD_ATK);
        double cur = old == null ? 0 : old.getAmount();
        double next = Math.min(cur + ATK_PER_STACK, max);
        if (next == cur) return; // 上限到達
        if (old != null) inst.removeModifier(old);
        inst.addPermanentModifier(new AttributeModifier(
                MOD_ATK, "gtcsolo.defiance", next, AttributeModifier.Operation.ADDITION));
    }
}
