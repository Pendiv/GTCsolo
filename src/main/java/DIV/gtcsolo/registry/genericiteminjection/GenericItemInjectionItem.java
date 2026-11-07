package DIV.gtcsolo.registry.genericiteminjection;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Plain functional shell for a generic injected appearance.
 */
public final class GenericItemInjectionItem extends Item {

    private final GenericItemInjection declaration;

    public GenericItemInjectionItem(Properties properties, GenericItemInjection declaration) {
        super(properties);
        this.declaration = declaration;
    }

    public GenericItemInjection declaration() {
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
