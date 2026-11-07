package DIV.gtcsolo.integration.mekanism;

import DIV.gtcsolo.Gtcsolo;
import mekanism.api.chemical.gas.Gas;
import mekanism.common.registration.impl.GasDeferredRegister;
import mekanism.common.registration.impl.GasRegistryObject;

/** GTCsolo が追加する Mekanism Gas。 */
public final class ModGases {

    public static final GasDeferredRegister GASES = new GasDeferredRegister(Gtcsolo.MODID);

    /**
     * オスミウムを精製黒曜石へ直接吹き込まず、酸化・揮発性中間体として扱うための OsO4 蒸気。
     */
    public static final GasRegistryObject<Gas> OSMIUM_TETROXIDE =
            GASES.register("osmium_tetroxide", 0xE8E2B8);

    /** APT recipes require an actual Mekanism gas, not the legacy KubeJS fluid. */
    public static final GasRegistryObject<Gas> NEUTRONIUM =
            GASES.register("neutronium", 0x00FF7F);

    /** Oxidizer/working gas for boosted combustion; deliberately has no Fuel attribute. */
    public static final GasRegistryObject<Gas> OXYGEN_STEAM_MIXTURE =
            GASES.register("oxygen_steam_mixture", 0xD9EAF1);

    private ModGases() {}
}
