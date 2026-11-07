package DIV.gtcsolo.integration.mekanism.generator;

import mekanism.api.IContentsListener;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.jetbrains.annotations.Nullable;

/** Native upgrade lifecycle and storage clamping, with output-only energy and no operating energy cost. */
public final class GasGeneratorEnergyContainer extends MachineEnergyContainer<TileEntityMekanism> {
    public GasGeneratorEnergyContainer(TileEntityMekanism tile, FloatingLong baseCapacity,
                                       @Nullable IContentsListener listener) {
        super(baseCapacity, FloatingLong.ZERO, alwaysTrue, internalOnly, tile, listener);
        updateMaxEnergy();
    }

    @Override public void updateMaxEnergy() {
        setMaxEnergy(GasGeneratorUpgrades.capacity(tile, getBaseMaxEnergy()).copyAsConst());
    }
}
