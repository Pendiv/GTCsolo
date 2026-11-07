package DIV.gtcsolo.mixin;

import DIV.gtcsolo.render.material.portal.GalacticaBlockModel;
import DIV.gtcsolo.render.material.portal.GalacticaRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.lowdragmc.lowdraglib.utils.DummyWorld;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockRenderDispatcher.class)
public abstract class GalacticaPreviewMixin {
    @Redirect(method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", remap = false), remap = false)
    private void gtcsolo$previewBatched(ModelBlockRenderer renderer, BlockAndTintGetter level, BakedModel model,
                                       BlockState state, BlockPos pos, PoseStack pose, VertexConsumer out, boolean check,
                                       RandomSource random, long seed, int overlay, ModelData data, RenderType type) {
        boolean preview = level instanceof DummyWorld;
        if (preview) GalacticaRenderContext.enterPreview();
        try { renderer.tesselateBlock(level, model, state, pos, pose, out, check, random, seed, overlay, data, type); }
        finally { if (preview) GalacticaRenderContext.leavePreview(); }
    }

    @Redirect(method = "renderSingleBlock(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;renderModel(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/resources/model/BakedModel;FFFIILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", remap = false), remap = false)
    private void gtcsolo$previewSingle(ModelBlockRenderer renderer, PoseStack.Pose pose, VertexConsumer out,
                                      BlockState state, BakedModel model, float red, float green, float blue, int light,
                                      int overlay, ModelData data, RenderType type) {
        boolean preview = model instanceof GalacticaBlockModel;
        if (preview) GalacticaRenderContext.enterPreview();
        try { renderer.renderModel(pose, out, state, model, red, green, blue, light, overlay, data, type); }
        finally { if (preview) GalacticaRenderContext.leavePreview(); }
    }
}

