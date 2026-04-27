package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.mojang.logging.LogUtils;
import mekanism.common.capabilities.Capabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mekanism chemical IO hatch 機械. ChemicalHatchVariant で変種を指定:
 *  - GAS 単体、INFUSION 単体、OTHER (PIGMENT+SLURRY 同居)
 *
 * OTHER variant は 2 tank を内包し、PIGMENT_HANDLER / SLURRY_HANDLER 両方を露出する.
 * tier により容量スケール: baseCapacity * 2^min(tier, 9)
 */
public class ChemicalIOHatchMachine extends TieredIOPartMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ChemicalHatchVariant variant;
    private final Map<ChemicalIngredient.Type, NotifiableChemicalTank> tanks = new EnumMap<>(ChemicalIngredient.Type.class);
    private final Map<ChemicalIngredient.Type, LazyOptional<Object>> capOpts = new EnumMap<>(ChemicalIngredient.Type.class);

    public ChemicalIOHatchMachine(IMachineBlockEntity holder, int tier, IO io,
                                    ChemicalHatchVariant variant) {
        this(holder, tier, io, variant, variant.baseCapacity * (1L << Math.min(9, tier)));
    }

    public ChemicalIOHatchMachine(IMachineBlockEntity holder, int tier, IO io,
                                    ChemicalHatchVariant variant, long capacity) {
        super(holder, tier, io);
        this.variant = variant;
        for (ChemicalIngredient.Type t : variant.types) {
            NotifiableChemicalTank tank = new NotifiableChemicalTank(this, t, capacity, io);
            tanks.put(t, tank);
            capOpts.put(t, LazyOptional.of(tank::getMekTank));
        }
        LOGGER.debug("[ChemCap] Hatch constructed variant={} io={} tier={} capacity={} tanks={}",
                variant, io, tier, capacity, variant.types);
    }

    public ChemicalHatchVariant getVariant() { return variant; }
    public List<NotifiableChemicalTank> getTanks() { return new ArrayList<>(tanks.values()); }
    public NotifiableChemicalTank getTank(ChemicalIngredient.Type type) { return tanks.get(type); }

    /**
     * Mek 側からの capability query に応える.
     * 保持 tank の中に一致する cap があれば返す (OTHER は pigment/slurry 両対応).
     */
    public <T> LazyOptional<T> getMekCapability(Capability<T> cap) {
        for (Map.Entry<ChemicalIngredient.Type, LazyOptional<Object>> e : capOpts.entrySet()) {
            if (cap == capabilityFor(e.getKey())) {
                LOGGER.debug("[ChemCap] Hatch[{}] cap query HIT: {}", variant, cap.getName());
                return e.getValue().cast();
            }
        }
        LOGGER.debug("[ChemCap] Hatch[{}] cap query MISS: got={}", variant, cap.getName());
        return LazyOptional.empty();
    }

    private static Capability<?> capabilityFor(ChemicalIngredient.Type t) {
        switch (t) {
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
        for (LazyOptional<Object> opt : capOpts.values()) opt.invalidate();
    }
}