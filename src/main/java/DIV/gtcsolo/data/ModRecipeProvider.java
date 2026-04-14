package DIV.gtcsolo.data;

import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModMachines;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        // FEC機械レシピは GtcSoloAddon.addRecipes() で登録（ランタイムレシピ）。
        // ここではクラフトテーブルレシピ（runData用JSON生成）のみ定義する。

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

        // CC: binary 3 = 0b11 → bit0 + bit1 (top-left + top-center)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachines.CC.getBlock())
                .pattern("II")
                .pattern(" X")
                .define('I', Blocks.IRON_BLOCK)
                .define('X', ModItems.XCRYSTAL.get())
                .unlockedBy("has_xcrystal", has(ModItems.XCRYSTAL.get()))
                .save(writer, new ResourceLocation("gtcsolo", "cc_controller"));
    }
}