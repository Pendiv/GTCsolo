package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.InputAmperageBudget;
import DIV.gtcsolo.integration.mekanism.InputVoltageReceiver;
import DIV.gtcsolo.integration.mekanism.MachineVoltage;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class MachineVoltageMixin implements MachineVoltage, InputVoltageReceiver {
    @Unique private long gtcsolo$lastInputVoltage;
    @Unique private final InputAmperageBudget gtcsolo$inputBudget = new InputAmperageBudget();

    @Override
    public long gtcsolo$getLastInputVoltage() {
        return gtcsolo$lastInputVoltage;
    }

    @Override
    public TileEntityMekanism gtcsolo$machine() {
        return (TileEntityMekanism) (Object) this;
    }

    @Override
    public long gtcsolo$remainingAmps() {
        TileEntityMekanism tile = gtcsolo$machine();
        if (tile.getLevel() == null) return 0;
        return gtcsolo$inputBudget.remaining(tile.getLevel().getGameTime(), MachineVoltage.inputAmps(tile));
    }

    @Override
    public void gtcsolo$acceptAmps(long voltage, long amps) {
        gtcsolo$inputBudget.record(amps);
        gtcsolo$lastInputVoltage = voltage;
    }
}
