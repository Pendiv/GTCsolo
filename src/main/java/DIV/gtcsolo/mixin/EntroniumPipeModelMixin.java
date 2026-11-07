package DIV.gtcsolo.mixin;

import DIV.gtcsolo.block.EntroniumBlock;
import DIV.gtcsolo.render.material.EntroniumPipeModels;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.common.pipelike.cable.Insulation;
import com.gregtechceu.gtceu.common.pipelike.fluidpipe.FluidPipeType;
import com.gregtechceu.gtceu.common.pipelike.item.ItemPipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {FluidPipeType.class, ItemPipeType.class, Insulation.class}, remap = false)
public abstract class EntroniumPipeModelMixin {
    @Inject(method = "createPipeModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$entroniumTextures(Material material, CallbackInfoReturnable<PipeModel> cir) {
        if (!EntroniumBlock.matches(material)) return;
        Object type = this;
        if (type instanceof FluidPipeType fluid) cir.setReturnValue(EntroniumPipeModels.create(fluid));
        else if (type instanceof ItemPipeType item) cir.setReturnValue(EntroniumPipeModels.create(item));
        else if (type instanceof Insulation cable) cir.setReturnValue(EntroniumPipeModels.create(cable));
    }
}
