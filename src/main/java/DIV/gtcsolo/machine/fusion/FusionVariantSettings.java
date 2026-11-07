package DIV.gtcsolo.machine.fusion;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;

/** Performance choices live here; the exported pattern resources only describe geometry. */
public enum FusionVariantSettings {
    ADVANCED_MK1("advanced", 1), ADVANCED_MK2("advanced", 2),
    ADVANCED_MK3("advanced", 3), ADVANCED_MK4("advanced", 4),
    COMPACT_MK1("compact", 1), COMPACT_MK2("compact", 2),
    COMPACT_MK3("compact", 3), COMPACT_MK4("compact", 4),
    GIANT_MK4("giant", 4), MEGA_MK4("mega", 4);

    public final String family;
    public final int mk;
    private final FusionVariantPattern pattern;

    FusionVariantSettings(String family, int mk) {
        this.family = family;
        this.mk = mk;
        this.pattern = new FusionVariantPattern(id());
    }

    public String id() { return family.equals("mega") ? "mega_fusion" : family + "_fusion_reactor_mk" + mk; }
    public boolean isMega() { return this == MEGA_MK4; }
    public int tier() { return GTValues.LuV + mk - 1; }
    public int minimumEnergyHatches() { return mk == 4 ? FusionMk4Machine.ENERGY_PORTS : 1; }
    public int maximumEnergyHatches() { return isMega() ? 128 : mk == 4 ? FusionMk4Machine.ENERGY_PORTS : 16; }
    public int maximumHatchTier() { return mk == 4 ? GTValues.UHV : GTValues.UV; }
    public boolean fusionOverclocking() { return mk != 4; }
    /** Receives GT's hatch-count-dependent capacity; use this hook to change actual storage. */
    public long startupCapacity(long nativeCapacity) {
        if (isMega()) return MegaFusionRules.STORAGE;
        return mk == 4 ? FusionMk4Machine.STARTUP_CAPACITY : nativeCapacity;
    }
    public long maximumStartupCapacity() {
        return startupCapacity(FusionReactorMachine.calculateEnergyStorageFactor(tier(), maximumEnergyHatches()));
    }
    public FusionVariantPattern pattern() { return pattern; }
}
