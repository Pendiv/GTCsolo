package DIV.gtcsolo.machine.hpca;

import com.gregtechceu.gtceu.api.capability.IHPCACoolantProvider;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.machine.multiblock.part.hpca.HPCAComponentPartMachine;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

import java.util.Objects;

/** The native HPCA controls coolant consumption and partial-load cooling. */
public final class TieredHpcaActiveCoolerMachine extends HPCAComponentPartMachine
        implements IHPCACoolantProvider {
    private final HpcaTierSpec tierSpec;
    private ResourceTexture icon;

    public TieredHpcaActiveCoolerMachine(IMachineBlockEntity holder, HpcaTierSpec tierSpec) {
        super(holder);
        this.tierSpec = Objects.requireNonNull(tierSpec);
    }

    public HpcaTierSpec getTierSpec() { return tierSpec; }

    @Override
    public boolean isAdvanced() { return true; }

    @Override
    public ResourceTexture getComponentIcon() {
        if (icon == null) icon = new ResourceTexture(tierSpec.iconPath("active_cooler_component"));
        return icon;
    }

    @Override
    public int getUpkeepEUt() { return tierSpec.coolerEUt(); }

    @Override
    public int getMaxEUt() { return tierSpec.coolerEUt(); }

    @Override
    public int getCoolingAmount() { return tierSpec.coolingPerTick(); }

    @Override
    public boolean isActiveCooler() { return true; }

    @Override
    public int getMaxCoolantPerTick() { return tierSpec.coolantPerTick(); }

    @Override
    public boolean canBeDamaged() { return false; }
}
