ServerEvents.recipes(event => {
    // 抽出器 (Extractor)
    event.recipes.gtceu.extractor('polonium_to_poronium')
        .itemInputs('mekanism:pellet_polonium')
        .outputFluids(Fluid.of('gtceu:poronium', 1000))
        .duration(200)
        .EUt(30);
})