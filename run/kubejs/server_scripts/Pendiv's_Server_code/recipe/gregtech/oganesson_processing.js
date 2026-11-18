// Selective photoionization and an electrodeposited Cf target represent the accelerator feed preparation.
// https://doi.org/10.1103/PhysRevC.107.034313
// https://doi.org/10.1140/epja/s10050-023-01144-y
// https://doi.org/10.1103/PhysRevC.74.044602
ServerEvents.recipes(event => {
    const target = 'gtcsolo:californium_249_target';
    if (!Item.exists('gtcsolo:fusion_reactor_mk5') ||
        !GTRecipeTypes.get('gtcsolo:irradiation_separation') || !Item.exists(target) ||
        !Fluid.exists('gtcsolo:calcium_48') || !Fluid.exists('gtcsolo:californium_249')) return;

    const gt = event.recipes.gtceu;
    const id = name => 'gtcsolo:oganesson/' + name;
    const separate = name => event.recipes.gtcsolo.irradiation_separation(id(name));
    const fluid = (name, amount) => Fluid.of('gtcsolo:' + name, amount);
    const uv = GTValues.VA[GTValues.UV];
    const uhv = GTValues.VA[GTValues.UHV];

    gt.fluid_heater(id('vaporize_calcium'))
        .inputFluids(Fluid.of('gtceu:calcium', 4000))
        .outputFluids(fluid('calcium_vapor', 4000))
        .duration(400).EUt(uv);

    // 16,000 mB of natural feed supplies at most 29.92 mB Ca-48 at 0.187%.
    // Keep both depleted cuts out of the natural-feed stream.
    separate('concentrate_calcium_48')
        .notConsumable('#forge:lenses/green')
        .inputFluids(fluid('calcium_vapor', 16000))
        .outputFluids(fluid('calcium_48_enriched_vapor', 256), fluid('calcium_48_depleted', 15744))
        .duration(12000).EUt(uhv);

    separate('purify_calcium_48')
        .notConsumable('#forge:lenses/blue')
        .inputFluids(fluid('calcium_48_enriched_vapor', 256))
        .outputFluids(fluid('calcium_48', 16), fluid('calcium_48_depleted', 240))
        .duration(4800).EUt(uhv);

    gt.mixer(id('concrete_from_depleted_calcium'))
        .itemInputs('2x gtceu:stone_dust', 'gtceu:gypsum_dust')
        .inputFluids(fluid('calcium_48_depleted', 1000), Fluid.of('minecraft:water', 1000))
        .outputFluids(Fluid.of('gtceu:concrete', 1152))
        .duration(200).EUt(GTValues.VA[GTValues.HV]);

    separate('concentrate_californium_249')
        .notConsumable('#forge:lenses/magenta')
        .inputFluids(Fluid.of('kubejs:liquid_californium', 256), Fluid.of('gtceu:argon', 1000))
        .outputFluids(fluid('californium_249_concentrate', 32), fluid('californium_249_depleted', 224))
        .duration(6000).EUt(uhv);

    gt.large_chemical_reactor(id('californium_plating_solution'))
        .inputFluids(fluid('californium_249_concentrate', 32), Fluid.of('gtceu:nitric_acid', 500))
        .outputFluids(fluid('californium_249_plating_solution', 500))
        .duration(2400).EUt(uhv);

    gt.electrolyzer(id('plate_californium_target'))
        .itemInputs('gtceu:titanium_plate')
        .inputFluids(fluid('californium_249_plating_solution', 500))
        .itemOutputs(target)
        .duration(3600).EUt(uhv);

    // The native fusion reactor takes two fluids; release the target material and recover its backing.
    gt.extractor(id('recover_target_californium'))
        .itemInputs(target)
        .itemOutputs('gtceu:titanium_plate')
        .outputFluids(fluid('californium_249', 16))
        .duration(1200).EUt(uv);

    gt.fusion_reactor(id('depleted_californium_to_anteria'))
        .inputFluids(fluid('californium_249_depleted', 16), Fluid.of('gtceu:nickel', 16))
        .outputFluids(fluid('anteria_plasma', 16))
        .duration(64).EUt(GTValues.V[GTValues.UV]).fusionStartEU(500000000);

    // 249Cf + 48Ca -> 294Og + 3n; sample lifetime and yield are abstracted.
    gt.fusion_reactor('gtcsolo:nuclear_fusion/californium_calcium_oganesson')
        .inputFluids(fluid('californium_249', 16), fluid('calcium_48', 16))
        .outputFluids(Fluid.of('gtceu:oganesson', 4))
        .duration(6000).EUt(GTValues.VA[GTValues.UEV]).fusionStartEU(2500000000);
});
