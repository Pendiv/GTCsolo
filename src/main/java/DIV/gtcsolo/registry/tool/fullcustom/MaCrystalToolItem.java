package DIV.gtcsolo.registry.tool.fullcustom;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.tool.GTToolItem;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.IGTToolDefinition;
import com.gregtechceu.gtceu.api.item.tool.MaterialToolTier;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Initializes native tool data even for plain recipe outputs and /give stacks. */
public final class MaCrystalToolItem extends GTToolItem {
    MaCrystalToolItem(
            GTToolType type,
            MaterialToolTier tier,
            Material material,
            IGTToolDefinition definition,
            Properties properties) {
        super(type, tier, material, definition, properties.fireResistant());
    }

    @Override
    public void verifyTagAfterLoad(CompoundTag tag) {
        super.verifyTagAfterLoad(tag);
        initializeTag(tag);
    }

    private void initializeTag(CompoundTag tag) {
        MaToolMode.normalize(tag);
        tag.putBoolean(ToolHelper.UNBREAKABLE_KEY, true);
        tag.putInt("HideFlags", tag.getInt("HideFlags") | 2);
        var tool = tag.getCompound(ToolHelper.TOOL_TAG_KEY);
        tool.putInt(ToolHelper.DURABILITY_KEY, 0);
        tag.put(ToolHelper.TOOL_TAG_KEY, tool);
        MaToolGrowth.updateArea(tag);
    }

    @Override
    public float getTotalToolSpeed(ItemStack stack) {
        return super.getTotalToolSpeed(stack) + MaToolGrowth.level(stack);
    }

    @Override
    public int getDamage(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        var stack = player.getMainHandItem();
        return (!MaToolMode.isPrecision(stack) || MaToolMode.isSuitableTarget(stack, state))
                && super.canAttackBlock(state, level, pos, player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, level, tooltip, flags);
        tooltip.add(Component.translatable("gtcsolo.ma_tool.mode", MaToolMode.get(stack).label()));
        tooltip.add(
                Component.translatable(
                        "gtcsolo.ma_tool.growth",
                        MaToolGrowth.level(stack),
                        MaToolGrowth.remainingBlocks(stack)));
        if (MaToolMode.isPrecision(stack))
            tooltip.add(Component.translatable("gtcsolo.ma_tool.precision_description"));
        tooltip.add(Component.translatable("gtcsolo.ma_tool.switch_hint"));
        tooltip.add(Component.translatable("gtcsolo.ma_tool.area_hint"));
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        // Forge initializes capabilities lazily; ordinary ItemStack construction must not depend on
        // them.
        initializeTag(stack.getOrCreateTag());
        return super.getMaxDamage(stack);
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        initializeTag(stack.getOrCreateTag());
    }
}
