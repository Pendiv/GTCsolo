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
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
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

    /**
     * 既存 GTCEu material に generate flag を後付けする hook。
     * material registry frozen 前に呼ばれるので addFlags() 可能。
     * vanilla の Netherite はGTCEuで ingot + tool のみ持つので、
     * 主要な派生 prefix item (plate / dense / rod / long_rod / gear / small_gear /
     * foil / ring / spring / rotor) を生やす。
     * 仕様詰めの経緯は ja_jp トーク 2026-05-25 参照、 「特別視しない中庸案」。
     */
    @Override
    public void registerMaterials() {
        GTMaterials.Netherite.addFlags(
                MaterialFlags.GENERATE_PLATE,
                MaterialFlags.GENERATE_DENSE,
                MaterialFlags.GENERATE_ROD,
                MaterialFlags.GENERATE_LONG_ROD,
                MaterialFlags.GENERATE_GEAR,
                MaterialFlags.GENERATE_SMALL_GEAR,
                MaterialFlags.GENERATE_FOIL,
                MaterialFlags.GENERATE_RING,
                MaterialFlags.GENERATE_SPRING,
                MaterialFlags.GENERATE_ROTOR
        );
    }

    @Override
    public void registerRecipeCapabilities() {
        Gtcsolo.LOGGER.debug("[ChemCap] Addon.registerRecipeCapabilities called");
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
                Gtcsolo.LOGGER.debug("[ChemCap] BEFORE reflection: {} hasCustomUI()={}",
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
                Gtcsolo.LOGGER.debug("[ChemCap] AFTER reflection: {} hasCustomUI()={}",
                        type.registryName, ui.hasCustomUI());
            } catch (Throwable t) {
                Gtcsolo.LOGGER.error("[ChemCap] Failed to disable custom UI for {}",
                        type.registryName, t);
            }
        }
    }

    @Override
    public void registerRecipeKeys(com.gregtechceu.gtceu.api.addon.events.KJSRecipeKeyEvent event) {
        Gtcsolo.LOGGER.debug("[ChemCap] Addon.registerRecipeKeys called");
        ChemicalRecipeComponents.registerRecipeKeys(event);
    }

    @Override
    public String addonModId() {
        return Gtcsolo.MODID;
    }

    private static final String[] FA_PIECES = {"helmet", "chestplate", "leggings", "boots"};

    private static net.minecraft.world.item.Item faItem(String path) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("fantasy_armor", path));
    }

    /**
     * Fantasy Builder の防具アップグレード群 (credit /craftpattern 由来を Java 移植)。
     * 入力順は正準順 (教主指定): 触媒 (moon_crystal / arcane_ingot) → 装備一式4点 → fgear → fcore
     * (= rtui の item_in_0.. と対応)。 同一入力で分岐する段は {@code circuitMeta(n)} で区別する。
     */
    private void addFantasyArmorBuildRecipes(Consumer<FinishedRecipe> provider) {
        net.minecraft.world.item.Item moon = faItem("moon_crystal");
        net.minecraft.world.item.Item arcane = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("irons_spellbooks", "arcane_ingot"));
        if (moon == null || arcane == null) {
            Gtcsolo.LOGGER.warn("[fantasy_builder] moon_crystal / arcane_ingot 未解決 → 防具レシピをスキップ");
            return;
        }
        // 入口 (moon_crystal)
        fab(provider, moon, 4, "hero", 2, 1, "chess_board_knight");
        fab(provider, moon, 1, "chess_board_knight", 1, 1, "redeemer");
        // chess 分岐 (circuit 1/2)
        fab(provider, arcane, 1, "chess_board_knight", 1, 1, "gilded_hunt");
        fab(provider, arcane, 1, "chess_board_knight", 1, 2, "flesh_of_the_feaster");
        fab(provider, arcane, 1, "gilded_hunt", 1, 1, "dark_cover");
        // dark_cover 分岐 (circuit 1/2/3)
        fab(provider, arcane, 1, "dark_cover", 1, 1, "crucible_knight");
        fab(provider, arcane, 1, "dark_cover", 1, 2, "lady_maria");
        fab(provider, arcane, 1, "dark_cover", 1, 3, "dragonslayer");
        // crucible_knight 分岐 (circuit 1/2/3)
        fab(provider, arcane, 1, "crucible_knight", 1, 1, "golden_execution");
        fab(provider, arcane, 1, "crucible_knight", 1, 2, "wandering_wizard");
        fab(provider, arcane, 1, "crucible_knight", 1, 3, "old_knight");
        fab(provider, arcane, 1, "dragonslayer", 1, 1, "golden_horns");
        fab(provider, arcane, 1, "golden_horns", 1, 1, "ornstein");
        fab(provider, arcane, 1, "flesh_of_the_feaster", 1, 1, "grave_sentinel");
        fab(provider, arcane, 1, "grave_sentinel", 1, 1, "dark_lord");
        fab(provider, arcane, 1, "dark_lord", 1, 1, "sunset_wings");
        // golden_execution 分岐 (circuit 1/2)
        fab(provider, arcane, 1, "golden_execution", 1, 1, "spark_of_dawn");
        fab(provider, arcane, 1, "golden_execution", 1, 2, "ronin");
        fab(provider, arcane, 1, "spark_of_dawn", 1, 1, "malenia");
        fab(provider, arcane, 1, "ronin", 1, 1, "wind_worshipper");
        fab(provider, arcane, 1, "wandering_wizard", 1, 1, "fog_guard");
        fab(provider, arcane, 1, "old_knight", 1, 1, "twinned");
        fab(provider, arcane, 1, "twinned", 1, 1, "forgotten_trace");
    }

    /** 1 段分: 触媒 + 元防具一式 + fgear + fcore (circuit 分岐) → 次防具一式。 装備未解決ならスキップ。 */
    private void fab(Consumer<FinishedRecipe> provider, net.minecraft.world.item.Item catalyst,
                     int catalystCount, String fromSet, int fgearCount, int circuit, String toSet) {
        net.minecraft.world.item.Item[] from = new net.minecraft.world.item.Item[FA_PIECES.length];
        net.minecraft.world.item.Item[] to = new net.minecraft.world.item.Item[FA_PIECES.length];
        for (int i = 0; i < FA_PIECES.length; i++) {
            from[i] = faItem(fromSet + "_" + FA_PIECES[i]);
            to[i] = faItem(toSet + "_" + FA_PIECES[i]);
            if (from[i] == null || to[i] == null) {
                Gtcsolo.LOGGER.warn("[fantasy_builder] 防具未解決 {} -> {} → skip", fromSet, toSet);
                return;
            }
        }
        var b = ModRecipeTypes.FANTASY_ARMOR_BUILD.recipeBuilder(
                new ResourceLocation("gtcsolo", "fab_" + fromSet + "_to_" + toSet));
        // 正準順: 触媒 → 装備一式 → fgear → fcore (rtui スロットと対応)
        b.inputItems(catalyst, catalystCount);
        // Fantasy 装備は new ItemStack 時点で初期 NBT({Damage:0}) を持つため、inputItems(Item) 経由だと
        // SizedIngredient が NBTIngredient(forge:nbt) 厳密一致に化け、実物の装備(ダメージ有り/別 NBT)と
        // 一切マッチしなくなる。Ingredient.of でアイテム単位一致に固定する (credit の KubeJS と同じ挙動)。
        for (net.minecraft.world.item.Item piece : from)
            b.inputItems(net.minecraft.world.item.crafting.Ingredient.of(piece));
        b.inputItems(ModItems.FGEAR.get(), fgearCount);
        b.inputItems(ModItems.FCORE.get(), 1);
        b.circuitMeta(circuit);
        for (net.minecraft.world.item.Item piece : to) b.outputItems(piece, 1);
        b.duration(200).EUt(480).save(provider);
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

        // Fantasy Builder (fantasy_armor_build): トーテミックゴールドインゴット3 → fcore
        net.minecraft.world.item.Item totemicGold = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("l2complements", "totemic_gold_ingot"));
        if (totemicGold != null) {
            ModRecipeTypes.FANTASY_ARMOR_BUILD.recipeBuilder(new ResourceLocation("gtcsolo", "fcore_from_totemic_gold"))
                    .inputItems(totemicGold, 3)
                    .outputItems(ModItems.FCORE.get(), 1)
                    .duration(200)
                    .EUt(GTValues.VA[GTValues.HV])
                    .save(provider);
        }

        // Fantasy Builder: 防具アップグレード・ツリー (credit /craftpattern → Java 移植、 入力正準順 + circuit 分岐)
        addFantasyArmorBuildRecipes(provider);

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

        // =====================================================================
        //  WEN ハッチ系のクラフト + 組立機レシピ (LV〜OpV、UIV/MAX はsuper-conductor不在でskip)
        // =====================================================================
        addWENHatchRecipes(provider);

        // =====================================================================
        //  WEN ハッチ tier4以降アップグレードレシピ (全電圧、前tier×4 + ケーシング)
        // =====================================================================
        addWENHatchUpgradeRecipes(provider);

        // =====================================================================
        //  WEN Integration 易化レシピ (素材消費75%)
        //  tier1-3: wire×3 + 中央hatch + casing
        //  tier4+: 前tier hatch×3 + casing
        // =====================================================================
        addWENIntegrationRecipes(provider);

        // =====================================================================
        //  WEN Nexus Assembler — WEN関連レシピ
        // =====================================================================
        addWENNexusAssemblerRecipes(provider);
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

    /**
     * WENハッチ4種(wireless_input/output, energy_hatch, energy_output_hatch)の
     * クラフト + 組立機レシピを各電圧×amp(1/4/16)で生成。
     * UIV/MAXは超電導素材不在でskip。
     * クラフト配置: 1278=超電導wire, 4=レンチ('w'記号), 5=同電圧の機能ハッチ, 6=マシンケーシング
     * 組立機: 同素材だがレンチ無し、duration=20ticks(1秒)、EUt=各電圧
     */
    private void addWENHatchRecipes(Consumer<FinishedRecipe> provider) {
        java.util.Map<Integer, Material> wires = new java.util.LinkedHashMap<>();
        wires.put(GTValues.LV, ModMaterials.TARITON);
        wires.put(GTValues.MV, ModMaterials.REFINED_GLOWSTONE);
        wires.put(GTValues.HV, ModMaterials.INFUSED_STAINLESS_STEEL);
        wires.put(GTValues.EV, ModMaterials.REFINED_OBSIDIAN);
        wires.put(GTValues.IV, ModMaterials.OBLIVION);
        wires.put(GTValues.LuV, ModMaterials.HSSX);
        wires.put(GTValues.ZPM, ModMaterials.PURE_NAQUADAH);
        wires.put(GTValues.UV, ModMaterials.ORIGINALIUM);
        wires.put(GTValues.UHV, GTMaterials.RutheniumTriniumAmericiumNeutronate);
        wires.put(GTValues.UEV, ModMaterials.JUPITATE);
        wires.put(GTValues.UXV, ModMaterials.HYPERX_NEUTRONIUM);
        wires.put(GTValues.OpV, ModMaterials.FRACTALINE);

        java.util.Map<Integer, com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block>> casings =
                new java.util.HashMap<>();
        casings.put(GTValues.LV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_LV);
        casings.put(GTValues.MV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_MV);
        casings.put(GTValues.HV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_HV);
        casings.put(GTValues.EV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_EV);
        casings.put(GTValues.IV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_IV);
        casings.put(GTValues.LuV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_LuV);
        casings.put(GTValues.ZPM, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_ZPM);
        casings.put(GTValues.UV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UV);
        casings.put(GTValues.UHV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UHV);
        casings.put(GTValues.UEV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UEV);
        casings.put(GTValues.UXV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UXV);
        casings.put(GTValues.OpV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_OpV);

        java.util.Map<Integer, TagPrefix> wireSizes = new java.util.HashMap<>();
        wireSizes.put(1, TagPrefix.wireGtSingle);
        wireSizes.put(4, TagPrefix.wireGtQuadruple);
        wireSizes.put(16, TagPrefix.wireGtHex);

        for (var e : wires.entrySet()) {
            int tier = e.getKey();
            Material wire = e.getValue();
            var casingBlock = casings.get(tier);
            if (casingBlock == null) continue;

            for (int amp : new int[]{1, 4, 16}) {
                TagPrefix wireSize = wireSizes.get(amp);
                String tierName = GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT);

                generateWENRecipePair(provider, "wireless_input", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.WIRELESS_INPUT.get(tier).get(amp),
                        com.gregtechceu.gtceu.common.data.GTMachines.FLUID_IMPORT_HATCH[tier]);
                generateWENRecipePair(provider, "wireless_output", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.WIRELESS_OUTPUT.get(tier).get(amp),
                        com.gregtechceu.gtceu.common.data.GTMachines.FLUID_EXPORT_HATCH[tier]);
                generateWENRecipePair(provider, "energy_hatch", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.ENERGY_HATCH.get(tier).get(amp),
                        gtEnergyHatchByAmp(amp, true)[tier]);
                generateWENRecipePair(provider, "energy_output_hatch", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.ENERGY_OUTPUT_HATCH.get(tier).get(amp),
                        gtEnergyHatchByAmp(amp, false)[tier]);
            }
        }
    }

    private static com.gregtechceu.gtceu.api.machine.MachineDefinition[] gtEnergyHatchByAmp(int amp, boolean input) {
        if (input) {
            return amp == 1 ? com.gregtechceu.gtceu.common.data.GTMachines.ENERGY_INPUT_HATCH
                 : amp == 4 ? com.gregtechceu.gtceu.common.data.GTMachines.ENERGY_INPUT_HATCH_4A
                 : com.gregtechceu.gtceu.common.data.GTMachines.ENERGY_INPUT_HATCH_16A;
        } else {
            return amp == 1 ? com.gregtechceu.gtceu.common.data.GTMachines.ENERGY_OUTPUT_HATCH
                 : amp == 4 ? com.gregtechceu.gtceu.common.data.GTMachines.ENERGY_OUTPUT_HATCH_4A
                 : com.gregtechceu.gtceu.common.data.GTMachines.ENERGY_OUTPUT_HATCH_16A;
        }
    }

    private void generateWENRecipePair(Consumer<FinishedRecipe> provider,
            String typeKey, String tierName, int tier, int amp,
            Material wire, TagPrefix wireSize,
            com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block> casing,
            com.gregtechceu.gtceu.api.machine.MachineDefinition wenHatch,
            com.gregtechceu.gtceu.api.machine.MachineDefinition centerHatch) {
        if (wenHatch == null || centerHatch == null) return;

        String baseId = "wen_" + typeKey + "_" + tierName + "_" + amp + "a";

        com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper.addShapedRecipe(provider,
                new ResourceLocation("gtcsolo", "shaped/" + baseId),
                wenHatch.asStack(),
                "WW ", "wHC", "WW ",
                'W', new com.gregtechceu.gtceu.api.data.chemical.material.stack.UnificationEntry(wireSize, wire),
                'H', centerHatch.asStack(),
                'C', casing.asStack());

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(
                        new ResourceLocation("gtcsolo", "assembler/" + baseId))
                .inputItems(wireSize, wire, 4)
                .inputItems(centerHatch.asStack())
                .inputItems(casing.asStack())
                .outputItems(wenHatch.asStack())
                .duration(20)
                .EUt(GTValues.VA[tier])
                .save(provider);
    }

    /**
     * WENハッチ tier4以降のアップグレードレシピ (全14電圧)。
     * 1278: 同電圧・前tier(amp1段階下)のWENハッチ × 4
     * 5: レンチ('w'記号、耐久-1)
     * 6: 電圧に応じたマシンケーシング
     * 組立機レシピも併設、duration=20ticks、EUt=各電圧。
     */
    private void addWENHatchUpgradeRecipes(Consumer<FinishedRecipe> provider) {
        java.util.Map<Integer, com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block>> casings =
                buildAllMachineCasings();

        java.util.Map<String, int[]> ampSequences = new java.util.LinkedHashMap<>();
        ampSequences.put("wireless_input", new int[]{1, 4, 16, 64, 256});
        ampSequences.put("wireless_output", new int[]{1, 4, 16, 64, 256});
        ampSequences.put("energy_hatch", new int[]{1, 4, 16, 64, 256, 1024, 4096, 16384});
        ampSequences.put("energy_output_hatch", new int[]{1, 4, 16, 64, 256, 1024, 2048});

        int[] allTiers = {GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV,
                          GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV,
                          GTValues.UEV, GTValues.UIV, GTValues.UXV, GTValues.OpV, GTValues.MAX};

        for (int tier : allTiers) {
            var casingBlock = casings.get(tier);
            if (casingBlock == null) continue;
            String tierName = GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT);

            for (var e : ampSequences.entrySet()) {
                String typeKey = e.getKey();
                int[] amps = e.getValue();
                for (int i = 3; i < amps.length; i++) {
                    int currAmp = amps[i];
                    int prevAmp = amps[i - 1];
                    var curr = getWENMachine(typeKey, tier, currAmp);
                    var prev = getWENMachine(typeKey, tier, prevAmp);
                    if (curr == null || prev == null) continue;

                    generateWENUpgradeRecipePair(provider, typeKey, tierName, tier, currAmp,
                            prev, casingBlock, curr);
                }
            }
        }
    }

    private static java.util.Map<Integer, com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block>> buildAllMachineCasings() {
        java.util.Map<Integer, com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block>> m =
                new java.util.HashMap<>();
        m.put(GTValues.LV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_LV);
        m.put(GTValues.MV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_MV);
        m.put(GTValues.HV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_HV);
        m.put(GTValues.EV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_EV);
        m.put(GTValues.IV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_IV);
        m.put(GTValues.LuV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_LuV);
        m.put(GTValues.ZPM, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_ZPM);
        m.put(GTValues.UV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UV);
        m.put(GTValues.UHV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UHV);
        m.put(GTValues.UEV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UEV);
        m.put(GTValues.UIV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UIV);
        m.put(GTValues.UXV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_UXV);
        m.put(GTValues.OpV, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_OpV);
        m.put(GTValues.MAX, com.gregtechceu.gtceu.common.data.GTBlocks.MACHINE_CASING_MAX);
        return m;
    }

    private static com.gregtechceu.gtceu.api.machine.MachineDefinition getWENMachine(String typeKey, int tier, int amp) {
        java.util.Map<Integer, com.gregtechceu.gtceu.api.machine.MachineDefinition> tierMap = switch (typeKey) {
            case "wireless_input" -> DIV.gtcsolo.registry.WENMachines.WIRELESS_INPUT.get(tier);
            case "wireless_output" -> DIV.gtcsolo.registry.WENMachines.WIRELESS_OUTPUT.get(tier);
            case "energy_hatch" -> DIV.gtcsolo.registry.WENMachines.ENERGY_HATCH.get(tier);
            case "energy_output_hatch" -> DIV.gtcsolo.registry.WENMachines.ENERGY_OUTPUT_HATCH.get(tier);
            default -> null;
        };
        return tierMap == null ? null : tierMap.get(amp);
    }

    private void generateWENUpgradeRecipePair(Consumer<FinishedRecipe> provider,
            String typeKey, String tierName, int tier, int amp,
            com.gregtechceu.gtceu.api.machine.MachineDefinition prev,
            com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block> casing,
            com.gregtechceu.gtceu.api.machine.MachineDefinition curr) {
        String baseId = "wen_upgrade_" + typeKey + "_" + tierName + "_" + amp + "a";

        com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper.addShapedRecipe(provider,
                new ResourceLocation("gtcsolo", "shaped/" + baseId),
                curr.asStack(),
                "HH ", " wC", "HH ",
                'H', prev.asStack(),
                'C', casing.asStack());

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(
                        new ResourceLocation("gtcsolo", "assembler/" + baseId))
                .inputItems(prev.asStack(4))
                .inputItems(casing.asStack())
                .outputItems(curr.asStack())
                .duration(20)
                .EUt(GTValues.VA[tier])
                .save(provider);
    }

    /**
     * WEN_INTEGRATION 易化レシピ。素材消費75%換算 (wire×4→×3、prev hatch×4→×3)。
     * duration=20ticks、EUt=各電圧、duration/EUt は元の組立機と同じ。
     */
    private void addWENIntegrationRecipes(Consumer<FinishedRecipe> provider) {
        java.util.Map<Integer, Material> wires = new java.util.LinkedHashMap<>();
        wires.put(GTValues.LV, ModMaterials.TARITON);
        wires.put(GTValues.MV, ModMaterials.REFINED_GLOWSTONE);
        wires.put(GTValues.HV, ModMaterials.INFUSED_STAINLESS_STEEL);
        wires.put(GTValues.EV, ModMaterials.REFINED_OBSIDIAN);
        wires.put(GTValues.IV, ModMaterials.OBLIVION);
        wires.put(GTValues.LuV, ModMaterials.HSSX);
        wires.put(GTValues.ZPM, ModMaterials.PURE_NAQUADAH);
        wires.put(GTValues.UV, ModMaterials.ORIGINALIUM);
        wires.put(GTValues.UHV, GTMaterials.RutheniumTriniumAmericiumNeutronate);
        wires.put(GTValues.UEV, ModMaterials.JUPITATE);
        wires.put(GTValues.UXV, ModMaterials.HYPERX_NEUTRONIUM);
        wires.put(GTValues.OpV, ModMaterials.FRACTALINE);

        var allCasings = buildAllMachineCasings();

        java.util.Map<Integer, TagPrefix> wireSizes = new java.util.HashMap<>();
        wireSizes.put(1, TagPrefix.wireGtSingle);
        wireSizes.put(4, TagPrefix.wireGtQuadruple);
        wireSizes.put(16, TagPrefix.wireGtHex);

        // tier1-3: wire-based 易化
        for (var e : wires.entrySet()) {
            int tier = e.getKey();
            Material wire = e.getValue();
            var casingBlock = allCasings.get(tier);
            if (casingBlock == null) continue;
            String tierName = GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT);

            for (int amp : new int[]{1, 4, 16}) {
                TagPrefix wireSize = wireSizes.get(amp);
                registerWENIntegrationTier13(provider, "wireless_input", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.WIRELESS_INPUT.get(tier).get(amp),
                        com.gregtechceu.gtceu.common.data.GTMachines.FLUID_IMPORT_HATCH[tier]);
                registerWENIntegrationTier13(provider, "wireless_output", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.WIRELESS_OUTPUT.get(tier).get(amp),
                        com.gregtechceu.gtceu.common.data.GTMachines.FLUID_EXPORT_HATCH[tier]);
                registerWENIntegrationTier13(provider, "energy_hatch", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.ENERGY_HATCH.get(tier).get(amp),
                        gtEnergyHatchByAmp(amp, true)[tier]);
                registerWENIntegrationTier13(provider, "energy_output_hatch", tierName, tier, amp, wire, wireSize, casingBlock,
                        DIV.gtcsolo.registry.WENMachines.ENERGY_OUTPUT_HATCH.get(tier).get(amp),
                        gtEnergyHatchByAmp(amp, false)[tier]);
            }
        }

        // tier4+: cascade 易化 (前tier hatch×3 + casing)
        java.util.Map<String, int[]> ampSequences = new java.util.LinkedHashMap<>();
        ampSequences.put("wireless_input", new int[]{1, 4, 16, 64, 256});
        ampSequences.put("wireless_output", new int[]{1, 4, 16, 64, 256});
        ampSequences.put("energy_hatch", new int[]{1, 4, 16, 64, 256, 1024, 4096, 16384});
        ampSequences.put("energy_output_hatch", new int[]{1, 4, 16, 64, 256, 1024, 2048});

        int[] allTiers = {GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV,
                          GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV,
                          GTValues.UEV, GTValues.UIV, GTValues.UXV, GTValues.OpV, GTValues.MAX};

        for (int tier : allTiers) {
            var casingBlock = allCasings.get(tier);
            if (casingBlock == null) continue;
            String tierName = GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT);
            for (var entry : ampSequences.entrySet()) {
                String typeKey = entry.getKey();
                int[] amps = entry.getValue();
                for (int i = 3; i < amps.length; i++) {
                    int currAmp = amps[i];
                    int prevAmp = amps[i - 1];
                    var curr = getWENMachine(typeKey, tier, currAmp);
                    var prev = getWENMachine(typeKey, tier, prevAmp);
                    if (curr == null || prev == null) continue;

                    String baseId = "integration_upgrade_" + typeKey + "_" + tierName + "_" + currAmp + "a";
                    ModRecipeTypes.WEN_INTEGRATION.recipeBuilder(
                                    new ResourceLocation("gtcsolo", baseId))
                            .inputItems(prev.asStack(3))
                            .inputItems(casingBlock.asStack())
                            .outputItems(curr.asStack())
                            .duration(20)
                            .EUt(GTValues.VA[tier])
                            .save(provider);
                }
            }
        }
    }

    private void registerWENIntegrationTier13(Consumer<FinishedRecipe> provider,
            String typeKey, String tierName, int tier, int amp,
            Material wire, TagPrefix wireSize,
            com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block> casing,
            com.gregtechceu.gtceu.api.machine.MachineDefinition wenHatch,
            com.gregtechceu.gtceu.api.machine.MachineDefinition centerHatch) {
        if (wenHatch == null || centerHatch == null) return;
        String baseId = "integration_" + typeKey + "_" + tierName + "_" + amp + "a";
        ModRecipeTypes.WEN_INTEGRATION.recipeBuilder(
                        new ResourceLocation("gtcsolo", baseId))
                .inputItems(wireSize, wire, 3)
                .inputItems(centerHatch.asStack())
                .inputItems(casing.asStack())
                .outputItems(wenHatch.asStack())
                .duration(20)
                .EUt(GTValues.VA[tier])
                .save(provider);
    }

    /**
     * WEN Nexus Assembler レシピ群。WEN関連の機材をここで作る。
     */
    private void addWENNexusAssemblerRecipes(Consumer<FinishedRecipe> provider) {
        // WEN基本蓄電セル: 石炭ブロック×4 + LV小型リチウム電池 → WEN_BASIC_ENERGY_CELL, MV 2A, 120ticks
        ModRecipeTypes.WEN_NEXUS_ASSEMBLER.recipeBuilder(
                        new ResourceLocation("gtcsolo", "wen_basic_energy_cell"))
                .inputItems(new net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.COAL_BLOCK, 4))
                .inputItems(com.gregtechceu.gtceu.common.data.GTItems.BATTERY_LV_LITHIUM.asStack())
                .outputItems(new net.minecraft.world.item.ItemStack(
                        DIV.gtcsolo.registry.ModBlocks.WEN_BASIC_ENERGY_CELL.get(), 1))
                .duration(120)
                .EUt(GTValues.VA[GTValues.MV] * 2L)
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