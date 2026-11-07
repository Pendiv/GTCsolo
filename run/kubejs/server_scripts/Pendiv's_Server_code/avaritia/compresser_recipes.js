// SINGULARITY_COMPRESSER の自動レシピ生成
// AvaritiaEvents.singularity は injectRuntimeRecipes の最後に発火するため、
// ServerEvents.recipes 時点では我々の singularity 群は未登録。
// よって singularities.js のリストをここでミラー (ハードコード)。
// 例外:
//   - gtcsolo:singularity_singularity (二重 singularity、レシピなし)
//   - gtcsolo:singularity_nether_star : tier UV、count 2000、EUt V[UV]
//   - eternal_singularity は別レシピ型 (EternalSingularityCraftRecipe) なので素通り
// avaritia 既定 (~27) も同様に signature 圧縮レシピ生成 (getAllSingularities() 経由)

ServerEvents.recipes(event => {

    const ZPM = 131072;    // V[ZPM]
    const UV  = 524288;    // V[UV]
    const UHV = 2097152;   // V[UHV]

    // 既存 avaritia auto-compressor を削除 (recipeEnabled=false が間に合わない場合の保険)
    // Type is checked before output; do not resolve unrelated Cucumber tag recipes.
    event.remove({ type: 'avaritia:compressor', output: 'avaritia:singularity' });

    // ============ tier オーバーライド表 (itemId → {count, eut}) ============
    // 未登録は default (count=1000, eut=ZPM)
    const OVERRIDES = {
        // UV / 2000 (nether_star に倣う)
        'minecraft:nether_star':            { count: 2000, eut: UV },
        // recipe_75941 群 (UV / 2000)
        'mekanism:teleportation_core':      { count: 2000, eut: UV },
        'ae2:fluix_crystal':                { count: 2000, eut: UV },
        'gtceu:neutronium_dust':            { count: 2000, eut: UV },
        'gtcsolo:starlight_ingot':          { count: 2000, eut: UV },
        'gtcsolo:nebulite_dust':            { count: 2000, eut: UV },
        'gtcsolo:oblivion_dust':            { count: 2000, eut: UV },
        'gtcsolo:enigma_dust':              { count: 2000, eut: UV },
        // recipe_48058 群 (UHV / 300)
        'gtcsolo:bedrockium_dust':          { count: 300, eut: UHV },
        'gtcsolo:antimatter_dust':          { count: 300, eut: UHV },
        'gtcsolo:aurum_stellis_gold_ingot': { count: 300, eut: UHV },
        'gtcsolo:jupitate_dust':            { count: 300, eut: UHV },
        'gtcsolo:stasis_dust':              { count: 300, eut: UHV },
        'gtcsolo:fractal_dust':             { count: 300, eut: UHV },
    };
    function getOverride(itemId) {
        return OVERRIDES[itemId] || { count: 1000, eut: ZPM };
    }

    // [itemId, singularityId, count, eut] 直接定義
    function reg(itemId, singularityId, count, eut) {
        const recipeId = 'gtcsolo:autogen_singularity_' + singularityId.replace(/[^a-z0-9_]/g, '_');
        event.recipes.gtcsolo.singularity_compresser(recipeId)
            .itemInputs(count + 'x ' + itemId)
            .itemOutputs(Item.of('avaritia:singularity', '{Id:"' + singularityId + '"}'))
            .duration(240)
            .EUt(eut);
    }

    // GT material (suffix _dust/_ingot 剥ぎで simpleName を導く)
    function regMat(itemId, materialId, savedName) {
        const matName = materialId || itemId.replace(/_(dust|ingot|pellet)$/, '');
        const simpleName = savedName || matName.replace(/^[a-z0-9_]+:/, '');
        const sid = 'gtcsolo:singularity_' + simpleName;
        const o = getOverride(itemId);
        reg(itemId, sid, o.count, o.eut);
    }

    // 非 material (suffix そのまま)
    function regManual(itemId) {
        const simpleName = itemId.replace(/^[a-z0-9_]+:/, '');
        const sid = 'gtcsolo:singularity_' + simpleName;
        const o = getOverride(itemId);
        reg(itemId, sid, o.count, o.eut);
    }

    // ============ GTCEu materials (41) ============
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

    // ============ gtcsolo materials (37) ============
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

    // ============ Mekanism (5) ============
    regManual('mekanism:hdpe_pellet');
    regManual('mekanism:pellet_plutonium');
    regManual('mekanism:pellet_polonium');
    regManual('mekanism:reprocessed_fissile_fragment');
    regManual('mekanism:teleportation_core');

    // ============ Minecraft (4) ============
    regManual('minecraft:dragon_egg');
    regManual('minecraft:ender_pearl');
    regManual('minecraft:heart_of_the_sea');
    regManual('minecraft:nether_star');

    // ============ AE2 (3) ============
    regManual('ae2:charged_certus_quartz_crystal');
    regManual('ae2:fluix_crystal');
    // ae2:singularity → gtcsolo:singularity_singularity (仮レシピ、credit 対応後に差し替え予定)
    // simpleName='singularity' なので registry id = gtcsolo:singularity_singularity になる
    regManual('ae2:singularity');

    // ============ avaritia 既定 (~27) ============
    // 上記イベント時点で SingularityReloadListener には avaritia 既定のみ登録済み
    const SingularityReloadListener = Java.loadClass(
            'committee.nova.mods.avaritia.core.singularity.SingularityReloadListener');
    const all = SingularityReloadListener.INSTANCE.getAllSingularities();

    let avaritiaCount = 0;
    all.forEach(function (id, sing) {
        // 我々の gtcsolo: 系は↑で既に追加済 (実際この時点では未登録だが念のため)
        if (id.getNamespace().equals('gtcsolo')) return;
        if (sing.getIngredient().isEmpty()) return;
        // Preserve the complete native ingredient (including alternatives and NBT).
        const input = Ingredient.of(sing.getIngredient(), sing.getCount());
        const recipeId = 'gtcsolo:autogen_singularity_avaritia__' + id.getPath();
        event.recipes.gtcsolo.singularity_compresser(recipeId)
            .itemInputs(input)
            .itemOutputs(Item.of('avaritia:singularity', '{Id:"' + id.toString() + '"}'))
            .duration(240)
            .EUt(ZPM);
        avaritiaCount++;
    });

    console.info('[SingularityCompresser] generated 88 (gtcsolo) + ' + avaritiaCount + ' (avaritia 既定) compresser recipes');
});
