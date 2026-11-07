package DIV.gtcsolo.render;

import DIV.gtcsolo.machine.IndustrialRockBreakerMachine;
import DIV.gtcsolo.machine.StoneGeneratorGeometry;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.renderer.block.FluidBlockRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.Set;

/** Native fluid textures and block models; animation never places blocks or schedules fluid ticks. */
public final class IndustrialRockBreakerRenderer extends WorkableCasingMachineRenderer {
    private final FluidBlockRenderer fluids = FluidBlockRenderer.Builder.create().getRenderer();

    public IndustrialRockBreakerRenderer() {
        super(new ResourceLocation("gtcsolo", "block/casings/industrial/nimonic_80a_heatproof_casing"),
                new ResourceLocation("gtceu", "block/machines/rock_crusher"), false);
    }

    @Override public boolean hasTESR(BlockEntity entity) { return true; }
    @Override public boolean isGlobalRenderer(BlockEntity entity) { return true; }
    @Override public int getViewDistance() { return 32; }

    @Override @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity entity, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        super.render(entity, partialTicks, pose, buffer, light, overlay);
        if (!(entity instanceof IMachineBlockEntity holder)
                || !(holder.getMetaMachine() instanceof IndustrialRockBreakerMachine machine)) return;
        boolean preview = entity.getLevel() instanceof TrackedDummyWorld;
        if (!preview && !machine.isFormed()) return;
        Direction front = machine.getFrontFacing();
        if (front.getAxis() == Direction.Axis.Y) return;
        if (ConfigHolder.INSTANCE.client.renderer.renderFluids) {
            Fluid first = preview ? Fluids.LAVA : machine.getDisplayFluidA().getFluid();
            Fluid second = preview ? Fluids.WATER : machine.getDisplayFluidB().getFluid();
            drawFluid(first, StoneGeneratorGeometry.offsets('B', front), pose, buffer, overlay);
            drawFluid(second, StoneGeneratorGeometry.offsets('H', front), pose, buffer, overlay);
        }
        var output = machine.getDisplayOutput();
        var state = preview ? Blocks.COBBLESTONE.defaultBlockState()
                : output.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState() : null;
        if (state == null) return;
        double time = preview ? System.currentTimeMillis() / 50.0 : entity.getLevel().getGameTime() + partialTicks;
        double phase = preview || machine.isActive() ? (time / 20.0) % 1.0 : 0;
        var back = front.getOpposite();
        var outputs = StoneGeneratorGeometry.offsets('G', front);
        for (BlockPos offset : outputs) {
            // Carry one block toward the front each second; recycle the leading block at the rear.
            double index = offset.getX() * back.getStepX() + offset.getZ() * back.getStepZ() - 1;
            double shift = index < phase ? outputs.size() - phase : -phase;
            pose.pushPose();
            try {
                pose.translate(offset.getX() + back.getStepX() * shift + .1,
                        offset.getY() + .1, offset.getZ() + back.getStepZ() * shift + .1);
                pose.scale(.8f, .8f, .8f);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffer, light, overlay);
            } finally {
                pose.popPose();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawFluid(Fluid fluid, Set<BlockPos> offsets, PoseStack pose, MultiBufferSource buffer, int overlay) {
        if (fluid == Fluids.EMPTY || offsets.isEmpty()) return;
        var extensions = IClientFluidTypeExtensions.of(fluid);
        // A flowing world block is unnecessary: GT's fluid renderer reads the original fluid's atlas sprites.
        if (extensions.getStillTexture() == null) return;
        var texture = extensions.getFlowingTexture() == null ? RenderUtil.FluidTextureType.STILL
                : RenderUtil.FluidTextureType.FLOWING;
        pose.pushPose();
        try {
            fluids.drawBlocks(offsets, pose.last().pose(),
                    buffer.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)),
                    fluid, texture, overlay, LightTexture.FULL_BRIGHT);
        } finally {
            pose.popPose();
        }
    }
}
