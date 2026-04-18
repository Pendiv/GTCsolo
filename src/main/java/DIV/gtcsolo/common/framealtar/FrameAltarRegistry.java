package DIV.gtcsolo.common.framealtar;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FrameAltarRegistry {

    private static final List<FrameAltarRecipe> RECIPES = new ArrayList<>();

    public static void register(FrameAltarRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static FrameAltarRecipe.MatchResult findMatch(List<ItemStack> frameItems) {
        for (FrameAltarRecipe recipe : RECIPES) {
            FrameAltarRecipe.MatchResult result = recipe.match(frameItems);
            if (result.isSuccess()) {
                return result;
            }
        }
        return FrameAltarRecipe.MatchResult.FAIL;
    }

    public static FrameAltarRecipe findMatchingRecipe(List<ItemStack> frameItems) {
        for (FrameAltarRecipe recipe : RECIPES) {
            FrameAltarRecipe.MatchResult result = recipe.match(frameItems);
            if (result.isSuccess()) {
                return recipe;
            }
        }
        return null;
    }
}