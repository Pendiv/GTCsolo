package DIV.gtcsolo.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tropical Candy = 食べると満腹度 20/20 回復、 体力 +6 (= 3 ハート分) 回復する菓子アイテム。
 * saturationMod = saturation / (nutrition * 2) より、 nutrition=20 + saturation=20 → mod=0.5
 */
public class TropicalCandyItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(20)
            .saturationMod(0.5f)
            .build();

    private static final float HEAL_AMOUNT = 6.0f;

    public TropicalCandyItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player) {
            player.heal(HEAL_AMOUNT);
        }
        return result;
    }

    /**
     * キャンディーで殴ったとき = 秘剣 Mode 3 と同じ上方吹き飛ばし。
     * ロジックは SecretSwordItem.applyKnockup と共有。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        SecretSwordItem.applyKnockup(target);
        return false;
    }
}
