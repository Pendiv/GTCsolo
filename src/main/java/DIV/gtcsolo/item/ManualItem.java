package DIV.gtcsolo.item;

import DIV.gtcsolo.manual.ManualUIFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * マニュアル (解説書) — gtcsolo の改変・特性・この世界特有の要素を記した UI を開くアイテム。
 *
 * <p>右クリックで {@link ManualUIFactory} 経由の LDLib ModularUI を開く
 * ({@code /gtcsolo manual} コマンドと同一経路)。 レシピは「本 + レンチ (耐久 -1)」。
 */
public class ManualItem extends Item {

    public ManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ManualUIFactory.INSTANCE.openUI(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
