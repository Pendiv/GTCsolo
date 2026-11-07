package DIV.gtcsolo.registry.iteminjection;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Plain item shell for an injected appearance. Functional subclasses can be added later without changing
 * the texture declaration/generation pipeline.
 */
public final class KnownItemInjectionItem extends Item {

    private final KnownItemInjection declaration;

    public KnownItemInjectionItem(Properties properties, KnownItemInjection declaration) {
        super(properties);
        this.declaration = declaration;
    }

    public KnownItemInjection declaration() {
        return declaration;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(declaration.displayName());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal(declaration.variantLabel()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(
                declaration.method().displayName() + " / #" + declaration.targetHex())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Source: " + declaration.sourceItemId())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
