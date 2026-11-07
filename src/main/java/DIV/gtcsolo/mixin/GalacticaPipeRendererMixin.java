package DIV.gtcsolo.mixin;

import DIV.gtcsolo.render.material.portal.GalacticaWorldRenderer;
import DIV.gtcsolo.render.material.portal.GalacticaRenderContext;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = PipeBlockRenderer.class, remap = false)
public abstract class GalacticaPipeRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), remap = false)
    private void gtcsolo$observePipe(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction side,
                                     RandomSource random, CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (!GalacticaRenderContext.isPreview() && level != null && pos != null && level.getBlockEntity(pos) instanceof IPipeNode<?, ?> pipe
                && GalacticaWorldRenderer.isGalactica(pipe.getFrameMaterial())) {
            GalacticaWorldRenderer.observePipe(pos);
        }
    }
}
