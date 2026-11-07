// Spaceforge レシピで使用される全 itemInputs を Singularity 化 (~90 個)。
// - GT/gtcsolo material 系: GTMaterials から色を自動取得 (underlay=原色 / overlay=明)
// - 非 material 系: 手動カラーペア指定
// 命名規約: gtcsolo:singularity_<simplename>  (avaritia:aluminum 形式に prefix を加えた版)
// count=1000 / timeCost=240 (avaritia 標準値) 一律

// Re-Avaritia 未導入なら何もしない (typeof チェックで Rhino エラー回避)
if (typeof AvaritiaEvents === 'undefined') {
    console.warn('[Singularity] Re-Avaritia (avaritia) MOD が読み込まれていません。singularities.js は素通りします。');
} else {
AvaritiaEvents.singularity(event => {

    // 各 RGB チャネル ×1.4 で clamp して overlay 用に明るくする
    function brighten(rgb) {
        let r = (rgb >> 16) & 0xFF;
        let g = (rgb >> 8) & 0xFF;
        let b = rgb & 0xFF;
        r = Math.min(255, Math.round(r * 1.4));
        g = Math.min(255, Math.round(g * 1.4));
        b = Math.min(255, Math.round(b * 1.4));
        return (r << 16) | (g << 8) | b;
    }

    // namespace と _dust/_ingot/_pellet を剥がして simpleName を得る
    function stripSuffix(itemId) {
        return itemId.replace(/_(dust|ingot|pellet)$/, '');
    }
    function simpleNameOf(itemId) {
        return stripSuffix(itemId).replace(/^[a-z0-9_]+:/, '');
    }
    function titleCase(s) {
        return s.split('_').map(function (p) {
            return p.charAt(0).toUpperCase() + p.substring(1);
        }).join(' ');
    }

    // GT material 駆動: 色は GTMaterials.get(matName).getMaterialRGB() で取得
    function regMat(itemId, materialId, savedName) {
        const matName = materialId || stripSuffix(itemId);
        const mat = GTMaterials.get(matName);
        if (mat == null) {
            console.warn('[Singularity] material not found: ' + matName + ' (skip ' + itemId + ')');
            return;
        }
        const c = mat.getMaterialRGB() & 0xFFFFFF;
        const simpleName = savedName || simpleNameOf(matName);
        event.register('gtcsolo:singularity_' + simpleName, function (s) {
            s.setColors(brighten(c), c);
            s.setIngredient(Ingredient.of(itemId));
            s.setDisplayName(titleCase(simpleNameOf(matName)));
            s.setCount(1000);
            s.setTimeCost(240);
        });
    }

    // 非 material 系: 色 (overlay, underlay) を手動指定
    function regManual(itemId, overlay, underlay, displayName) {
        const simpleName = itemId.replace(/^[a-z0-9_]+:/, '');
        event.register('gtcsolo:singularity_' + simpleName, function (s) {
            s.setColors(overlay & 0xFFFFFF, underlay & 0xFFFFFF);
            s.setIngredient(Ingredient.of(itemId));
            s.setDisplayName(displayName);
            s.setCount(1000);
            s.setTimeCost(240);
        });
    }

    // ============ GTCEu material 系 (41 個) ============
    regMat('gtceu:aluminium_dust');
    regMat('gtceu:antimony_dust');
    regMat('gtceu:arsenic_dust');
    regMat('gtcsolo:aurum_stellis_gold_ingot');
    regMat('gtceu:barium_dust');
    regMat('gtceu:bismuth_dust');
    regMat('gtceu:blue_alloy_ingot');
    regMat('gtceu:boron_dust');
    regMat('gtceu:bronze_dust');
    regMat('gtceu:chromium_dust');
    regMat('gtceu:cinnabar_dust');
    regMat('gtceu:cobalt_dust');
    regMat('gtceu:copper_dust');
    regMat('gtceu:darmstadtium_dust');
    regMat('gtceu:duranium_dust');
    regMat('gtceu:europium_dust');
    regMat('gtceu:gallium_dust');
    regMat('gtceu:gold_dust');
    regMat('gtceu:indium_dust');
    regMat('gtceu:molybdenum_dust');
    regMat('gtceu:naquadah_dust');
    regMat('gtceu:neodymium_dust');
    regMat('gtceu:neutronium_dust');
    regMat('gtceu:nickel_dust');
    regMat('gtceu:niobium_dust');
    regMat('gtceu:osmium_dust');
    regMat('gtceu:palladium_dust');
    regMat('gtceu:phosphorus_dust');
    regMat('gtceu:platinum_dust');
    regMat('gtceu:plutonium_dust');
    regMat('gtceu:potassium_dust');
    regMat('gtceu:red_alloy_ingot');
    regMat('gtceu:rhodium_dust');
    regMat('gtceu:ruthenium_dust');
    regMat('gtceu:samarium_dust');
    regMat('gtceu:silver_dust');
    regMat('gtcsolo:starlight_ingot');
    regMat('gtceu:sulfur_dust');
    regMat('gtceu:tantalum_dust');
    regMat('gtceu:trinium_dust');
    regMat('gtceu:tritanium_dust');

    // ============ gtcsolo material 系 (37 個) ============
    regMat('gtcsolo:adamantite_dust');
    regMat('gtcsolo:antimatter_dust');
    regMat('gtcsolo:auroralium_dust');
    regMat('gtcsolo:axiom_steel_dust');
    regMat('gtcsolo:bedrockium_dust');
    regMat('gtcsolo:better_gold_dust');
    regMat('gtcsolo:dilithium_dust');
    regMat('gtcsolo:enigma_dust');
    regMat('gtcsolo:etherium_dust');
    regMat('gtcsolo:exporonium_dust');
    regMat('gtcsolo:flaurite_dust');
    regMat('gtcsolo:fractal_dust');
    regMat('gtceu:hafnium_dust', 'gtceu:hafnium', 'hafhnium');
    regMat('gtcsolo:harmonium_dust');
    regMat('gtcsolo:infused_stainless_steel_dust');
    regMat('gtcsolo:jupitate_dust');
    regMat('gtcsolo:mialineum_dust');
    regMat('gtcsolo:mithril_dust');
    regMat('gtcsolo:nebulite_dust');
    regMat('gtcsolo:nethera_mialineum_dust');
    regMat('gtcsolo:nocturnium_dust');
    regMat('gtcsolo:oblivion_dust');
    regMat('gtcsolo:orichalcum_dust');
    regMat('gtcsolo:originalium_dust');
    regMat('gtcsolo:pluslitherite_dust');
    regMat('gtcsolo:refined_glowstone_dust');
    regMat('mekanism:dust_refined_obsidian', 'gtcsolo:refined_obsidian');
    regMat('gtcsolo:skystone_dust');
    regMat('gtcsolo:stasis_dust');
    regMat('gtcsolo:stellarium_dust');
    regMat('gtcsolo:tariton_dust');
    regMat('gtcsolo:urumetal_dust');
    regMat('gtcsolo:valinium_dust');
    regMat('gtcsolo:velzenium_dust');
    regMat('gtcsolo:vesker_dust');
    regMat('gtcsolo:viliria_steel_dust');

    // ============ Mekanism (5 個 - 手動色) ============
    // hdpe_pellet: 黒 (HDPE プラスチック)
    regManual('mekanism:hdpe_pellet', brighten(0x303030), 0x303030, 'HDPE Pellet');
    // pellet_plutonium: 緑 (放射性緑)
    regManual('mekanism:pellet_plutonium', brighten(0x44ff44), 0x44ff44, 'Plutonium Pellet');
    // pellet_polonium: 黄橙 (放射性黄)
    regManual('mekanism:pellet_polonium', brighten(0xff9500), 0xff9500, 'Polonium Pellet');
    // reprocessed_fissile_fragment: 暗緑 (使用済み燃料)
    regManual('mekanism:reprocessed_fissile_fragment', brighten(0x556644), 0x556644, 'Reprocessed Fissile Fragment');
    // teleportation_core: シアン (テレポートコア青)
    regManual('mekanism:teleportation_core', brighten(0x4ee5ff), 0x4ee5ff, 'Teleportation Core');

    // ============ Minecraft (4 個 - 手動色) ============
    // dragon_egg: 黒紫
    regManual('minecraft:dragon_egg', brighten(0x110018), 0x110018, 'Dragon Egg');
    // ender_pearl: シアングリーン
    regManual('minecraft:ender_pearl', brighten(0x32d49d), 0x32d49d, 'Ender Pearl');
    // heart_of_the_sea: 淡シアン
    regManual('minecraft:heart_of_the_sea', brighten(0x66cccc), 0x66cccc, 'Heart of the Sea');
    // nether_star: ほぼ白
    regManual('minecraft:nether_star', brighten(0xfafaef), 0xfafaef, 'Nether Star');

    // ============ AE2 (3 個 - 手動色) ============
    // charged_certus_quartz: 淡い水色
    regManual('ae2:charged_certus_quartz_crystal', brighten(0xc4dffe), 0xc4dffe, 'Charged Certus Quartz Crystal');
    // fluix_crystal: 紫
    regManual('ae2:fluix_crystal', brighten(0xa56cd2), 0xa56cd2, 'Fluix Crystal');
    // ae2:singularity → singularity_singularity (ユーザー要望、二重 Singularity)
    regManual('ae2:singularity', brighten(0x101010), 0x101010, 'Singularity');

    // ============ Top-tier (maker 専用、compresser/avaritia 経由で作成不可) ============
    // setRecipeEnabled(false) で auto-recipe 抑制。
    // ingredient は dummy (minecraft:barrier) を入れる:
    //   理由: HaloBakedModel.renderItem (Shift 押下時) が ingredient.getItems()[0]
    //         に直接アクセスするため、EMPTY だと AIOOBE クラッシュ。
    function regTopTier(simpleName, overlay, underlay, displayName) {
        event.register('gtcsolo:singularity_' + simpleName, function (s) {
            s.setColors(overlay & 0xFFFFFF, underlay & 0xFFFFFF);
            s.setDisplayName(displayName);
            s.setIngredient(Ingredient.of('minecraft:barrier'));
            s.setRecipeEnabled(false);
        });
    }
    // Retire the unused placeholder routes; Fantasia now feeds the ATG reconstruction route.
    event.remove('gtcsolo:singularity_extend');
    event.remove('gtcsolo:singularity_void');
    regTopTier('fantasia', 0xFFD700, 0x2244BB, 'Fantasia');

    // 全 singularity の avaritia auto-compressor recipe を無効化
    // (singularity_compresser レシピタイプで肩代わりするため)
    // eternal_singularity は別レシピタイプ (EternalSingularityCraftRecipe) なので影響なし
    const SingularityReloadListener = Java.loadClass(
            'committee.nova.mods.avaritia.core.singularity.SingularityReloadListener');
    SingularityReloadListener.INSTANCE.getAllSingularities().forEach(function (id, sing) {
        event.removeRecipe(id);
    });
});
}
