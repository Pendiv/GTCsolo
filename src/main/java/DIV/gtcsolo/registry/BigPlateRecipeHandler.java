package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.UnificationEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Generates recipes for {@link ModMaterials#BIG_PLATE_PREFIX} through GTCEu's
 * normal material-recipe pipeline.
 *
 * <p>All material traversal and recipe construction stays in Java. This avoids
 * the Rhino collection-conversion issue that affected the old KubeJS bridge and
 * keeps the recipes aligned with GTCEu's own generated-part handlers.</p>
 */
public final class BigPlateRecipeHandler {

    private static final int FORGE_HAMMER_DURATION = 8 * 20;
    private static final long HV_TWO_AMP = 960L;

    private BigPlateRecipeHandler() {}

    public static void register(Consumer<FinishedRecipe> provider) {
        AtomicInteger registered = new AtomicInteger();
        AtomicInteger skippedNoBlock = new AtomicInteger();

        ModMaterials.BIG_PLATE_PREFIX.executeHandler(
                provider,
                PropertyKey.INGOT,
                (prefix, material, property, recipeProvider) -> {
                    if (registerForMaterial(prefix, material, property, recipeProvider)) {
                        registered.incrementAndGet();
                    } else {
                        skippedNoBlock.incrementAndGet();
                    }
                });

        Gtcsolo.LOGGER.info(
                "[big_plate] Registered {} crafting + {} forge hammer recipes (skippedNoBlock={})",
                registered.get(), registered.get(), skippedNoBlock.get());
    }

    private static boolean registerForMaterial(TagPrefix prefix, Material material, IngotProperty property,
                                               Consumer<FinishedRecipe> provider) {
        ItemStack output = ChemicalHelper.get(prefix, material);
        ItemStack block = ChemicalHelper.get(TagPrefix.block, material);
        ResourceLocation outputId = ForgeRegistries.ITEMS.getKey(output.getItem());
        boolean vanillaNetheriteBlock = material == GTMaterials.Netherite;
        if (output.isEmpty() || (!vanillaNetheriteBlock && block.isEmpty()) || outputId == null) {
            Gtcsolo.LOGGER.warn(
                    "[big_plate] Skipping {} because no storage block item is registered",
                    material.getResourceLocation());
            return false;
        }

        String recipeSuffix = outputId.getNamespace() + "_" + outputId.getPath().replace('/', '_');

        Object blockIngredient = vanillaNetheriteBlock
                ? Blocks.NETHERITE_BLOCK
                : new UnificationEntry(TagPrefix.block, material);
        VanillaRecipeHelper.addShapedRecipe(
                provider,
                new ResourceLocation(Gtcsolo.MODID, "big_plate/craft_" + recipeSuffix),
                output,
                "BBB",
                "BhB",
                "BBB",
                'B', blockIngredient);

        var forgeHammer = GTRecipeTypes.FORGE_HAMMER_RECIPES
                .recipeBuilder(new ResourceLocation(Gtcsolo.MODID, "big_plate/forge_hammer_" + recipeSuffix));
        if (vanillaNetheriteBlock) {
            forgeHammer.inputItems(Blocks.NETHERITE_BLOCK.asItem(), 7);
        } else {
            forgeHammer.inputItems(TagPrefix.block, material, 7);
        }
        forgeHammer.outputItems(prefix, material)
                .duration(FORGE_HAMMER_DURATION)
                .EUt(HV_TWO_AMP)
                .save(provider);

        return true;
    }
}
