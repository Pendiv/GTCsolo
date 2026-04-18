package DIV.gtcsolo;

import DIV.gtcsolo.registry.ModElements;
import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModMachines;
import DIV.gtcsolo.registry.ModMaterials;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
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
    public void registerElements() {
        ModElements.init();
    }

    @Override
    public String addonModId() {
        return Gtcsolo.MODID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        // FEC レシピ1: クアンタムスター×1, スカルクシュリーカー×2, ニッケルプラズマ144mb → fcore×1
        ModRecipeTypes.FEC.recipeBuilder(new ResourceLocation("gtcsolo", "quantum_construct"))
                .inputItems(GTItems.QUANTUM_STAR.asStack(1))
                .inputItems(new net.minecraft.world.item.ItemStack(Blocks.SCULK_SHRIEKER, 2))
                .inputFluids(new FluidStack(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA), 144))
                .outputItems(ModItems.FCORE.get(), 1)
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);

        // FEC レシピ2: fcore×1, fgear×2, ナクアドリア28800mb → ニュートロニウムブロック×16
        ModRecipeTypes.FEC.recipeBuilder(new ResourceLocation("gtcsolo", "neutronium_synthesis"))
                .inputItems(ModItems.FCORE.get(), 1)
                .inputItems(ModItems.FGEAR.get(), 2)
                .inputFluids(GTMaterials.Naquadria.getFluid(28800))
                .outputItems(ChemicalHelper.get(TagPrefix.block, GTMaterials.Neutronium, 16))
                .duration(12000)
                .EUt(GTValues.VA[GTValues.ZPM] * 2)
                .save(provider);

        // EBF テストレシピ: 吹込みダイヤモンド液体80mb + 黒曜石粉x2 → 精製黒曜石液体80mb
        // EV 2A (960 EU/t), 40秒 (800 ticks)
        Material infDiamond = GTCEuAPI.materialManager.getMaterial("gtcsolo:infusion_mekanism_diamond");
        Material infRefinedObs = GTCEuAPI.materialManager.getMaterial("gtcsolo:infusion_mekanism_refined_obsidian");
        if (infDiamond != null && infRefinedObs != null) {
            GTRecipeTypes.BLAST_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "infusion_refined_obsidian"))
                    .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.Obsidian, 2))
                    .inputFluids(new FluidStack(infDiamond.getFluid(), 80))
                    .outputFluids(new FluidStack(infRefinedObs.getFluid(), 80))
                    .blastFurnaceTemp(2000)
                    .duration(800)
                    .EUt(GTValues.VA[GTValues.EV] * 2)
                    .save(provider);
        }

        // SpaceForge レシピ1: 液体Nt + 精製グロウストーンプラズマ + 錫プラズマ288mb → ユーピタイトプラズマ144mb
        // UV 2A (524288 EU/t), 60秒 (1200 ticks)
        Material refinedGlowstone = GTCEuAPI.materialManager.getMaterial("gtcsolo:refined_glowstone");
        Material tinPlasma = GTCEuAPI.materialManager.getMaterial("gtcsolo:tin_plasma");
        Material jupitatePlasma = GTCEuAPI.materialManager.getMaterial("gtcsolo:jupitate_plasma");
        if (refinedGlowstone != null && tinPlasma != null && jupitatePlasma != null) {
            ModRecipeTypes.SPACEFORGE.recipeBuilder(new ResourceLocation("gtcsolo", "jupitate_plasma_synthesis"))
                    .inputFluids(GTMaterials.Neutronium.getFluid(288))
                    .inputFluids(new FluidStack(refinedGlowstone.getFluid(FluidStorageKeys.PLASMA), 288))
                    .inputFluids(new FluidStack(tinPlasma.getFluid(), 288))
                    .outputFluids(new FluidStack(jupitatePlasma.getFluid(), 144))
                    .duration(1200)
                    .EUt(GTValues.VA[GTValues.UV] * 2)
                    .save(provider);
        }

        // NH₃ + H₂O₂ → N₂H₄ + H₂O (化学反応機, IV, 40秒)
        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "hydrazine"))
                .inputFluids(GTMaterials.Ammonia.getFluid(2000))
                .inputFluids(new FluidStack(ModMaterials.HYDROGEN_PEROXIDE.getFluid(), 1000))
                .outputFluids(new FluidStack(ModMaterials.HYDRAZINE.getFluid(), 1000))
                .outputFluids(GTMaterials.Water.getFluid(1000))
                .duration(800)
                .EUt(GTValues.VA[GTValues.IV])
                .save(provider);

        // SbF₃ + F₂ → SbF₅ (化学反応機, LuV, 10秒)
        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "antimony_pentafluoride"))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.AntimonyTrifluoride, 1))
                .inputFluids(GTMaterials.Fluorine.getFluid(1000))
                .outputFluids(new FluidStack(ModMaterials.ANTIMONY_PENTAFLUORIDE.getFluid(), 1000))
                .duration(200)
                .EUt(GTValues.VA[GTValues.LuV])
                .save(provider);

        // Cl₂ + 3F₂ → 2ClF₃ (化学反応機)
        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "chlorine_trifluoride"))
                .inputFluids(GTMaterials.Chlorine.getFluid(1000))
                .inputFluids(GTMaterials.Fluorine.getFluid(3000))
                .outputFluids(new FluidStack(ModMaterials.CHLORINE_TRIFLUORIDE.getFluid(), 2000))
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);

        // === Chemical Combustion Generator レシピ ===

        // ClF₃ + H₂ → 3HF + HCl + IV 2A発電
        ModRecipeTypes.CHEMICAL_COMBUSTION_GENERATOR.recipeBuilder(
                        new ResourceLocation("gtcsolo", "ccg_clf3_hydrogen"))
                .inputFluids(new FluidStack(ModMaterials.CHLORINE_TRIFLUORIDE.getFluid(), 1000))
                .inputFluids(GTMaterials.Hydrogen.getFluid(3000))
                .outputFluids(new FluidStack(GTMaterials.HydrofluoricAcid.getFluid(), 3000))
                .outputFluids(new FluidStack(GTMaterials.HydrochloricAcid.getFluid(), 1000))
                .duration(200)
                .EUt(-GTValues.VA[GTValues.IV] * 2)
                .save(provider);

        // 2ClF₃ + 3N₂H₄ → 6HF + 2HCl + 3N₂ + LuV 1A発電
        ModRecipeTypes.CHEMICAL_COMBUSTION_GENERATOR.recipeBuilder(
                        new ResourceLocation("gtcsolo", "ccg_clf3_hydrazine"))
                .inputFluids(new FluidStack(ModMaterials.CHLORINE_TRIFLUORIDE.getFluid(), 2000))
                .inputFluids(new FluidStack(ModMaterials.HYDRAZINE.getFluid(), 3000))
                .outputFluids(new FluidStack(GTMaterials.HydrofluoricAcid.getFluid(), 6000))
                .outputFluids(new FluidStack(GTMaterials.HydrochloricAcid.getFluid(), 2000))
                .outputFluids(GTMaterials.Nitrogen.getFluid(3000))
                .duration(400)
                .EUt(-GTValues.VA[GTValues.LuV])
                .save(provider);
    }
}