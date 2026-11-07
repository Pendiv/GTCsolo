package DIV.gtcsolo.mixin;

import DIV.gtcsolo.registry.tool.fullcustom.MaCrystalToolItem;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.client.renderer.item.ToolItemRenderer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the supplied handheld models instead of generating material-tinted tool models. */
@Mixin(value = ToolItemRenderer.class, remap = false)
public abstract class MaCrystalToolModelMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void gtcsolo$useSuppliedModel(Item item, GTToolType type, CallbackInfo callback) {
        if (item instanceof MaCrystalToolItem) callback.cancel();
    }
}
