package DIV.gtcsolo.mixin;

import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use the default during early splash reload, then honor the loaded Botania client setting. */
@Pseudo
@Mixin(targets = "vazkii.botania.forge.ForgeBotaniaConfig$Client", remap = false)
public class BotaniaSplashConfigMixin {
    @Shadow @Final private ForgeConfigSpec.BooleanValue splashesEnabled;

    @Inject(method = "splashesEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$guardEarlyConfig(CallbackInfoReturnable<Boolean> cir) {
        try {
            cir.setReturnValue(splashesEnabled.get());
        } catch (IllegalStateException configNotLoaded) {
            cir.setReturnValue(true);
        }
    }
}
