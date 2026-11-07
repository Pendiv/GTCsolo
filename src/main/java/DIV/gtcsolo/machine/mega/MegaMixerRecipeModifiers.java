package DIV.gtcsolo.machine.mega;

import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.GTUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Batch size precedes inventory-limited hatch and sub-tick parallel calculation. */
public final class MegaMixerRecipeModifiers {
    public static final int MIXER_BATCH = 1024;
    private MegaMixerRecipeModifiers() {}

    public static ModifierFunction batch(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine electric) || electric.getTier() < GTValues.UV)
            return ModifierFunction.NULL;
        if (recipe.recipeType == ModRecipeTypes.MULTI_MIXING) return ModifierFunction.IDENTITY;
        if (recipe.recipeType != GTRecipeTypes.MIXER_RECIPES || safeMultiplier(recipe) < MIXER_BATCH)
            return ModifierFunction.NULL;
        return MegaMixerRecipeModifiers::scaleMixerBatch;
    }

    /** No EU/t, duration or parallel-counter change: this is a throughput bonus, not electrical OC. */
    public static GTRecipe scaleMixerBatch(GTRecipe source) {
        if (source.recipeType != GTRecipeTypes.MIXER_RECIPES) return source;
        return scaleBatch(source, MIXER_BATCH);
    }

    public static GTRecipe scaleBatch(GTRecipe source, int batch) {
        if (batch < 1 || safeMultiplier(source) < batch) return null;
        GTRecipe result = ModifierFunction.builder().build().apply(source);
        scaleContents(result.inputs, batch);
        scaleContents(result.outputs, batch);
        scaleContents(result.tickInputs, batch);
        scaleContents(result.tickOutputs, batch);
        return result;
    }

    private static void scaleContents(Map<RecipeCapability<?>, List<Content>> contents, int batch) {
        for (RecipeCapability<?> cap : List.of(ItemRecipeCapability.CAP, FluidRecipeCapability.CAP)) {
            List<Content> old = contents.get(cap);
            if (old == null) continue;
            List<Content> replacement = new ArrayList<>(old.size());
            for (Content content : old) {
                // Circuits, molds and other non-consumable catalysts remain one copy.
                // For probabilistic contents, scale the amount per successful roll;
                // hatch/subtick parallels still supply the usual GT number of rolls.
                replacement.add(content.chance == 0 ? content.copy(cap) : new Content(
                        cap.copyContent(content.content, ContentModifier.multiplier(batch)),
                        content.chance, content.maxChance, content.tierChanceBoost,
                        content.slotName, content.uiName));
            }
            contents.put(cap, replacement);
        }
    }

    public static ModifierFunction parallel(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine electric) || !electric.isFormed())
            return ModifierFunction.NULL;
        int requested = electric.getParallelHatch().map(h -> h.getCurrentParallel()).orElse(1);
        int limit = Math.min(requested, safeMultiplier(recipe));
        int count = availableParallels(electric, recipe, limit);
        if (count <= 0) return ModifierFunction.NULL;
        return ModifierFunction.builder().modifyAllContents(ContentModifier.multiplier(count))
                .eutMultiplier(count).parallels(count).build();
    }

    public static ModifierFunction subTick(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine electric)) return ModifierFunction.NULL;
        long eut = Math.abs(RecipeHelper.getRealEUt(recipe));
        long voltage = electric.getOverclockVoltage();
        if (eut > voltage) return ModifierFunction.NULL;
        if (eut == 0) return ModifierFunction.IDENTITY;
        int recipeTier = GTUtil.getTierByVoltage(eut);
        int overclocks = GTUtil.getOCTierByVoltage(voltage) - recipeTier;
        if (recipeTier == GTValues.ULV) overclocks--;
        if (overclocks <= 0) return ModifierFunction.IDENTITY;
        int limit = safeMultiplier(recipe);
        int available = availableParallels(electric, recipe, limit);
        if (available <= 0) return ModifierFunction.NULL;
        return OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK.runOverclockingLogic(
                new OverclockingLogic.OCParams(eut, recipe.duration, overclocks, available), voltage).toModifier();
    }

    public static int availableParallels(WorkableElectricMultiblockMachine machine, GTRecipe recipe, int limit) {
        if (limit <= 0) return 0;
        // GT's getParallelAmount returns 1 immediately for limit=1. This machine
        // still needs to test the full 1024-sized batch in that case.
        int inputs = ParallelLogic.limitByInput(machine, recipe, limit);
        if (inputs <= 0) return 0;
        return ParallelLogic.limitByOutputMerging(machine, recipe, Math.min(limit, inputs), machine::canVoidRecipeOutputs);
    }

    /** Reject impossible batches rather than silently saturating one side of a recipe. */
    public static int safeMultiplier(GTRecipe recipe) {
        int limit = Integer.MAX_VALUE / Math.max(1, recipe.parallels);
        for (var map : List.of(recipe.inputs, recipe.outputs, recipe.tickInputs, recipe.tickOutputs)) {
            for (RecipeCapability<?> cap : List.of(ItemRecipeCapability.CAP, FluidRecipeCapability.CAP)) {
                for (Content content : map.getOrDefault(cap, List.of())) {
                    if (content.chance == 0) continue;
                    int amount;
                    if (cap == FluidRecipeCapability.CAP) {
                        amount = FluidRecipeCapability.CAP.of(content.content).getAmount();
                    } else {
                        var ingredient = ItemRecipeCapability.CAP.of(content.content);
                        amount = ingredient instanceof SizedIngredient sized ? sized.getAmount()
                                : ingredient instanceof IntProviderIngredient ranged ? ranged.getCountProvider().getMaxValue() : 1;
                    }
                    if (amount < 0) return 0;
                    long maximumTotal = (long) amount * (content.chance < content.maxChance ? Math.max(1, recipe.parallels) : 1);
                    if (maximumTotal > 0) limit = (int) Math.min(limit, Integer.MAX_VALUE / maximumTotal);
                }
            }
        }
        return limit;
    }
}
