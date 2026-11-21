package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.InputVoltageReceiver;
import DIV.gtcsolo.integration.mekanism.MachineVoltage;
import DIV.gtcsolo.integration.mekanism.PacketInputAccess;
import com.gregtechceu.gtceu.api.capability.compat.EUToFEProvider;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.Direction;
import net.minecraftforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EUToFEProvider.GTEnergyWrapper.class, remap = false)
public abstract class GregTechInputVoltageMixin implements InputVoltageReceiver {
    @Shadow @Final private IEnergyStorage energyStorage;

    @Override
    public TileEntityMekanism gtcsolo$machine() {
        return energyStorage instanceof InputVoltageReceiver receiver ? receiver.gtcsolo$machine() : null;
    }

    @Inject(method = "getInputVoltage", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$ratedVoltage(CallbackInfoReturnable<Long> result) {
        TileEntityMekanism tile = gtcsolo$machine();
        if (tile != null) result.setReturnValue(MachineVoltage.rating(tile));
    }

    @Inject(method = "getInputAmperage", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$ratedAmps(CallbackInfoReturnable<Long> result) {
        TileEntityMekanism tile = gtcsolo$machine();
        if (tile != null) result.setReturnValue((long) MachineVoltage.inputAmps(tile));
    }

    @Inject(method = "acceptEnergyFromNetwork", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$acceptPackets(Direction side, long voltage, long offered, CallbackInfoReturnable<Long> result) {
        TileEntityMekanism tile = gtcsolo$machine();
        if (tile == null) return;
        MachineVoltage machine = (MachineVoltage) tile;
        long amps = Math.min(offered, machine.gtcsolo$remainingAmps());
        if (voltage <= 0 || voltage > MachineVoltage.rating(tile) || amps <= 0) {
            result.setReturnValue(0L);
            return;
        }
        long accepted = ((PacketInputAccess) energyStorage).gtcsolo$insertPackets(voltage, amps);
        if (accepted > 0) machine.gtcsolo$acceptAmps(voltage, accepted);
        result.setReturnValue(accepted);
    }
}
