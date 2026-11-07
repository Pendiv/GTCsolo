package DIV.gtcsolo.common;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class TooltipDisplayEvents {

    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        TooltipDisplayHelper.appendTooltip(stack, event.getToolTip());
    }
}