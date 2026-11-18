package DIV.gamacrafting.helper;

import DIV.gamacrafting.Gamacrafting;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class Renders {
    private Renders() {
    }

    public static void with(PoseStack poseStack, Runnable runnable) {
        poseStack.pushPose();
        try {
            runnable.run();
        } finally {
            poseStack.popPose();
        }
    }

    public static MultiBufferSource.BufferSource bufferSource() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static VertexConsumer getBuffer(RenderType type) {
        return bufferSource().getBuffer(type);
    }

    public static void endBatch(RenderType type) {
        bufferSource().endBatch(type);
    }

    public static void cube(Matrix4f matrix, VertexConsumer builder, float radius, int color) {
        float min = -radius;
        float max = radius;
        int alpha = color >>> 24;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;

        builder.vertex(matrix, min, min, min).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();
        builder.vertex(matrix, max, min, min).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();
        builder.vertex(matrix, max, min, max).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();
        builder.vertex(matrix, min, min, max).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();

        builder.vertex(matrix, min, max, min).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, min, max, max).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, max, max, max).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, max, max, min).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();

        builder.vertex(matrix, min, min, min).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();
        builder.vertex(matrix, min, max, min).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();
        builder.vertex(matrix, max, max, min).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();
        builder.vertex(matrix, max, min, min).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();

        builder.vertex(matrix, min, min, max).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();
        builder.vertex(matrix, max, min, max).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();
        builder.vertex(matrix, max, max, max).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();
        builder.vertex(matrix, min, max, max).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();

        builder.vertex(matrix, min, min, min).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();
        builder.vertex(matrix, min, min, max).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();
        builder.vertex(matrix, min, max, max).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();
        builder.vertex(matrix, min, max, min).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();

        builder.vertex(matrix, max, min, min).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
        builder.vertex(matrix, max, max, min).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
        builder.vertex(matrix, max, max, max).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
        builder.vertex(matrix, max, min, max).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
    }

    public static void hollowTriangle(Matrix4f matrix, VertexConsumer builder, float radius, float width, int color) {
        float angle = (float) (Math.PI * 2 / 3);
        float innerRadius = radius - width;
        int alpha = color >>> 24;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;

        for (int side = 1; side <= 3; side++) {
            float x = Mth.cos(side * angle);
            float y = Mth.sin(side * angle);
            float nextX = Mth.cos((side + 1) * angle);
            float nextY = Mth.sin((side + 1) * angle);
            builder.vertex(matrix, x * radius, y * radius, 0).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, nextX * radius, nextY * radius, 0).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, nextX * innerRadius, nextY * innerRadius, 0).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x * innerRadius, y * innerRadius, 0).color(red, green, blue, alpha).endVertex();
        }
    }

    public static final class Type extends RenderType {
        private Type(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                     boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        public static final RenderType HIGHLIGHT = create(Gamacrafting.MOD_ID + ":highlight",
                DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS, 256, true, true,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(NO_TEXTURE)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        public static final RenderType LIGHTNING_NO_CULL = lightning("lightning_no_cull", WEATHER_TARGET);
        public static final RenderType GUI_LIGHTNING_NO_CULL = lightning("gui_lightning_no_cull", MAIN_TARGET);

        private static RenderType lightning(String name, OutputStateShard target) {
            return create(Gamacrafting.MOD_ID + ":" + name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                    256, false, true, CompositeState.builder()
                            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setOutputState(target)
                            .setCullState(NO_CULL)
                            .createCompositeState(false));
        }
    }
}
