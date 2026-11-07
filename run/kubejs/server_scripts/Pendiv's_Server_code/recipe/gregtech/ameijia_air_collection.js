// Continue the native gas collector's air / nether / end progression.
ServerEvents.recipes(event => {
    if (!Fluid.exists('gtceu:ameijia_air')) {
        console.warn('[Ameijia air] The registered fluid is unavailable; restart after registering the material.');
        return;
    }
    event.recipes.gtceu.gas_collector('gtcsolo:ameijia_air')
        .circuit(4)
        .outputFluids(Fluid.of('gtceu:ameijia_air', 10000))
        .recipeConditions([])
        .dimension('gtcsolo:ameijia')
        .duration(200).EUt(1024);
});
