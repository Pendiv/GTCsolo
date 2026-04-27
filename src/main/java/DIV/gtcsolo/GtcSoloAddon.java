package DIV.gtcsolo;

import DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities;
import DIV.gtcsolo.integration.mekanism.capability.ChemicalIngredient;
import DIV.gtcsolo.integration.mekanism.capability.ChemicalRecipeComponents;
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
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Consumer;

/**
 * GTCEu アドオン。
 * IGTAddon.addRecipes() はゲーム起動時にランタイム呼び出しされるため、
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
    public void registerRecipeCapabilities() {
        Gtcsolo.LOGGER.info("[ChemCap] Addon.registerRecipeCapabilities called");
        ChemicalCapabilities.init();
        disableCustomUIForChemicalCompatibleTypes();
    }

    @Override
    public void registerWorldgenLayers() {
        DIV.gtcsolo.worldgen.GtcsoloWorldGenLayers.init();
    }

    /**
     * GTCEu の一部 recipe type は `ui/recipe_type/xxx.rtui` で custom JEI レイアウトを持つが、
     * それは item/fluid/EU のスロットしか含まない. 我々が後付けで追加した chemical cap は
     * layout 外になり slot が描画されない.
     *
     * 対策: 該当 recipe type の `customUICache` を空で上書きし、
     * `hasCustomUI() = false` を強制 → default `addInventorySlotGroup` 経路で全 cap の slot 作成.
     *
     * 副作用: 対象 recipe type の JEI 見た目が default grid になる. chemical 使わないレシピも同じ表示.
     */
    private void disableCustomUIForChemicalCompatibleTypes() {
        com.gregtechceu.gtceu.api.recipe.GTRecipeType[] targets = {
                com.gregtechceu.gtceu.common.data.GTRecipeTypes.BLAST_RECIPES,
                com.gregtechceu.gtceu.common.data.GTRecipeTypes.LARGE_CHEMICAL_RECIPES,
        };
        for (com.gregtechceu.gtceu.api.recipe.GTRecipeType type : targets) {
            if (type == null) {
                Gtcsolo.LOGGER.warn("[ChemCap] target recipe type is null at this lifecycle phase");
                continue;
            }
            try {
                com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI ui = type.getRecipeUI();
                Gtcsolo.LOGGER.info("[ChemCap] BEFORE reflection: {} hasCustomUI()={}",
                        type.registryName, ui.hasCustomUI());
                // xeiSize も null にして再計算させる
                java.lang.reflect.Field cacheField =
                        com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI.class
                                .getDeclaredField("customUICache");
                cacheField.setAccessible(true);
                cacheField.set(ui, new net.minecraft.nbt.CompoundTag());
                try {
                    java.lang.reflect.Field xeiSizeField =
                            com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI.class
                                    .getDeclaredField("xeiSize");
                    xeiSizeField.setAccessible(true);
                    xeiSizeField.set(ui, null);
                } catch (NoSuchFieldException ignored) {}
                Gtcsolo.LOGGER.info("[ChemCap] AFTER reflection: {} hasCustomUI()={}",
                        type.registryName, ui.hasCustomUI());
            } catch (Throwable t) {
                Gtcsolo.LOGGER.error("[ChemCap] Failed to disable custom UI for {}",
                        type.registryName, t);
            }
        }
    }

    @Override
    public void registerRecipeKeys(com.gregtechceu.gtceu.api.addon.events.KJSRecipeKeyEvent event) {
        Gtcsolo.LOGGER.info("[ChemCap] Addon.registerRecipeKeys called");
        ChemicalRecipeComponents.registerRecipeKeys(event);
    }

    @Override
    public String addonModId() {
        return Gtcsolo.MODID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        // FEC レシピ1: fgear×2 + エンチャ本(種類不問) + 現世液体空気1000mb → fcore×1
        ModRecipeTypes.FEC.recipeBuilder(new ResourceLocation("gtcsolo", "quantum_construct"))
                .inputItems(ModItems.FGEAR.get(), 2)
                .inputItems(net.minecraft.world.item.Items.ENCHANTED_BOOK, 1)
                .inputFluids(GTMaterials.LiquidAir.getFluid(1000))
                .outputItems(ModItems.FCORE.get(), 1)
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);

        // EEBF fissile_fuel テストレシピ: 鉄dust + 核分裂燃料ガス 100mb → ウラン dust
        //   新 chemical capability (GAS) 経由の end-to-end 動作確認用
        //   EEBF 側の INPUT_GAS hatch から fissile_fuel を供給することで発動する...はず
        Gtcsolo.LOGGER.info("[ChemCap] registering test_iron_to_uranium recipe (BLAST + GAS input)");
        try {
            GTRecipeTypes.BLAST_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "test_iron_to_uranium"))
                    .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.Iron, 1))
                    .input(DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.GAS,
                            DIV.gtcsolo.integration.mekanism.capability.ChemicalIngredient
                                    .gas("mekanism:fissile_fuel", 100))
                    .outputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.Uranium238, 1))
                    .blastFurnaceTemp(3000)
                    .duration(400)
                    .EUt(GTValues.VA[GTValues.EV])
                    .save(provider);
            Gtcsolo.LOGGER.info("[ChemCap] test_iron_to_uranium recipe saved OK");
        } catch (Exception e) {
            Gtcsolo.LOGGER.error("[ChemCap] test_iron_to_uranium recipe FAILED: {}", e.toString(), e);
        }


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

        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "hydrazine"))
                .inputFluids(GTMaterials.Ammonia.getFluid(2000))
                .inputFluids(new FluidStack(ModMaterials.HYDROGEN_PEROXIDE.getFluid(), 1000))
                .outputFluids(new FluidStack(ModMaterials.HYDRAZINE.getFluid(), 1000))
                .outputFluids(GTMaterials.Water.getFluid(1000))
                .duration(800)
                .EUt(GTValues.VA[GTValues.IV])
                .save(provider);

        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "antimony_pentafluoride"))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.AntimonyTrifluoride, 1))
                .inputFluids(GTMaterials.Fluorine.getFluid(1000))
                .outputFluids(new FluidStack(ModMaterials.ANTIMONY_PENTAFLUORIDE.getFluid(), 1000))
                .duration(200)
                .EUt(GTValues.VA[GTValues.LuV])
                .save(provider);

        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "chlorine_trifluoride"))
                .inputFluids(GTMaterials.Chlorine.getFluid(1000))
                .inputFluids(GTMaterials.Fluorine.getFluid(3000))
                .outputFluids(new FluidStack(ModMaterials.CHLORINE_TRIFLUORIDE.getFluid(), 2000))
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save(provider);


        ModRecipeTypes.CHEMICAL_COMBUSTION_GENERATOR.recipeBuilder(
                        new ResourceLocation("gtcsolo", "ccg_clf3_hydrogen"))
                .inputFluids(new FluidStack(ModMaterials.CHLORINE_TRIFLUORIDE.getFluid(), 1000))
                .inputFluids(GTMaterials.Hydrogen.getFluid(3000))
                .outputFluids(new FluidStack(GTMaterials.HydrofluoricAcid.getFluid(), 3000))
                .outputFluids(new FluidStack(GTMaterials.HydrochloricAcid.getFluid(), 1000))
                .duration(200)
                .EUt(-GTValues.VA[GTValues.IV] * 2)
                .save(provider);

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

        // =====================================================================
        //  Mekanism Infuser — 合金チェーン
        //  infused(LV) → reinforced(MV) → atomic(HV) → hypercharged(IV, EMek)
        // =====================================================================
        addMekanismInfuserRecipes(provider);

        // =====================================================================
        //  超電導ワイヤー素材レシピ
        // =====================================================================
        addSuperconductorRecipes(provider);
    }

    private void addSuperconductorRecipes(Consumer<FinishedRecipe> provider) {

        GTRecipeTypes.ALLOY_SMELTER_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "tariton_ingot"))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.RedAlloy, 2))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.BlueAlloy, 3))
                .outputItems(ChemicalHelper.get(TagPrefix.ingot, ModMaterials.TARITON, 5))
                .duration(100)
                .EUt(GTValues.VA[GTValues.LV])
                .save(provider);

        GTRecipeTypes.BLAST_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "endnium_ingot"))
                .inputItems(ChemicalHelper.get(TagPrefix.ingot, GTMaterials.Tungsten, 1))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.Endstone, 5))
                .outputItems(ChemicalHelper.get(TagPrefix.ingot, ModMaterials.ENDNIUM, 1))
                .blastFurnaceTemp(3500)
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save(provider);


        GTRecipeTypes.MIXER_RECIPES.recipeBuilder(new ResourceLocation("gtcsolo", "hssx_dust"))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.HSSG, 9))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.HSSS, 9))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.HSSE, 9))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.RoseGold, 5))
                .inputItems(ChemicalHelper.get(TagPrefix.dust, GTMaterials.StainlessSteel, 9))
                .outputItems(ChemicalHelper.get(TagPrefix.dust, ModMaterials.HSSX, 41))
                .duration(600)
                .EUt(GTValues.VA[GTValues.EV])
                .save(provider);
    }

    private void addMekanismInfuserRecipes(Consumer<FinishedRecipe> provider) {
        // ChemicalCapabilities.INFUSION 方式で Mek infusion を直接入力として扱う
        net.minecraft.world.item.Item alloyInfused = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("mekanism", "alloy_infused"));
        net.minecraft.world.item.Item alloyReinforced = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("mekanism", "alloy_reinforced"));
        net.minecraft.world.item.Item alloyAtomic = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("mekanism", "alloy_atomic"));
        net.minecraft.world.item.Item alloyHypercharged = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("evolvedmekanism", "alloy_hypercharged"));

        if (alloyInfused != null) {
            ModRecipeTypes.MEKANISM_INFUSER.recipeBuilder(
                            new ResourceLocation("gtcsolo", "mekanism_infuser_alloy_infused"))
                    .inputItems(ChemicalHelper.get(TagPrefix.ingot, GTMaterials.Iron, 1))
                    .input(ChemicalCapabilities.INFUSION,
                            ChemicalIngredient.infusion("mekanism:redstone", 10))
                    .outputItems(new net.minecraft.world.item.ItemStack(alloyInfused, 1))
                    .duration(100)
                    .EUt(GTValues.VA[GTValues.LV])
                    .save(provider);
        }

        if (alloyInfused != null && alloyReinforced != null) {
            ModRecipeTypes.MEKANISM_INFUSER.recipeBuilder(
                            new ResourceLocation("gtcsolo", "mekanism_infuser_alloy_reinforced"))
                    .inputItems(new net.minecraft.world.item.ItemStack(alloyInfused, 1))
                    .input(ChemicalCapabilities.INFUSION,
                            ChemicalIngredient.infusion("mekanism:diamond", 20))
                    .outputItems(new net.minecraft.world.item.ItemStack(alloyReinforced, 1))
                    .duration(140)
                    .EUt(GTValues.VA[GTValues.MV])
                    .save(provider);
        }

        if (alloyReinforced != null && alloyAtomic != null) {
            ModRecipeTypes.MEKANISM_INFUSER.recipeBuilder(
                            new ResourceLocation("gtcsolo", "mekanism_infuser_alloy_atomic"))
                    .inputItems(new net.minecraft.world.item.ItemStack(alloyReinforced, 1))
                    .input(ChemicalCapabilities.INFUSION,
                            ChemicalIngredient.infusion("mekanism:refined_obsidian", 40))
                    .outputItems(new net.minecraft.world.item.ItemStack(alloyAtomic, 1))
                    .duration(180)
                    .EUt(GTValues.VA[GTValues.HV])
                    .save(provider);
        }

        if (alloyAtomic != null && alloyHypercharged != null) {
            ModRecipeTypes.MEKANISM_INFUSER.recipeBuilder(
                            new ResourceLocation("gtcsolo", "mekanism_infuser_alloy_hypercharged"))
                    .inputItems(new net.minecraft.world.item.ItemStack(alloyAtomic, 1))
                    .input(ChemicalCapabilities.INFUSION,
                            ChemicalIngredient.infusion("evolvedmekanism:uranium", 20))
                    .outputItems(new net.minecraft.world.item.ItemStack(alloyHypercharged, 1))
                    .duration(260)
                    .EUt(GTValues.VA[GTValues.IV])
                    .save(provider);
        }
    }
}