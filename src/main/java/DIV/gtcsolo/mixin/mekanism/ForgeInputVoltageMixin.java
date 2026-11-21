package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.InputVoltageReceiver;
import DIV.gtcsolo.integration.mekanism.MachineVoltage;
import DIV.gtcsolo.integration.mekanism.PacketInputAccess;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import mekanism.common.integration.energy.forgeenergy.ForgeEnergyIntegration;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeEnergyIntegration.class, remap = false)
public abstract class ForgeInputVoltageMixin implements InputVoltageReceiver, PacketInputAccess {
    @Shadow @Final private IStrictEnergyHandler handler;

    @Override
    public long gtcsolo$insertPackets(long voltage, long offered) {
        FloatingLong packet = MachineVoltage.joules(voltage, 1);
        if (packet.isZero()) return 0;
        FloatingLong amount = packet.multiply(offered);
        FloatingLong room = amount.subtract(handler.insertEnergy(amount, Action.SIMULATE));
        long amps = room.divideToInt(packet);
        if (amps > 0) handler.insertEnergy(packet.multiply(amps), Action.EXECUTE);
        return amps;
    }

    @Inject(method = "receiveEnergy", at = @At("RETURN"))
    private void gtcsolo$onForgeInput(int offered, boolean simulate, CallbackInfoReturnable<Integer> result) {
        if (!simulate && result.getReturnValueI() > 0 && gtcsolo$machine() instanceof MachineVoltage machine) {
            machine.gtcsolo$acceptAmps(0, 0);
        }
    }

    @Override
    public TileEntityMekanism gtcsolo$machine() {
        return handler instanceof InputVoltageReceiver receiver ? receiver.gtcsolo$machine() : null;
    }
}
