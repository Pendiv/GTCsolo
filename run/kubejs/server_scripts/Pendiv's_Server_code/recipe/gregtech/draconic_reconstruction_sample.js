ServerEvents.recipes(event => {
    if (!GTRecipeTypes.get('gtceu:draconic_reconstruction') ||
        !Fluid.exists('gtceu:liquid_ender_air') || !Fluid.exists('gtceu:xenon') ||
        !Fluid.exists('gtceu:tritium') || !Fluid.exists('gtcsolo:dragon_breath')) return;

    event.recipes.gtceu.draconic_reconstruction('gtcsolo:dragon_breath_synthesis')
        .inputFluids(Fluid.of('gtceu:liquid_ender_air', 1000), Fluid.of('gtceu:xenon', 125),
            Fluid.of('gtceu:tritium', 125))
        .outputFluids(Fluid.of('gtcsolo:dragon_breath', 250))
        .duration(600).EUt(GTValues.VA[GTValues.UHV]);
});
