package DIV.gtcsolo.integration.jei.performance;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.ingredients.IIngredientSupplier;
import mezz.jei.library.ingredients.TypedIngredient;
import mezz.jei.library.plugins.vanilla.ingredients.ItemStackHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class ArcaneAnvilIngredientIndex {
    private static final RecipeFields FIELDS = findRecipeFields();

    private ArcaneAnvilIngredientIndex() {}

    @Nullable
    public static IIngredientSupplier tryCreate(Object recipe, IIngredientManager manager) {
        if (FIELDS == null || recipe == null || recipe.getClass() != ArcaneAnvilJeiRecipe.class) return null;
        try {
            if (!(FIELDS.type().get(recipe) instanceof Enum<?> type) || !type.name().equals("Imbue")) return null;
            Item base = (Item) FIELDS.leftItem().get(recipe);
            var helper = manager.getIngredientHelper(VanillaTypes.ITEM_STACK);
            if (base == null || helper.getClass() != ItemStackHelper.class) return null;
            ItemStack input = new ItemStack(base);
            if (helper.hasSubtypes(input)) return null;
            var typedInput = TypedIngredient.createAndFilterInvalid(helper, VanillaTypes.ITEM_STACK, input, false);
            if (typedInput.isEmpty()) return null;

            List<ITypedIngredient<?>> inputs = new ArrayList<>();
            List<ITypedIngredient<?>> outputs = new ArrayList<>(1);
            inputs.add(typedInput.get());
            Item scrollItem = ItemRegistry.SCROLL.get();
            for (var spell : SpellRegistry.getEnabledSpells()) {
                IntStream.rangeClosed(spell.getMinLevel(), spell.getMaxLevel()).forEach(level -> {
                    ItemStack scroll = new ItemStack(scrollItem);
                    ISpellContainer.createScrollContainer(spell, level, scroll);
                    TypedIngredient.createAndFilterInvalid(helper, VanillaTypes.ITEM_STACK, scroll, false).ifPresent(inputs::add);
                    // JEI's RecipeMap indexes non-subtyped item stacks by Item. Each imbued output shares that key.
                    if (outputs.isEmpty()) {
                        ItemStack output = new ItemStack(base);
                        ISpellContainer.createScrollContainer(spell, level, output);
                        TypedIngredient.createAndFilterInvalid(helper, VanillaTypes.ITEM_STACK, output, false).ifPresent(outputs::add);
                    }
                });
            }
            return role -> switch (role) {
                case INPUT -> inputs;
                case OUTPUT -> outputs;
                default -> List.of();
            };
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return null;
        }
    }

    @Nullable
    private static RecipeFields findRecipeFields() {
        // The Imbue branch and its package-private fields were checked against this Iron's release.
        var mod = ModList.get().getModContainerById("irons_spellbooks");
        if (mod.isEmpty() || !mod.get().getModInfo().getVersion().toString().equals("1.20.1-3.15.6")) return null;
        try {
            Field type = ArcaneAnvilJeiRecipe.class.getDeclaredField("type");
            Field leftItem = ArcaneAnvilJeiRecipe.class.getDeclaredField("leftItem");
            return type.trySetAccessible() && leftItem.trySetAccessible() ? new RecipeFields(type, leftItem) : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return null;
        }
    }

    private record RecipeFields(Field type, Field leftItem) {}
}
