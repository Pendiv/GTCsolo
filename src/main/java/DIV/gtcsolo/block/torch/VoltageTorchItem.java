package DIV.gtcsolo.block.torch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public final class VoltageTorchItem extends StandingAndWallBlockItem {
    private final VoltageTorchTier tier;

    public VoltageTorchItem(Block standing, Block wall, Properties properties, VoltageTorchTier tier) {
        super(standing, wall, properties, Direction.DOWN);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("tooltip.gtcsolo.voltage_torch.light", VoltageTorchTier.LIGHT_LEVEL)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.gtcsolo.voltage_torch.ward", tier.radius())
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.gtcsolo.voltage_torch.scope")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
