package DIV.gtcsolo.render.halo;

import com.mojang.blaze3d.vertex.PoseStack;

import committee.nova.mods.avaritia.api.client.model.bakedmodels.WrappedItemModel;
import committee.nova.mods.avaritia.api.client.render.buffer.AlphaOverrideVertexConsumer;
import committee.nova.mods.avaritia.api.client.util.TransformUtils;
import committee.nova.mods.avaritia.api.iface.IToolTransform;
import committee.nova.mods.avaritia.client.model.loader.base.HaloUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * A Re-Avaritia wrapped item model that animates only the halo's hue.
 */
public final class RainbowHaloBakedModel extends WrappedItemModel {

    private static final int HUE_STEPS = 360;

    private final Random random = new Random();
    private final TextureAtlasSprite haloSprite;
    private final RainbowHaloSetting setting;
    private final BakedQuad[] haloQuads = new BakedQuad[HUE_STEPS];

    public RainbowHaloBakedModel(
            BakedModel wrapped,
            TextureAtlasSprite haloSprite,
            RainbowHaloSetting setting) {
        super(wrapped);
        this.haloSprite = haloSprite;
        this.setting = setting;
    }

    @Override
    public void renderItem(
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            ItemModelShaper itemModelShaper,
            TextureManager textureManager) {
        this.parentState = stack.getItem() instanceof IToolTransform ?
                TransformUtils.DEFAULT_TOOL :
                TransformUtils.DEFAULT_ITEM;

        if (context == ItemDisplayContext.GUI) {
            Minecraft.getInstance().getItemRenderer().renderQuadList(
                    poseStack,
                    bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(stack, true)),
                    List.of(currentHaloQuad()),
                    stack,
                    packedLight,
                    packedOverlay);

            if (this.setting.halo().pulse()) {
                poseStack.pushPose();
                double scale = this.random.nextDouble() * 0.15D + 0.95D;
                double translation = (1.0D - scale) / 2.0D;
                poseStack.translate(translation, translation, 0.0D);
                poseStack.scale((float) scale, (float) scale, 1.0001F);
                this.renderWrapped(
                        stack,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        true,
                        consumer -> new AlphaOverrideVertexConsumer(consumer, 0.6D));
                poseStack.popPose();
            }
        }

        this.renderWrapped(stack, poseStack, bufferSource, packedLight, packedOverlay, true);
    }

    private BakedQuad currentHaloQuad() {
        long phaseMillis = Math.floorMod(Util.getMillis(), this.setting.cycleMillis());
        int hueIndex = (int) (phaseMillis * HUE_STEPS / this.setting.cycleMillis());
        BakedQuad quad = this.haloQuads[hueIndex];
        if (quad == null) {
            float hue = hueIndex / (float) HUE_STEPS;
            int rgb = Mth.hsvToRgb(hue, 1.0F, 1.0F);
            int alpha = this.setting.halo().color() >>> 24;
            int argb = (alpha << 24) | rgb;
            quad = HaloUtils.generateHaloQuad(
                    this.haloSprite,
                    this.setting.halo().size(),
                    argb);
            this.haloQuads[hueIndex] = quad;
        }
        return quad;
    }
}
