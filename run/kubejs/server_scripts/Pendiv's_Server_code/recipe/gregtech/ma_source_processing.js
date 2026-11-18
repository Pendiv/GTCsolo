ServerEvents.recipes(event => {
    if (!GTRecipeTypes.get('gtcsolo:irradiation_separation') || !Fluid.exists('gtcsolo:concentrated_ma_source')) return;

    const gt = event.recipes.gtceu;
    const fluid = (id, amount) => Fluid.of(id, amount);
    const uv = GTValues.VA[GTValues.UV];
    const uhv = GTValues.VA[GTValues.UHV];

    event.recipes.gtcsolo.irradiation_separation('gtcsolo:ma_source_processing/irradiate')
        .notConsumable('gtcsolo:purified_chi_lens')
        .inputFluids(fluid('kubejs:unrefined_ma_fluid', 1000))
        .outputFluids(fluid('gtcsolo:concentrated_ma_source', 100), fluid('gtcsolo:mantle_magma', 900),
            fluid('gtcsolo:deep_mantle_gas', 250))
        .duration(100).EUt(uhv);

    gt.vacuum_freezer('gtcsolo:ma_source_processing/crystallize')
        .inputFluids(fluid('gtcsolo:concentrated_ma_source', 200), fluid('gtceu:pcb_coolant', 1000))
        .itemOutputs('gtcsolo:ma_matter_crystal')
        .outputFluids(fluid('gtcsolo:spent_pcb_coolant', 1000))
        .duration(800).EUt(uv);

    gt.vacuum_freezer('gtcsolo:ma_source_processing/recover_pcb_coolant')
        .inputFluids(fluid('gtcsolo:spent_pcb_coolant', 1000))
        .outputFluids(fluid('gtceu:pcb_coolant', 900))
        .duration(300).EUt(GTValues.VA[GTValues.ZPM]);

    gt.extractor('gtcsolo:ma_source_processing/melt_crystal')
        .itemInputs('gtcsolo:ma_matter_crystal')
        .outputFluids(fluid('gtcsolo:ma_fluid', 100))
        .duration(200).EUt(uv);

    gt.distillation_tower('gtcsolo:ma_source_processing/distill_deep_gas')
        .inputFluids(fluid('gtcsolo:deep_mantle_gas', 1000))
        .outputFluids(fluid('gtceu:carbon_dioxide', 900), fluid('gtceu:helium', 99), fluid('gtceu:helium_3', 1))
        .disableDistilleryRecipes(true)
        .duration(200).EUt(uv);

    gt.centrifuge('gtcsolo:ma_source_processing/separate_magma')
        .inputFluids(fluid('gtcsolo:mantle_magma', 3600))
        .itemOutputs('2x gtceu:crushed_pentlandite_ore', '2x gtceu:crushed_chalcopyrite_ore',
            'gtceu:platinum_group_sludge_dust', 'gtcsolo:heavy_rare_earth_concentrate_dust',
            '8x gtceu:silicon_dioxide_dust', '4x gtceu:magnesia_dust')
        .duration(240).EUt(uv);

    gt.rare_mineral_refining('gtcsolo:ma_source_processing/recover_lutetium')
        .itemInputs('4x gtcsolo:heavy_rare_earth_concentrate_dust', 'gtceu:calcium_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 2000))
        .itemOutputs('gtceu:lutetium_dust', 'gtceu:gypsum_dust')
        .duration(320).EUt(GTValues.VA[GTValues.IV]);
});
