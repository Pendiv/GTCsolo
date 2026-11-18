package DIV.gamacrafting.mixin.desk;

import DIV.gamacrafting.content.crafting.desk.DeskScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "renderSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void gamacrafting$beforeSlotItem(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        if ((Object) this instanceof DeskScreen screen) {
            float partialTick = Minecraft.getInstance().getPartialTick();
            if (screen.hasSlotTransform(slot, partialTick)) {
                graphics.pose().pushPose();
                screen.applySlotTransform(graphics, slot, partialTick);
            }
        }
    }

    @Inject(method = "renderSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V",
            shift = At.Shift.AFTER))
    private void gamacrafting$afterSlotItem(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        if ((Object) this instanceof DeskScreen screen && screen.hasSlotTransform(slot, Minecraft.getInstance().getPartialTick())) {
            graphics.pose().popPose();
        }
    }
}
