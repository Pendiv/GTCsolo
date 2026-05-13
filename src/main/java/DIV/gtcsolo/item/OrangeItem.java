package DIV.gtcsolo.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/**
 * ミカン (Orange)。
 *   - 右クリック保持で食べる: 満腹度 3 / 隠し満腹度 3 (saturationMod 0.5)
 *   - 左クリックで投擲 (= OrangeClickHandler が LeftClickEmpty/Block/AttackEntity を捕捉して
 *     OrangeThrowPacket を server に送り、 server 側で OrangeProjectile を spawn する)
 */
public class OrangeItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(3)
            .saturationMod(0.5f)
            .build();

    public OrangeItem(Properties properties) {
        super(properties.food(FOOD));
    }
}
