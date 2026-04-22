package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.mojang.logging.LogUtils;
import mekanism.common.capabilities.Capabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;

/**
 * Mekanism chemical IO hatch 機械. 4種(GAS/INFUSION/PIGMENT/SLURRY) を type field で扱う統一クラス.
 *
 * tier により容量スケール: baseCapacity * 2^min(tier, 9)
 * - ULV(0)=×1, LV(1)=×2, ... UEV(9)=×512, それ以上は×512で打ち止め
 */
public class ChemicalIOHatchMachine extends TieredIOPartMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ChemicalIngredient.Type chemType;
    private final NotifiableChemicalTank tank;
    private final LazyOptional<Object> mekCapabilityOpt;

    public ChemicalIOHatchMachine(IMachineBlockEntity holder, int tier, IO io,
                                    ChemicalIngredient.Type chemType, long baseCapacity) {
        super(holder, tier, io);
        this.chemType = chemType;
        long capacity = baseCapacity * (1L << Math.min(9, tier));
        this.tank = new NotifiableChemicalTank(this, chemType, capacity, io);
        this.mekCapabilityOpt = LazyOptional.of(tank::getMekTank);
        LOGGER.debug("[ChemCap] Hatch constructed type={} io={} tier={} capacity={}",
                chemType, io, tier, capacity);
    }

    public ChemicalIngredient.Type getChemType() { return chemType; }
    public NotifiableChemicalTank getTank() { return tank; }

    /**
     * Mek 側からの capability query に応える.
     * ChemicalCapabilityAttacher から呼ばれる.
     */
    public <T> LazyOptional<T> getMekCapability(Capability<T> cap) {
        Capability<?> expected = expectedCapability();
        if (expected != null && cap == expected) {
            LOGGER.debug("[ChemCap] Hatch[{}] cap query HIT: {}", chemType, cap.getName());
            return mekCapabilityOpt.cast();
        }
        LOGGER.debug("[ChemCap] Hatch[{}] cap query MISS: got={} expected={}",
                chemType, cap.getName(), expected == null ? "null" : expected.getName());
        return LazyOptional.empty();
    }

    private Capability<?> expectedCapability() {
        switch (chemType) {
            case GAS:      return Capabilities.GAS_HANDLER;
            case INFUSION: return Capabilities.INFUSION_HANDLER;
            case PIGMENT:  return Capabilities.PIGMENT_HANDLER;
            case SLURRY:   return Capabilities.SLURRY_HANDLER;
        }
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        mekCapabilityOpt.invalidate();
    }
}