package DIV.gtcsolo.block.torch;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import javax.annotation.Nullable;
import java.util.List;

public final class VoltageLightItem extends BlockItem {
    private final VoltageTorchTier tier;

    public VoltageLightItem(Block block, Properties properties, VoltageTorchTier tier) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("tooltip.gtcsolo.voltage_torch.light", 15).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.gtcsolo.voltage_torch.ward", tier.radius()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.gtcsolo.voltage_torch.scope").withStyle(ChatFormatting.DARK_GRAY));
    }
}
