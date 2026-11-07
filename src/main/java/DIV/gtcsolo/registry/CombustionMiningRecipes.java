package DIV.gtcsolo.registry;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class CombustionMiningRecipes {
    public static GTRecipeType TYPE;

    private CombustionMiningRecipes() {}

    public static GTRecipeType createType(ResourceLocation id) {
        return new MiningRecipeType(id);
    }

    private static final class MiningRecipeType extends GTRecipeType {
        private final GTRecipeLookup miningLookup = new MiningLookup(this);

        private MiningRecipeType(ResourceLocation id) { super(id, "multiblock"); }

        @Override public GTRecipeLookup getLookup() { return miningLookup; }

        @Override public Iterator<GTRecipe> searchRecipe(IRecipeCapabilityHolder holder) {
            if (!holder.hasProxies()) return null;
            return getCategories().stream().flatMap(category -> getRecipesInCategory(category).stream())
                    .filter(recipe -> !recipe.isFuel() && recipe.matchRecipe(holder).isSuccess()
                            && recipe.matchTickRecipe(holder).isSuccess())
                    .iterator();
        }
    }

    /** The native input index cannot distinguish identical circuits at different recipe tiers. */
    private static final class MiningLookup extends GTRecipeLookup {
        private final Set<Integer> indexedCircuits = new HashSet<>();

        private MiningLookup(GTRecipeType type) { super(type); }

        @Override public boolean addRecipe(GTRecipe recipe) {
            if (recipe == null) return false;
            Integer circuit = circuitNumber(recipe);
            if (circuit != null && indexedCircuits.contains(circuit)) {
                recipe.recipeCategory.addRecipe(recipe);
                return true;
            }
            boolean indexed = super.addRecipe(recipe);
            if (indexed && circuit != null) indexedCircuits.add(circuit);
            if (!indexed) recipe.recipeCategory.addRecipe(recipe);
            return indexed;
        }

        @Override public void removeAllRecipes() {
            super.removeAllRecipes();
            indexedCircuits.clear();
        }

        private static Integer circuitNumber(GTRecipe recipe) {
            var items = recipe.getInputContents(ItemRecipeCapability.CAP);
            if (items.size() != 1) return null;
            Ingredient ingredient = ItemRecipeCapability.CAP.of(items.get(0).content);
            if (ingredient instanceof SizedIngredient sized) ingredient = sized.getInner();
            if (!(ingredient instanceof IntCircuitIngredient)) return null;
            var stacks = ingredient.getItems();
            return stacks.length == 1 ? IntCircuitBehaviour.getCircuitConfiguration(stacks[0]) : null;
        }
    }
}
