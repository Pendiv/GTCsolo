ServerEvents.recipes(event => {
    if (!GTRecipeTypes.get('gtceu:ma_irradiation') ||
        !Fluid.exists('gtcsolo:ma_fluid') || !Fluid.exists('gtcsolo:dragon_breath')) return;

    const uhv = GTValues.VA[GTValues.UHV];
    if (Fluid.exists('gtcsolo:dma_fluid') && Item.exists('gtcsolo:ma_matter_crystal') &&
        Item.exists('gtcsolo:purified_chi_lens')) {
        event.recipes.gtceu.ma_irradiation('gtcsolo:dma_fluid')
            .itemInputs('gtcsolo:ma_matter_crystal')
            .notConsumable('gtcsolo:purified_chi_lens')
            .inputFluids(Fluid.of('gtcsolo:ma_fluid', 100), Fluid.of('gtcsolo:dragon_breath', 250),
                Fluid.of('gtceu:argon', 100), Fluid.of('gtceu:helium', 100))
            .outputFluids(Fluid.of('gtcsolo:dma_fluid', 250))
            .duration(200).EUt(uhv);
    }

    if (Item.exists('draconicevolution:dragon_heart') && Item.exists('gtceu:quantum_star') &&
        Item.exists('gtcsolo:draconic_star')) {
        event.recipes.gtceu.ma_irradiation('gtcsolo:draconic_star')
            .itemInputs('draconicevolution:dragon_heart', 'gtceu:quantum_star')
            .inputFluids(Fluid.of('gtcsolo:ma_fluid', 100), Fluid.of('gtcsolo:dragon_breath', 250))
            .itemOutputs('gtcsolo:draconic_star')
            .duration(600).EUt(uhv);
    }
});
