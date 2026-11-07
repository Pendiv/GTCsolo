package DIV.gtcsolo.machine.hpca;

import com.gregtechceu.gtceu.api.capability.IHPCAComputationProvider;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.machine.multiblock.part.hpca.HPCAComponentPartMachine;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

import java.util.Objects;

/** Uses the native component's persisted damage state and damaged-item drops. */
public final class TieredHpcaComputationMachine extends HPCAComponentPartMachine
        implements IHPCAComputationProvider {
    private final HpcaTierSpec tierSpec;
    private ResourceTexture icon;
    private ResourceTexture damagedIcon;

    public TieredHpcaComputationMachine(IMachineBlockEntity holder, HpcaTierSpec tierSpec) {
        super(holder);
        this.tierSpec = Objects.requireNonNull(tierSpec);
    }

    public HpcaTierSpec getTierSpec() { return tierSpec; }

    @Override
    public boolean isAdvanced() { return true; }

    @Override
    public ResourceTexture getComponentIcon() {
        if (isDamaged()) {
            if (damagedIcon == null) damagedIcon = new ResourceTexture(tierSpec.iconPath("damaged_computation_component"));
            return damagedIcon;
        }
        if (icon == null) icon = new ResourceTexture(tierSpec.iconPath("computation_component"));
        return icon;
    }

    @Override
    public int getUpkeepEUt() { return tierSpec.computationUpkeepEUt(); }

    @Override
    public int getMaxEUt() { return tierSpec.computationMaxEUt(); }

    @Override
    public int getCWUPerTick() { return isDamaged() ? 0 : tierSpec.computationPerTick(); }

    @Override
    public int getCoolingPerTick() { return tierSpec.computationCoolingDemand(); }

    @Override
    public boolean canBeDamaged() { return true; }
}
