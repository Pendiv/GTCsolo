package DIV.gtcsolo.integration.kubejs;

import DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

/**
 * KubeJS startup scripts から chemical capability の max IO size を設定する helper.
 *
 * 使い方 :
 *   const t = event.create('my_recipe').category('...').setMaxIOSize(1,0,0,0).setEUIO('out')
 *   GtcsoloChemCapHelper.setInfusionOut(t, 1)   // INFUSION 最大1出力
 *   GtcsoloChemCapHelper.setGasIn(t, 2)         // GAS 最大2入力
 *
 * バインディング名: GtcsoloChemCapHelper (GtcSoloKubeJSPlugin.registerBindings で登録)
 */
public final class ChemCapKJSHelper {

    private ChemCapKJSHelper() {}

    // --- GAS ---
    public static GTRecipeType setGasIn(GTRecipeType type, int max) {
        return type.setMaxSize(IO.IN, ChemicalCapabilities.GAS, max);
    }
    public static GTRecipeType setGasOut(GTRecipeType type, int max) {
        return type.setMaxSize(IO.OUT, ChemicalCapabilities.GAS, max);
    }

    // --- INFUSION ---
    public static GTRecipeType setInfusionIn(GTRecipeType type, int max) {
        return type.setMaxSize(IO.IN, ChemicalCapabilities.INFUSION, max);
    }
    public static GTRecipeType setInfusionOut(GTRecipeType type, int max) {
        return type.setMaxSize(IO.OUT, ChemicalCapabilities.INFUSION, max);
    }

    // --- PIGMENT ---
    public static GTRecipeType setPigmentIn(GTRecipeType type, int max) {
        return type.setMaxSize(IO.IN, ChemicalCapabilities.PIGMENT, max);
    }
    public static GTRecipeType setPigmentOut(GTRecipeType type, int max) {
        return type.setMaxSize(IO.OUT, ChemicalCapabilities.PIGMENT, max);
    }

    // --- SLURRY ---
    public static GTRecipeType setSlurryIn(GTRecipeType type, int max) {
        return type.setMaxSize(IO.IN, ChemicalCapabilities.SLURRY, max);
    }
    public static GTRecipeType setSlurryOut(GTRecipeType type, int max) {
        return type.setMaxSize(IO.OUT, ChemicalCapabilities.SLURRY, max);
    }
}