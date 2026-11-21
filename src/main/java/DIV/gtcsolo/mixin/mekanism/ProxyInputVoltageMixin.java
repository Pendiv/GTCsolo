package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.InputVoltageReceiver;
import mekanism.api.energy.ISidedStrictEnergyHandler;
import mekanism.common.capabilities.proxy.ProxyStrictEnergyHandler;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ProxyStrictEnergyHandler.class, remap = false)
public abstract class ProxyInputVoltageMixin implements InputVoltageReceiver {
    @Shadow @Final private ISidedStrictEnergyHandler energyHandler;

    @Override
    public TileEntityMekanism gtcsolo$machine() {
        return energyHandler instanceof InputVoltageReceiver receiver ? receiver.gtcsolo$machine() : null;
    }
}
