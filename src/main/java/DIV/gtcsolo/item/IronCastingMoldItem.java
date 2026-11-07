package DIV.gtcsolo.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class IronCastingMoldItem extends Item {
    public static final int USES = 128;

    public IronCastingMoldItem(Properties properties) { super(properties.durability(USES)); }

    public static int remaining(ItemStack stack) {
        return stack.getItem() instanceof IronCastingMoldItem && stack.getCount() == 1
                ? Math.max(0, USES - Math.max(0, stack.getDamageValue())) : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gtcsolo.iron_casting_mold.uses", remaining(stack), USES));
    }
}
