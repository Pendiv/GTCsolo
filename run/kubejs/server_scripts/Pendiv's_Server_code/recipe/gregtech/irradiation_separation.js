ServerEvents.recipes(event => {
    if (!GTRecipeTypes.get('gtcsolo:irradiation_separation')) return;

    const separate = name => event.recipes.gtcsolo.irradiation_separation('gtcsolo:' + name);
    const iv = GTValues.VA[GTValues.IV];
    const zpm = GTValues.VA[GTValues.ZPM];

    // Nine centrifuge batches yield one full dust of zircon.
    separate('zircon_from_sand')
        .itemInputs('576x #minecraft:sand')
        .notConsumable('#forge:lenses/purple')
        .itemOutputs('64x gtceu:silicon_dioxide_dust', '8x gtceu:silicon_dioxide_dust', 'gtcsolo:zircon_dust')
        .duration(480).EUt(iv);

    separate('zinc_70_enrichment')
        .notConsumable('#forge:lenses/blue')
        .inputFluids(Fluid.of('gtcsolo:zinc_70_enriched_diethylzinc', 4700))
        .outputFluids(Fluid.of('gtcsolo:zinc_70_high_enriched_diethylzinc', 1000),
            Fluid.of('gtcsolo:zinc_70_low_enriched_diethylzinc', 3700))
        .duration(2400).EUt(zpm);

    const enrichedNaquadah = 'gtceu:enriched_naquadah_solution';
    const naquadria = 'gtceu:naquadria_solution';
    const wasteBatch = 32000;
    const fractions = [
        {
            name: 'enriched_naquadah', lens: 'green',
            product: enrichedNaquadah, amount: 4000,
            residue: 'gtcsolo:naquadria_bearing_residue', remaining: naquadria, remainingAmount: 1600
        },
        {
            name: 'naquadria', lens: 'red',
            product: naquadria, amount: 1600,
            residue: 'gtcsolo:enriched_naquadah_bearing_residue', remaining: enrichedNaquadah, remainingAmount: 4000
        }
    ];

    // Each branch removes one fraction. Distilling the residue recovers only the other fraction and byproducts.
    fractions.forEach(fraction => {
        if (!Fluid.exists(fraction.residue)) return;
        const residue = Fluid.of(fraction.residue, wasteBatch - fraction.amount);
        separate(fraction.name + '_from_waste')
            .notConsumable('#forge:lenses/' + fraction.lens)
            .inputFluids(Fluid.of('gtceu:enriched_naquadah_waste', wasteBatch))
            .outputFluids(Fluid.of(fraction.product, fraction.amount), residue)
            .duration(240).EUt(zpm);

        // The solid yield equals the expected yield from sixteen native distillation batches.
        event.recipes.gtceu.distillation_tower('gtcsolo:irradiation_separation/recover_after_' + fraction.name)
            .inputFluids(residue)
            .itemOutputs('8x gtceu:barium_sulfide_dust')
            .outputFluids(Fluid.of('gtceu:sulfuric_acid', 8000), Fluid.of(fraction.remaining, fraction.remainingAmount))
            .disableDistilleryRecipes(true)
            .duration(960).EUt(iv);
    });
});
