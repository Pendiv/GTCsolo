package DIV.gtcsolo.machine.hpca;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.machine.multiblock.part.hpca.HPCAComponentPartMachine;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

import java.util.Objects;

/** A matching component for unused grid positions, without an operating cost. */
public final class TieredHpcaEmptyMachine extends HPCAComponentPartMachine {
    private final HpcaTierSpec tierSpec;
    private ResourceTexture icon;

    public TieredHpcaEmptyMachine(IMachineBlockEntity holder, HpcaTierSpec tierSpec) {
        super(holder);
        this.tierSpec = Objects.requireNonNull(tierSpec);
    }

    public HpcaTierSpec getTierSpec() { return tierSpec; }

    @Override
    public boolean isAdvanced() { return true; }

    @Override
    public ResourceTexture getComponentIcon() {
        if (icon == null) icon = new ResourceTexture(tierSpec.iconPath("empty_component"));
        return icon;
    }

    @Override
    public int getUpkeepEUt() { return 0; }

    @Override
    public int getMaxEUt() { return 0; }

    @Override
    public boolean canBeDamaged() { return false; }
}
