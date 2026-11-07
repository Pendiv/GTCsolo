package DIV.gtcsolo.mixin;

import com.gregtechceu.gtceu.client.TooltipsHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** Optional localized descriptions shared by native fluid slots, JEI and fluid containers. */
@Mixin(value = TooltipsHandler.class, remap = false)
public abstract class FluidDescriptionMixin {
    @Inject(method = "appendFluidTooltips", at = @At("TAIL"), require = 1)
    private static void gtcsolo$describeFluid(FluidStack stack, Consumer<Component> tooltip,
                                             TooltipFlag flag, CallbackInfo ci) {
        var id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        if (id == null) return;
        String prefix = id.toLanguageKey("gtcsolo.fluid.description");
        var language = Language.getInstance();
        for (int line = 1; line <= 3; line++) {
            String key = prefix + "." + line;
            if (!language.has(key)) break;
            tooltip.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
