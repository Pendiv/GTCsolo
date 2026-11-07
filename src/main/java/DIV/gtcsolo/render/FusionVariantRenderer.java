package DIV.gtcsolo.render;

import DIV.gtcsolo.machine.fusion.FusionVariantMachine;
import DIV.gtcsolo.machine.fusion.MegaFusionMachine;
import DIV.gtcsolo.machine.fusion.FusionVariantSettings;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.FusionReactorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Reuse the native plasma colour/fade/bloom, with its centre and radius fitted to the new torus. */
public final class FusionVariantRenderer extends FusionReactorRenderer {
    private final int viewDistance;
    public FusionVariantRenderer(ResourceLocation casing,ResourceLocation controller) { this(casing,controller,64); }
    public FusionVariantRenderer(ResourceLocation casing,ResourceLocation controller,int viewDistance) {
        super(casing,controller); this.viewDistance=viewDistance;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity entity,float partialTicks,PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        if (!(entity instanceof IMachineBlockEntity holder)
                || !(holder.getMetaMachine() instanceof FusionReactorMachine machine)) return;
        FusionVariantSettings settings;
        if (machine instanceof FusionVariantMachine variant) settings=variant.settings();
        else if (machine instanceof MegaFusionMachine) settings=FusionVariantSettings.MEGA_MK4;
        else return;
        var layout=settings.pattern().layout();
        var front=machine.getFrontFacing(); var upwards=machine.getUpwardsFacing(); boolean flipped=machine.isFlipped();
        var right=RelativeDirection.RIGHT.getRelativeFacing(front,upwards,flipped);
        var up=RelativeDirection.UP.getRelativeFacing(front,upwards,flipped);
        var back=RelativeDirection.BACK.getRelativeFacing(front,upwards,flipped);
        double dx=(layout.width-1)/2.0-layout.anchorX;
        double dy=(layout.height-1)/2.0-layout.anchorY;
        double dz=(layout.depth-1)/2.0-layout.anchorZ;
        float scale=(float)(layout.radius/6.0);
        pose.pushPose();
        try {
            pose.translate(.5+right.getStepX()*dx+up.getStepX()*dy+back.getStepX()*dz,
                    .5+right.getStepY()*dx+up.getStepY()*dy+back.getStepY()*dz,
                    .5+right.getStepZ()*dx+up.getStepZ()*dy+back.getStepZ()*dz);
            pose.scale(up.getAxis()==Direction.Axis.X ? 1:scale,
                    up.getAxis()==Direction.Axis.Y ? 1:scale,up.getAxis()==Direction.Axis.Z ? 1:scale);
            pose.translate(-(.5+back.getStepX()*7),-(.5+back.getStepY()*7),-(.5+back.getStepZ()*7));
            super.render(entity,partialTicks,pose,buffer,light,overlay);
        } finally { pose.popPose(); }
    }

    @Override @OnlyIn(Dist.CLIENT)
    public int getViewDistance() { return viewDistance; }
}
