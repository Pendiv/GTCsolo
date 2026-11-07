package DIV.gtcsolo.mixin;

import DIV.gtcsolo.machine.pump.PumpAssistTrait;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.primitive.PrimitivePumpMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve the original structure and passive production; add only paid supplemental water. */
@Mixin(value = PrimitivePumpMachine.class, remap = false)
public abstract class PrimitivePumpAssistMixin {
    @Shadow private int biomeModifier;
    @Shadow private NotifiableFluidTank fluidTank;
    @Unique private PumpAssistTrait gtcsolo$pumpAssist;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtcsolo$attachAssist(IMachineBlockEntity holder, CallbackInfo ci) {
        gtcsolo$pumpAssist = new PumpAssistTrait((PrimitivePumpMachine) (Object) this);
    }

    @Inject(method = "produceWater", at = @At("TAIL"))
    private void gtcsolo$produceAssistedWater(CallbackInfo ci) {
        PrimitivePumpMachine pump = (PrimitivePumpMachine) (Object) this;
        if (pump.isRemote() || !pump.isFormed() || pump.getMultiblockState().hasError()
                || biomeModifier <= 0 || fluidTank == null) {
            gtcsolo$pumpAssist.reset();
            return;
        }
        gtcsolo$pumpAssist.tick(pump.getFluidProduction(), fluidTank);
    }

    @Inject(method = "resetState", at = @At("TAIL"))
    private void gtcsolo$resetAssist(CallbackInfo ci) {
        if (gtcsolo$pumpAssist != null) gtcsolo$pumpAssist.reset();
    }
}
