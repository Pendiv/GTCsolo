package DIV.gtcsolo.mixin;

import DIV.gtcsolo.registry.tool.fullcustom.FullCustomGtToolDeclaration;
import DIV.gtcsolo.registry.tool.fullcustom.FullCustomGtTools;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.GTAxeItem;
import com.gregtechceu.gtceu.api.item.tool.GTHoeItem;
import com.gregtechceu.gtceu.api.item.tool.GTShovelItem;
import com.gregtechceu.gtceu.api.item.tool.GTSwordItem;
import com.gregtechceu.gtceu.api.item.tool.GTToolItem;

import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GTCEu normally formats every tool as "{Material name} {Tool name}".
 * Full custom tools own their complete display name, so bypass that one formatting step only.
 */
@Mixin(value = {
        GTToolItem.class,
        GTSwordItem.class,
        GTShovelItem.class,
        GTAxeItem.class,
        GTHoeItem.class
})
public class FullCustomGtToolNameMixin {

    @Inject(method = "getDescriptionId", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$fullCustomDescriptionId(CallbackInfoReturnable<String> cir) {
        FullCustomGtToolDeclaration declaration =
                FullCustomGtTools.declarationFor((IGTTool) (Object) this);
        if (declaration != null) {
            cir.setReturnValue(declaration.id().toLanguageKey("item"));
        }
    }

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$fullCustomDescription(CallbackInfoReturnable<Component> cir) {
        IGTTool tool = (IGTTool) (Object) this;
        FullCustomGtToolDeclaration declaration = FullCustomGtTools.declarationFor(tool);
        if (declaration != null) {
            cir.setReturnValue(Component.literal(FullCustomGtTools.localizedName(tool)));
        }
    }
}
