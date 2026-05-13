package DIV.gtcsolo.client;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.entity.IceProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * IceProjectile を「氷ブロック」として描画。
 * 内部的には Mojang の BlockRenderer.renderSingleBlock を使って block model を引っ張ってきている。
 */
public class IceProjectileRenderer extends EntityRenderer<IceProjectile> {

    private static final ResourceLocation BLOCKS_ATLAS =
            new ResourceLocation("textures/atlas/blocks.png");

    public IceProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(IceProjectile entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // ブロック原点 (0,0,0) を中央に寄せる
        poseStack.translate(-0.5, 0.0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.ICE.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(IceProjectile entity) {
        return BLOCKS_ATLAS;
    }
}
