// SINGULARITY_MAKER の仮レシピ (JEI 編集用の枠確保)
// credit が singularity 対応するまでの一時実装。
// 16 スロット × 64x ae2:singularity = 1024x → singularity_singularity

ServerEvents.recipes(event => {

    const ZPM = 131072;    // V[ZPM]

    event.recipes.gtcsolo.singularity_maker('gtcsolo:autogen_maker_singularity_singularity')
        .itemInputs(
            '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity',
            '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity',
            '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity',
            '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity', '64x ae2:singularity'
        )
        .itemOutputs(Item.of('avaritia:singularity', '{Id:"gtcsolo:singularity_singularity"}'))
        .duration(600)
        .EUt(ZPM);

    console.info('[SingularityMaker] generated 1 (singularity_singularity) maker recipe');
});
