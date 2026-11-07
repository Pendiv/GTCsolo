package DIV.gtcsolo.integration.jei.performance;

import com.sakurafuld.hyperdaimc.HyperCommonConfig;
import com.sakurafuld.hyperdaimc.api.content.MaterializerRecipeEvent;
import com.sakurafuld.hyperdaimc.content.over.materializer.MaterializerHandler.Process;
import com.sakurafuld.hyperdaimc.mixin.materializer.Ingredient$TagValueAccessor;
import com.sakurafuld.hyperdaimc.mixin.materializer.IngredientAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * JEI-only catalog builder for Hyperlink 1201.2.4.
 * Retains native recipe/tag expansion and both Materializer events, replacing only ordered linear aggregation.
 * Upstream: Sakurafuld/Hyperlink (MIT); the exact installed 7052766 artifact is recorded in the audit.
 */
public final class IndexedMaterializerRecipes {
    private IndexedMaterializerRecipes() {}

    public static List<Process> load(Level level) {
        Set<Recipe<?>> searched = HyperCommonConfig.MATERIALIZER_RECIPE.get().stream()
                .map(String.class::cast)
                .map(ResourceLocation::new)
                .filter(ForgeRegistries.RECIPE_TYPES::containsKey)
                .map(ForgeRegistries.RECIPE_TYPES::getValue)
                .map(type -> level.getRecipeManager().getAllRecipesFor(cast(type)))
                .flatMap(Collection::stream)
                .filter(recipe -> !HyperCommonConfig.MATERIALIZER_RECIPE_BLACKLIST.get()
                        .contains(recipe.getId().toString()))
                .collect(Collectors.toSet());

        MinecraftForge.EVENT_BUS.post(new MaterializerRecipeEvent.Load(level, searched));
        List<Process> processes = new ArrayList<>();
        MaterializerProcessIndex<Process> index = new MaterializerProcessIndex<>(Process::result);
        BooleanSupplier stackIngredients = () -> HyperCommonConfig.MATERIALIZER_STACK_INGREDIENTS.get();

        for (Recipe<?> recipe : searched) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;

            List<ItemStack> ingredients = new ArrayList<>();
            ItemStackAccumulator accumulator = new ItemStackAccumulator(ingredients, stackIngredients);
            recipe.getIngredients().stream()
                    .filter(ingredient -> !ingredient.isEmpty())
                    .flatMap(ingredient -> Arrays.stream(((IngredientAccessor) ingredient).getValues()))
                    .filter(value -> !(value instanceof Ingredient.TagValue tag)
                            || !HyperCommonConfig.MATERIALIZER_TAG_BLACKLIST.get()
                                    .contains(((Ingredient$TagValueAccessor) tag).getTag().location().toString()))
                    .flatMap(value -> value.getItems().stream())
                    .map(ItemStack::copy)
                    .forEach(accumulator::add);

            MaterializerRecipeEvent.Add event = new MaterializerRecipeEvent.Add(level, recipe, ingredients);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled() || ingredients.isEmpty()) continue;

            Process existing = index.find(result);
            if (existing != null) {
                // Rebuild after event dispatch: listeners may have mutated an earlier ingredient list.
                ItemStackAccumulator merge = new ItemStackAccumulator(existing.ingredients(), stackIngredients);
                ingredients.forEach(merge::add);
            } else {
                Process process = new Process(result.copy(), ingredients);
                processes.add(process);
                index.add(process);
            }
        }
        return processes;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Recipe<?>> RecipeType<T> cast(RecipeType<?> type) {
        return (RecipeType<T>) type;
    }
}
