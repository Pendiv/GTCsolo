package DIV.gtcsolo.data;

import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModMachines;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        // fec レシピ: クアンタムスター×1, スカルクシュリーカー×2, ニッケルプラズマ144mb → fcore×1
        ModRecipeTypes.FEC.recipeBuilder(new ResourceLocation("gtcsolo", "quantum_construct"))
                .inputItems(GTItems.QUANTUM_STAR.asStack(1))
                .inputItems(Blocks.SCULK_SHRIEKER, 2)
                .inputFluids(new FluidStack(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA), 144))
                .outputItems(ModItems.FCORE.get(), 1)
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(writer);

        // マルチブロックコントローラー作業台レシピ
        // xcrystal を中央に、鉄ブロックを2進数パターンで配置
        //   bit0 bit1 bit2
        //   bit3  X  bit4
        //   bit5 bit6  _
        // FEC  = 1 = 0b0000001 → bit0
        // EEBF = 2 = 0b0000010 → bit1

        // FEC: iron at top-left
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachines.FEC.getBlock())
                .pattern("I ")
                .pattern(" X")
                .define('I', Blocks.IRON_BLOCK)
                .define('X', ModItems.XCRYSTAL.get())
                .unlockedBy("has_xcrystal", has(ModItems.XCRYSTAL.get()))
                .save(writer, new ResourceLocation("gtcsolo", "fec_controller"));

        // EEBF: iron at top-center
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachines.EEBF.getBlock())
                .pattern("I")
                .pattern("X")
                .define('I', Blocks.IRON_BLOCK)
                .define('X', ModItems.XCRYSTAL.get())
                .unlockedBy("has_xcrystal", has(ModItems.XCRYSTAL.get()))
                .save(writer, new ResourceLocation("gtcsolo", "eebf_controller"));
    }
}