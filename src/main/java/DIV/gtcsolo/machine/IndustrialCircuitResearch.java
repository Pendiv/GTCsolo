package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.gregtechceu.gtceu.utils.ResearchManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

/** Resolve computation cost from the registered research recipe, never from untrusted media cost fields. */
public final class IndustrialCircuitResearch {
    public static final long DEFAULT_EU_PER_CWU = 1_000;
    private IndustrialCircuitResearch() {}

    public record Specification(String key, long totalEU, long euPerTick, Set<String> recipes) {}

    public static String recipeKey(GTRecipe recipe) {
        return recipe.recipeType.registryName + "#" + recipe.id;
    }

    /** Analysis is an interim research route through UHV; higher-voltage recipes need permanent research. */
    public static boolean canAnalyse(GTRecipe recipe) {
        long eut = com.gregtechceu.gtceu.api.recipe.RecipeHelper.getInputEUt(recipe);
        return eut >= 0 && eut <= com.gregtechceu.gtceu.api.GTValues.V[com.gregtechceu.gtceu.api.GTValues.UHV];
    }

    public static Set<String> requirements(GTRecipe recipe) {
        Set<String> result = new LinkedHashSet<>();
        for (var condition : recipe.conditions) if (condition instanceof ResearchCondition research) {
            // Reverse research conditions do not express a conventional unlocking requirement.
            if (research.isReverse()) return Set.of();
            for (var entry : research.data) result.add(recipe.recipeType.registryName + "#" + entry.getResearchId());
        }
        return result;
    }

    public static Specification resolve(Level level, ItemStack media, GTRecipeType targetType, long factor) {
        if (factor < 1 || !ResearchManager.isStackDataItem(media, true)) return null;
        var identity = ResearchManager.readResearchId(media);
        if (identity == null || identity.getFirst() != GTRecipeTypes.ASSEMBLY_LINE_RECIPES) return null;
        Collection<GTRecipe> linked = identity.getFirst().getDataStickEntry(identity.getSecond());
        if (linked == null || linked.isEmpty()) return null;
        Set<String> recipes = new LinkedHashSet<>();
        Set<net.minecraft.world.item.Item> outputs = new HashSet<>();
        for (GTRecipe recipe : linked) for (var content : recipe.getOutputContents(ItemRecipeCapability.CAP))
            for (ItemStack item : ItemRecipeCapability.CAP.of(content.content).getItems()) if (!item.isEmpty()) outputs.add(item.getItem());
        for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(targetType)) {
            if (!canAnalyse(recipe)) continue;
            if (recipe.getOutputContents(ItemRecipeCapability.CAP).stream().anyMatch(content ->
                    Arrays.stream(ItemRecipeCapability.CAP.of(content.content).getItems()).anyMatch(stack -> outputs.contains(stack.getItem()))))
                recipes.add(recipeKey(recipe));
        }
        if (recipes.isEmpty()) return null;
        String key = identity.getFirst().registryName + "#" + identity.getSecond();
        long total = 0, rate = 0;
        List<GTRecipe> researchRecipes = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(GTRecipeTypes.RESEARCH_STATION_RECIPES));
        researchRecipes.addAll(level.getRecipeManager().getAllRecipesFor(GTRecipeTypes.SCANNER_RECIPES));
        for (GTRecipe research : researchRecipes) {
            boolean matches = false;
            for (var output : research.getOutputContents(ItemRecipeCapability.CAP)) {
                for (ItemStack stack : ItemRecipeCapability.CAP.of(output.content).getItems()) {
                    var outputIdentity = ResearchManager.readResearchId(stack);
                    if (outputIdentity != null && outputIdentity.getFirst() == identity.getFirst()
                            && identity.getSecond().equals(outputIdentity.getSecond())
                            && ItemStack.isSameItem(media, stack)) matches = true;
                }
            }
            if (!matches) continue;
            try {
                long candidateTotal, candidateRate;
                if (research.duration <= 0) continue;
                if (research.data.getBoolean("duration_is_total_cwu")) {
                    long cwut = 0;
                    for (var input : research.getTickInputContents(CWURecipeCapability.CAP)) {
                        cwut = Math.addExact(cwut, CWURecipeCapability.CAP.of(input.content));
                    }
                    if (cwut <= 0) continue;
                    candidateTotal = Math.multiplyExact((long) research.duration, factor);
                    candidateRate = Math.multiplyExact(cwut, factor);
                } else {
                    if (research.recipeType != GTRecipeTypes.SCANNER_RECIPES) continue;
                    long eu = com.gregtechceu.gtceu.api.recipe.RecipeHelper.getInputEUt(research);
                    if (eu <= 0) continue;
                    candidateTotal = Math.multiplyExact((long) research.duration, eu);
                    candidateRate = eu;
                }
                // Several recipes for the same medium must not provide a cheaper accidental bypass.
                total = Math.max(total, candidateTotal);
                rate = Math.max(rate, candidateRate);
            } catch (ArithmeticException overflow) { return null; }
        }
        return total == 0 ? null : new Specification(key, total, rate, Set.copyOf(recipes));
    }
}
