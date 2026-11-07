package DIV.gtcsolo.render;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.FusionReactorRenderer;
import DIV.gtcsolo.machine.fusion.FusionMk4Machine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Keep GT's radius-six plasma ring, moving its centre to this thicker torus. */
public final class FusionMk4Renderer extends FusionReactorRenderer {
    public FusionMk4Renderer(ResourceLocation casing, ResourceLocation controller) {
        super(casing, controller);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack pose, MultiBufferSource buffer,
                       int light, int overlay) {
        if (!(blockEntity instanceof IMachineBlockEntity holder)
                || !(holder.getMetaMachine() instanceof FusionMk4Machine machine)) return;
        var back = RelativeDirection.BACK.getRelativeFacing(machine.getFrontFacing(),
                machine.getUpwardsFacing(), machine.isFlipped());
        // Native MK1-3 renderer places the ring seven blocks behind the controller.
        int offset = FusionMk4Machine.CENTER_DISTANCE - 7;
        pose.pushPose();
        try {
            pose.translate(back.getStepX() * offset, back.getStepY() * offset, back.getStepZ() * offset);
            super.render(blockEntity, partialTicks, pose, buffer, light, overlay);
        } finally {
            pose.popPose();
        }
    }
}
