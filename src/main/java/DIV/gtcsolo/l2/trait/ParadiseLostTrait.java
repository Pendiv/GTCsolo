package DIV.gtcsolo.l2.trait;

import com.google.common.collect.Multimap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [05] Paradise Lost — 攻撃者の武器が提供する ATTACK_DAMAGE attribute 分をダメージから減算。
 *
 * <p>武器の base 攻撃力のみ対象。 エンチャント・affix は対象外。
 */
public class ParadiseLostTrait extends MobTrait {

    public ParadiseLostTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return;

        Multimap<Attribute, AttributeModifier> mods = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND);
        double weaponAtk = 0.0;
        for (AttributeModifier mod : mods.get(Attributes.ATTACK_DAMAGE)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                weaponAtk += mod.getAmount();
            }
        }
        if (weaponAtk <= 0) return;

        float reduced = Math.max(0f, event.getAmount() - (float) weaponAtk);
        event.setAmount(reduced);
    }
}
