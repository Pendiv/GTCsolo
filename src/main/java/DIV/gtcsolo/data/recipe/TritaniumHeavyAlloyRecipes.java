package DIV.gtcsolo.data.recipe;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.machine.IsmbfRules;
import DIV.gtcsolo.registry.ModRecipeTypes;
import DIV.gtcsolo.registry.material.TritaniumHeavyAlloyMaterials;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Two mass-equivalent routes, differing only in the state of the Tritanium feed. */
public final class TritaniumHeavyAlloyRecipes {
    private static final int MATERIAL_UNITS = 1 + 6 + 6 + 5 + 3;
    private static final int BASE_UNIT_TICKS = 10 * 20;
    private static final int DURATION_TICKS = BASE_UNIT_TICKS * MATERIAL_UNITS * 3 / 4;

    private TritaniumHeavyAlloyRecipes() {}

    public static void register(Consumer<FinishedRecipe> provider) {
        for (boolean liquidTritanium : new boolean[]{false, true}) {
            String name = liquidTritanium ? "tritanium_heavy_alloy_from_liquid_tritanium" : "tritanium_heavy_alloy";
            var recipe = ModRecipeTypes.EXTREME_TEMPERATURE_BLAST_FURNACE
                    .recipeBuilder(new ResourceLocation(Gtcsolo.MODID, name));
            if (liquidTritanium) recipe.inputFluids(GTMaterials.Tritanium.getFluid(GTValues.L));
            else recipe.inputItems(TagPrefix.dust, GTMaterials.Tritanium);
            recipe.inputItems(TagPrefix.dust, GTMaterials.Trinium, 6)
                    .inputItems(TagPrefix.dust, GTMaterials.NaquadahEnriched, 6)
                    .inputItems(TagPrefix.dust, GTMaterials.Netherite, 5)
                    .inputItems(TagPrefix.dust, GTMaterials.Bismuth, 3)
                    .outputFluids(TritaniumHeavyAlloyMaterials.TRITANIUM_HEAVY_ALLOY.getFluid(
                            GTValues.L * MATERIAL_UNITS))
                    .duration(DURATION_TICKS).EUt(GTValues.V[GTValues.UHV])
                    .blastFurnaceTemp(15_000)
                    .addData(IsmbfRules.START_TEMPERATURE, 10_000);
            recipe.save(provider);
        }
    }
}
