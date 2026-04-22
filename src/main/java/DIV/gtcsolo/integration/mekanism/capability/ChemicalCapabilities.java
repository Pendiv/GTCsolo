package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 4 種類 (GAS/INFUSION/PIGMENT/SLURRY) の ChemicalRecipeCapability インスタンスを保持し、
 * GTCEu レジストリへの登録を一元化する.
 *
 * 呼び出し元: IGTAddon.registerRecipeCapabilities()
 */
public final class ChemicalCapabilities {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ChemicalRecipeCapability GAS =
            new ChemicalRecipeCapability(ChemicalIngredient.Type.GAS,      "chemical_gas",      0xFFAFFF00);
    public static final ChemicalRecipeCapability INFUSION =
            new ChemicalRecipeCapability(ChemicalIngredient.Type.INFUSION, "chemical_infusion", 0xFFB30505);
    public static final ChemicalRecipeCapability PIGMENT =
            new ChemicalRecipeCapability(ChemicalIngredient.Type.PIGMENT,  "chemical_pigment",  0xFFD8A9D8);
    public static final ChemicalRecipeCapability SLURRY =
            new ChemicalRecipeCapability(ChemicalIngredient.Type.SLURRY,   "chemical_slurry",   0xFFA06835);

    private ChemicalCapabilities() {}

    /**
     * GTRegistries.RECIPE_CAPABILITIES への登録.
     * MapIngredient への変換は各 capability の convertToMapIngredient() で対応済み.
     */
    public static void init() {
        LOGGER.info("[ChemCap] === Registering ChemicalRecipeCapabilities ===");
        GTRegistries.RECIPE_CAPABILITIES.register(GAS.name, GAS);
        LOGGER.info("[ChemCap]   registered {} (color=0x{})", GAS.name, Integer.toHexString(GAS.color));
        GTRegistries.RECIPE_CAPABILITIES.register(INFUSION.name, INFUSION);
        LOGGER.info("[ChemCap]   registered {} (color=0x{})", INFUSION.name, Integer.toHexString(INFUSION.color));
        GTRegistries.RECIPE_CAPABILITIES.register(PIGMENT.name, PIGMENT);
        LOGGER.info("[ChemCap]   registered {} (color=0x{})", PIGMENT.name, Integer.toHexString(PIGMENT.color));
        GTRegistries.RECIPE_CAPABILITIES.register(SLURRY.name, SLURRY);
        LOGGER.info("[ChemCap]   registered {} (color=0x{})", SLURRY.name, Integer.toHexString(SLURRY.color));
        LOGGER.info("[ChemCap] === Capabilities registered (4 total) ===");
    }
}