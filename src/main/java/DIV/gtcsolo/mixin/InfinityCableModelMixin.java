package DIV.gtcsolo.mixin;

import DIV.gtcsolo.render.material.InfinityCableTextures;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.common.pipelike.cable.Insulation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Select the Infinity textures before PipeModel registers its atlas listeners. */
@Mixin(value = Insulation.class, remap = false)
public abstract class InfinityCableModelMixin {
    @Inject(method = "createPipeModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$infinityCableModel(Material material, CallbackInfoReturnable<PipeModel> cir) {
        if (InfinityCableTextures.matches(material)) {
            cir.setReturnValue(InfinityCableTextures.create((Insulation) (Object) this));
        }
    }
}
