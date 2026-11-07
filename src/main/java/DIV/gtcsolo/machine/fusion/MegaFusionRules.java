package DIV.gtcsolo.machine.fusion;

import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;

/** Recipe amounts and thermal units are deliberately independent. */
public final class MegaFusionRules {
    public static final int BATCH = 32768;
    public static final int PERFECT_OCS = 2;
    // Confirmed after explaining GT's thermal units: 1% efficiency, 100 EU -> 1 heat.
    public static final long EU_PER_HEAT = 100L;
    public static final long MAX_HEAT = FusionMk4Machine.STARTUP_CAPACITY;
    public static final long STORAGE = Math.multiplyExact(MAX_HEAT, EU_PER_HEAT);
    private static final String APPLIED = "gtcsolo_mega_fusion_applied";
    private MegaFusionRules() {}

    public static long heatingCost(long heat, long target) {
        if (heat < 0 || target < 0 || target > MAX_HEAT) return -1;
        return Math.multiplyExact(Math.max(0,target-heat),EU_PER_HEAT);
    }

    public static GTRecipe transform(GTRecipe source) {
        if (!source.data.contains("eu_to_start")) return null;
        long target=source.data.getLong("eu_to_start");
        if (target<0 || target>MAX_HEAT) return null;
        if (source.data.getBoolean(APPLIED)) return source;
        GTRecipe batched=MegaMixerRecipeModifiers.scaleBatch(source,BATCH);
        if (batched==null) return null;
        GTRecipe result=ModifierFunction.builder().durationMultiplier(1.0/16).addOCs(PERFECT_OCS).build().apply(batched);
        // eutMultiplier(0) is NOT zero in GT 1.6.4: it clamps the EU content to at least one.
        result.tickInputs.remove(EURecipeCapability.CAP);
        result.inputs.remove(EURecipeCapability.CAP);
        result.data=source.data.copy();
        result.data.putBoolean(APPLIED,true);
        return result;
    }
}
