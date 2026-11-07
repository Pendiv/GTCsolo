// Supplement the existing isotope route; its separation yields and nuclear reaction stay unchanged.
ServerEvents.recipes(event => {
    // A high-voltage compact alternative inspired by iodine recovery from seaweed.
    // Yield and reagent volumes are gameplay values; this combines extraction, oxidation and recovery.
    event.recipes.gtceu.large_chemical_reactor('gtcsolo:nihonium/iodine_from_kelp')
        .itemInputs('64x minecraft:dried_kelp')
        .inputFluids(
            Fluid.of('gtceu:oxygen', 32000),
            Fluid.of('gtceu:chlorine', 1000),
            Fluid.of('gtceu:sulfuric_acid', 2000),
            Fluid.of('minecraft:water', 1000)
        )
        .itemOutputs('gtceu:iodine_dust', '3x gtceu:ash_dust')
        .outputFluids(
            Fluid.of('gtceu:diluted_sulfuric_acid', 3000),
            Fluid.of('gtceu:carbon_dioxide', 16000),
            Fluid.of('gtceu:salt_water', 1000),
            Fluid.of('minecraft:water', 15000)
        )
        .duration(400).EUt(491520);

    // The oxide is a new startup material; /reload alone cannot register it.
    if (!Item.exists('gtcsolo:zinc_70_depleted_oxide_dust')) {
        console.warn('[Nihonium byproducts] Restart Minecraft to register recovered zinc oxide.');
        return;
    }
    event.recipes.gtceu.chemical_reactor('gtcsolo:nihonium/depleted_zinc_oxidation')
        .itemInputs('gtcsolo:zinc_70_depleted_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('2x gtcsolo:zinc_70_depleted_oxide_dust')
        .duration(200).EUt(120);

    // ZnO is incorporated as a vulcanization activator. Native rubber/sulfur ratios are retained.
    // Each batch produces the same polymer quantity as eight native batches in half their base time.
    // Do not return generic zinc through alloy decomposition: it would reset the removed 70Zn fraction.
    [
        ['rubber', 'gtceu:raw_rubber_dust', 'gtceu:rubber', 16],
        ['styrene_butadiene_rubber', 'gtceu:raw_styrene_butadiene_rubber_dust', 'gtceu:styrene_butadiene_rubber', 30]
    ].forEach(entry => {
        event.recipes.gtceu.large_chemical_reactor('gtcsolo:nihonium/zinc_activated_' + entry[0])
            .itemInputs('72x ' + entry[1], '8x gtceu:sulfur_dust', '2x gtcsolo:zinc_70_depleted_oxide_dust')
            .outputFluids(Fluid.of(entry[2], 10368))
            .duration(2400).EUt(entry[3]);
    });
});
