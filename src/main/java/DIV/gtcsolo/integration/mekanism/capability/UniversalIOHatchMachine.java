package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.mojang.logging.LogUtils;
import mekanism.common.capabilities.Capabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;

import java.util.List;

/**
 * 液体と気体の排他ユニバーサルハッチ.
 *
 * 1 fluid tank + 1 gas tank. どちらか片方のみ中身を保持でき、もう一方が空の時だけ
 * 反対種別を挿入できる. 空になれば再度どちらでも受け入れる.
 *
 * capacity: GT 標準 fluid hatch と同じ 8000 × 2^tier (両 tank 同容量).
 * PartAbility: INPUT_FLUIDS + INPUT_GAS (入力用) または EXPORT_FLUIDS + OUTPUT_GAS (出力用).
 */
public class UniversalIOHatchMachine extends TieredIOPartMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final long capacity;
    private final NotifiableFluidTank fluidTank;
    private final NotifiableChemicalTank gasTank;
    private final LazyOptional<Object> gasCapOpt;

    public UniversalIOHatchMachine(IMachineBlockEntity holder, int tier, IO io) {
        super(holder, tier, io);
        this.capacity = 8000L * (1L << Math.min(9, tier));

        // Fluid tank: 気体タンクが空の時のみ挿入を許可する validator 付き
        CustomFluidTank innerFluid = new CustomFluidTank((int) capacity,
                stack -> isGasEmpty());
        this.fluidTank = new NotifiableFluidTank(this, List.of(innerFluid), io);

        // Gas tank: 液体タンクが空の時のみ挿入を許可する canInsert 付き
        this.gasTank = new NotifiableChemicalTank(this, ChemicalIngredient.Type.GAS,
                capacity, io, this::isFluidEmpty);
        this.gasCapOpt = LazyOptional.of(gasTank::getMekTank);

        LOGGER.debug("[ChemCap] Universal hatch constructed io={} tier={} cap={}",
                io, tier, capacity);
    }

    public NotifiableFluidTank getFluidTank() { return fluidTank; }
    public NotifiableChemicalTank getGasTank() { return gasTank; }

    private boolean isFluidEmpty() {
        return fluidTank == null || fluidTank.getStorages()[0].getFluid().isEmpty();
    }

    private boolean isGasEmpty() {
        return gasTank == null || gasTank.getMekTank().isEmpty();
    }

    /**
     * Mek 側の chemical cap query に応える. GAS のみ対応.
     * ChemicalCapabilityAttacher から呼ばれる.
     */
    public <T> LazyOptional<T> getMekCapability(Capability<T> cap) {
        if (cap == Capabilities.GAS_HANDLER) {
            return gasCapOpt.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        gasCapOpt.invalidate();
    }
}