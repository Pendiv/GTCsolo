package DIV.gtcsolo.data.recipe;

import DIV.gtcsolo.registry.IronCastingMolds;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.UnificationEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;

public final class IronCastingMoldRecipes {
    private IronCastingMoldRecipes() {}
    public static void register(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, id("iron_empty_mold"), new ItemStack(IronCastingMolds.EMPTY.get()),
                "hf", "PP", "PP", 'P', new UnificationEntry(TagPrefix.plate, GTMaterials.Iron));
        // Same strict hammer/blank positions as GregTech 1.6.4's normal casting molds.
        String[][] shapes = {
                {"plate", " h ", " S ", "   "}, {"gear", "   ", " Sh", "   "},
                {"credit", "h  ", " S ", "   "}, {"bottle", "   ", " S ", "  h"},
                {"ingot", "   ", " S ", " h "}, {"ball", "   ", " S ", "h  "},
                {"block", "   ", "hS ", "   "}, {"nugget", "S h", "   ", "   "},
                {"cylinder", "  S", "   ", "  h"}, {"anvil", "  S", "   ", " h "},
                {"name", "  S", "   ", "h  "}, {"small_gear", "   ", "   ", "h S"},
                {"rotor", "  h", " S ", "   "}, {"pill", "  h", "  S", "   "}
        };
        for (String[] shape : shapes) VanillaRecipeHelper.addStrictShapedRecipe(provider,
                id("iron_" + shape[0] + "_casting_mold"), new ItemStack(IronCastingMolds.MOLDS.get(shape[0]).get()),
                shape[1], shape[2], shape[3], 'S', new ItemStack(IronCastingMolds.EMPTY.get()));
    }
    private static ResourceLocation id(String path) { return new ResourceLocation("gtcsolo", path); }
}
