package DIV.gtcsolo.integration.jei;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.common.TooltipDisplayHelper;
import DIV.gtcsolo.integration.mekanism.ChemicalBridge;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JeiPlugin
public class GtcSoloJeiPlugin implements IModPlugin {

    private static volatile IJeiRuntime runtime;

    private static final IIngredientSubtypeInterpreter<ItemStack> TOOLTIP_INTERPRETER =
            (stack, context) -> TooltipDisplayHelper.getSubtypeKey(stack);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Gtcsolo.MODID, "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            if (Gtcsolo.MODID.equals(id.getNamespace())) {
                registration.registerSubtypeInterpreter(item, TOOLTIP_INTERPRETER);
            }
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ChemicalConversionCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ChemicalConversionRecipe> recipes = buildConversionRecipes();
        registration.addRecipes(ChemicalConversionCategory.RECIPE_TYPE, recipes);
        Gtcsolo.LOGGER.info("[JEI] Registered {} chemical conversion recipes", recipes.size());
    }

    private List<ChemicalConversionRecipe> buildConversionRecipes() {
        List<ChemicalConversionRecipe> recipes = new ArrayList<>();

        for (Map.Entry<String, String> entry : ChemicalBridge.getAllMappings().entrySet()) {
            String chemKey = entry.getKey();
            String materialName = entry.getValue();

            // chemKey = "gas:mekanism:hydrogen" → type=gas
            String[] parts = chemKey.split(":", 3);
            if (parts.length < 3) continue;

            ChemicalBridge.ChemType chemType;
            try {
                chemType = ChemicalBridge.ChemType.valueOf(parts[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            // Gas と Infusion のみ（現時点で変換ブロックが対応している分）
            if (chemType != ChemicalBridge.ChemType.GAS && chemType != ChemicalBridge.ChemType.INFUSION) {
                continue;
            }

            Material mat = GTCEuAPI.materialManager.getMaterial("gtcsolo:" + materialName);
            if (mat == null || !mat.hasFluid()) continue;

            net.minecraft.world.level.material.Fluid fluid = mat.getFluid();
            if (fluid == null) continue;

            recipes.add(new ChemicalConversionRecipe(
                    new FluidStack(fluid, 1000),
                    chemKey,
                    chemType,
                    materialName
            ));
        }

        return recipes;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        Gtcsolo.LOGGER.info("Captured JEI runtime in GtcSoloJeiPlugin");
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static IJeiRuntime getRuntimeOrNull() {
        return runtime;
    }

    public static IJeiRuntime getRuntimeOrThrow() {
        if (runtime == null) {
            throw new IllegalStateException(
                    "JEI runtime is not available yet. Join a world, wait for JEI to finish starting, then run the command again."
            );
        }
        return runtime;
    }
}