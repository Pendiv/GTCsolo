package DIV.gtcsolo.mixin;

import DIV.gtcsolo.registry.tool.fullcustom.MaCrystalToolItem;
import DIV.gtcsolo.registry.tool.fullcustom.MaToolGrowth;
import DIV.gtcsolo.registry.tool.fullcustom.MaToolMode;

import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.item.tool.aoe.AoESymmetrical;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use the native area calculation and preview while preserving stored bulk-mode dimensions. */
@Mixin(value = ToolHelper.class, remap = false)
public abstract class MaToolAreaMixin {
    @Inject(method = "getMaxAoEDefinition", at = @At("HEAD"))
    private static void gtcsolo$growthLimit(
            ItemStack stack, CallbackInfoReturnable<AoESymmetrical> callback) {
        if (stack.getItem() instanceof MaCrystalToolItem)
            MaToolGrowth.updateArea(stack.getOrCreateTag());
    }

    @Inject(method = "getAoEDefinition", at = @At("HEAD"), cancellable = true)
    private static void gtcsolo$modeArea(
            ItemStack stack, CallbackInfoReturnable<AoESymmetrical> callback) {
        if (stack.getItem() instanceof MaCrystalToolItem
                && MaToolMode.get(stack) != MaToolMode.BULK)
            callback.setReturnValue(AoESymmetrical.none());
    }
}
