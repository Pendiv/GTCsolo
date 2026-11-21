package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.InputVoltageReceiver;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.common.blockentity.CableBlockEntity;
import com.gregtechceu.gtceu.common.pipelike.cable.EnergyNetHandler;
import com.gregtechceu.gtceu.common.pipelike.cable.EnergyRoutePath;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EnergyNetHandler.class, remap = false)
public abstract class CableMachineDemandMixin {
    @WrapOperation(method = "acceptEnergyFromNetwork", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/capability/IEnergyContainer;acceptEnergyFromNetwork(Lnet/minecraft/core/Direction;JJ)J"))
    private long gtcsolo$limitRoute(IEnergyContainer dest, Direction side, long voltage, long amps,
                                    Operation<Long> original, @Local EnergyRoutePath path) {
        if (dest instanceof InputVoltageReceiver receiver && receiver.gtcsolo$machine() != null) {
            for (CableBlockEntity cable : path.getPath()) {
                long used = ((CableLoadAccess) cable).gtcsolo$getAmperageCounter().getLast(cable.getLevel());
                amps = Math.min(amps, cable.getMaxAmperage() - used);
                if (amps <= 0) return 0;
            }
        }
        return original.call(dest, side, voltage, amps);
    }
}
