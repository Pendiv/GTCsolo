package DIV.gtcsolo.integration.curios;

import DIV.gtcsolo.registry.UtilityEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

/** Loaded only when Curios is present. */
public final class GleamRayRingIntegration {
    public static final int AMPLIFIER = 4;
    public static final int DURATION = 300;
    private GleamRayRingIntegration() {}
    public static Item createItem() { return new Ring(); }

    public static void maintainEffect(LivingEntity wearer) {
        if(wearer.level().isClientSide()||!wearer.isAlive())return;
        var effect=UtilityEffects.GLEAM_RAY.get();
        var current=wearer.getEffect(effect);
        if(current!=null&&(current.getAmplifier()>AMPLIFIER
                ||current.getAmplifier()==AMPLIFIER&&(current.isInfiniteDuration()||current.getDuration()>DURATION-20)))return;
        wearer.addEffect(new MobEffectInstance(effect,DURATION,AMPLIFIER,true,false,true));
    }

    private static final class Ring extends Item implements ICurioItem {
        Ring() { super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
        @Override public void curioTick(SlotContext context,ItemStack stack) {
            if(!context.cosmetic())maintainEffect(context.entity());
        }
        @Override public void onEquip(SlotContext context,ItemStack previous,ItemStack stack) {
            if(!context.cosmetic())maintainEffect(context.entity());
        }
        @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) { return true; }
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
            lines.add(Component.translatable("item.gtcsolo.gleam_ray_ring.effect"));
        }
    }
}
