package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.addon.events.KJSRecipeKeyEvent;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.integration.kjs.recipe.components.ContentJS;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeComponentFactoryRegistryEvent;
import org.slf4j.Logger;

/**
 * ChemicalRecipeCapability 4種 × IN/OUT = 8 つの ContentJS を保持し、
 * KubeJS の RecipeComponent ファクトリと IGTAddon.registerRecipeKeys に登録する.
 */
public final class ChemicalRecipeComponents {

    private static final Logger LOGGER = LogUtils.getLogger();

    // IN (isOutput=false)
    public static final ContentJS<ChemicalIngredient> GAS_IN = new ContentJS<>(
            ChemicalIngredientComponent.GAS, ChemicalCapabilities.GAS, false);
    public static final ContentJS<ChemicalIngredient> INFUSION_IN = new ContentJS<>(
            ChemicalIngredientComponent.INFUSION, ChemicalCapabilities.INFUSION, false);
    public static final ContentJS<ChemicalIngredient> PIGMENT_IN = new ContentJS<>(
            ChemicalIngredientComponent.PIGMENT, ChemicalCapabilities.PIGMENT, false);
    public static final ContentJS<ChemicalIngredient> SLURRY_IN = new ContentJS<>(
            ChemicalIngredientComponent.SLURRY, ChemicalCapabilities.SLURRY, false);

    // OUT (isOutput=true)
    public static final ContentJS<ChemicalIngredient> GAS_OUT = new ContentJS<>(
            ChemicalIngredientComponent.GAS, ChemicalCapabilities.GAS, true);
    public static final ContentJS<ChemicalIngredient> INFUSION_OUT = new ContentJS<>(
            ChemicalIngredientComponent.INFUSION, ChemicalCapabilities.INFUSION, true);
    public static final ContentJS<ChemicalIngredient> PIGMENT_OUT = new ContentJS<>(
            ChemicalIngredientComponent.PIGMENT, ChemicalCapabilities.PIGMENT, true);
    public static final ContentJS<ChemicalIngredient> SLURRY_OUT = new ContentJS<>(
            ChemicalIngredientComponent.SLURRY, ChemicalCapabilities.SLURRY, true);

    private ChemicalRecipeComponents() {}

    /** IGTAddon.registerRecipeKeys() から呼ぶ. 各 capability に対応する IN/OUT ContentJS を登録. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRecipeKeys(KJSRecipeKeyEvent event) {
        LOGGER.info("[ChemCap] === Registering KJS recipe keys (4 capabilities) ===");
        event.registerKey(ChemicalCapabilities.GAS,      (Pair) Pair.of(GAS_IN, GAS_OUT));
        LOGGER.info("[ChemCap]   key: GAS <-> (GAS_IN, GAS_OUT)");
        event.registerKey(ChemicalCapabilities.INFUSION, (Pair) Pair.of(INFUSION_IN, INFUSION_OUT));
        LOGGER.info("[ChemCap]   key: INFUSION <-> (INFUSION_IN, INFUSION_OUT)");
        event.registerKey(ChemicalCapabilities.PIGMENT,  (Pair) Pair.of(PIGMENT_IN, PIGMENT_OUT));
        LOGGER.info("[ChemCap]   key: PIGMENT <-> (PIGMENT_IN, PIGMENT_OUT)");
        event.registerKey(ChemicalCapabilities.SLURRY,   (Pair) Pair.of(SLURRY_IN, SLURRY_OUT));
        LOGGER.info("[ChemCap]   key: SLURRY <-> (SLURRY_IN, SLURRY_OUT)");
    }

    /** KubeJSPlugin.registerRecipeComponents() から呼ぶ. */
    public static void registerRecipeComponents(RecipeComponentFactoryRegistryEvent event) {
        LOGGER.info("[ChemCap] === Registering KJS recipe components ===");
        event.register("gtcsoloChemicalGasIn",      GAS_IN);
        event.register("gtcsoloChemicalGasOut",     GAS_OUT);
        event.register("gtcsoloChemicalInfusionIn",  INFUSION_IN);
        event.register("gtcsoloChemicalInfusionOut", INFUSION_OUT);
        event.register("gtcsoloChemicalPigmentIn",   PIGMENT_IN);
        event.register("gtcsoloChemicalPigmentOut",  PIGMENT_OUT);
        event.register("gtcsoloChemicalSlurryIn",    SLURRY_IN);
        event.register("gtcsoloChemicalSlurryOut",   SLURRY_OUT);

        // bare component registration (plugins が参照できるように)
        event.register("gtcsoloChemicalGas",      ChemicalIngredientComponent.GAS);
        event.register("gtcsoloChemicalInfusion", ChemicalIngredientComponent.INFUSION);
        event.register("gtcsoloChemicalPigment",  ChemicalIngredientComponent.PIGMENT);
        event.register("gtcsoloChemicalSlurry",   ChemicalIngredientComponent.SLURRY);
        LOGGER.info("[ChemCap]   components: 8 ContentJS + 4 base components registered");
    }
}