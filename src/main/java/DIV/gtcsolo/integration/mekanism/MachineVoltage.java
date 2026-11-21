package DIV.gtcsolo.integration.mekanism;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import mekanism.api.math.FloatingLong;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;

public interface MachineVoltage {
    long gtcsolo$getLastInputVoltage();
    long gtcsolo$remainingAmps();
    void gtcsolo$acceptAmps(long voltage, long amps);

    static int tier(TileEntityMekanism tile) {
        return tile instanceof TileEntityFactory<?> factory
                ? Math.min(GTValues.LV + factory.tier.ordinal(), GTValues.V.length - 1) : GTValues.ULV;
    }

    static long rating(TileEntityMekanism tile) {
        return GTValues.V[tier(tile)];
    }

    static int inputAmps(TileEntityMekanism tile) {
        return 2 * (tile instanceof TileEntityFactory<?> factory ? SlotUpgrade.processes(factory) : 1);
    }

    static FloatingLong joules(long voltage, long amps) {
        return EnergyUnit.FORGE_ENERGY.convertFrom(FloatingLong.createConst(voltage)
                .multiply(amps).multiply(FeCompat.ratio(false)));
    }
}
