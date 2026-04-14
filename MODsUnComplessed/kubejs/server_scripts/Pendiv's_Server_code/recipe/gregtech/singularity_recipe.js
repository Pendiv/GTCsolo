ServerEvents.recipes(event => {
    const UHV = GTValues.V[GTValues.UHV];

    // ==========================================================
    // 【レシピ管理リスト】
    // 形式: [素材ID, 出力倍率(dustの数)]
    // ==========================================================
    const SINGULARITY_RECIPES = [
        ['iron', 1],
        ['tritanium', 4],
        ['bismuth', 2],
        ['naquadah', 16],
        ['silver', 2],
        ['tungsten', 8],
        ['osmium', 12],
        ['samarium', 10]
    ];

    SINGULARITY_RECIPES.forEach(([mat, multiplier]) => {
        // 1. 重力井戸 (Gravity Well) でのシンギュラリティ化レシピ
        // 液体2,880,000mb (2000ブロック分) -> シンギュラリティの粉 1個
        event.recipes.gtceu.gravity_well(`${mat}_to_singularity`)
            .inputFluids(`gtceu:${mat} 2880000`)
            .itemOutputs(`gtceu:singularity_${mat}_dust`)
            .duration(3600)
            .EUt(UHV);

        // 2. GTNEH (singularity_gen) でのシンギュラリティ塵への分解レシピ
        // シンギュラリティ液体 288mb -> 汎用シンギュラリティ塵 (指定した倍率分)
        event.recipes.gtceu.gtneh(`gen_${mat}_singularity`)
            .inputFluids(`gtceu:singularity_${mat} 288`) 
            .itemOutputs(`${multiplier}x gtceu:singularity_dust`)
            .duration(2400)
            .EUt(UHV);
    });
});