package DIV.gtcsolo;

import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModMachines;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Consumer;

/**
 * GTCEu アドオン。
 * IGTAddon.addRecipes() はゲーム起動時にランタイム呼び出しされるため、
 * ここに GTCEu 機械レシピを登録する。
 * (ModRecipeProvider はあくまで runData 用のデータ生成専用)
 */
@GTAddon
public class GtcSoloAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return ModMachines.REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        // 初期化は Gtcsolo コンストラクタで完結しているため不要
    }

    @Override
    public String addonModId() {
        return Gtcsolo.MODID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        // FEC レシピ: クアンタムスター×1, スカルクシュリーカー×2, ニッケルプラズマ144mb → fcore×1
        ModRecipeTypes.FEC.recipeBuilder(ResourceLocation.fromNamespaceAndPath("gtcsolo", "quantum_construct"))
                .inputItems(GTItems.QUANTUM_STAR.asStack(1))
                .inputItems(Blocks.SCULK_SHRIEKER, 2)
                .inputFluids(new FluidStack(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA), 144))
                .outputItems(ModItems.FCORE.get(), 1)
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);
    }
}