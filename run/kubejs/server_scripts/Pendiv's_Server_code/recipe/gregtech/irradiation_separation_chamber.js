ServerEvents.recipes(event => {
    const controller = 'gtcsolo:irradiation_separation_chamber';
    if (!Item.exists(controller) || !GTRecipeTypes.get('gtcsolo:irradiation_separation')) return;

    const gt = event.recipes.gtceu;
    const separation = event.recipes.gtcsolo.irradiation_separation;
    gt.assembler('gtcsolo:irradiation_separation_chamber')
        .itemInputs('gtceu:iv_laser_engraver', '2x gtceu:iv_emitter', '2x gtceu:iv_sensor',
            '2x #gtceu:circuits/iv', '2x gtceu:laser_safe_engraving_casing', '2x gtcsolo:sus310s_heatproof_casing',
            'gtcsolo:hastelloy_c276_pipe_casing', 'gtcsolo:sus316l_pipe_casing', 'gtcsolo:voltage_glass_iv')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs(controller)
        .duration(400).EUt(GTValues.VA[GTValues.IV]);

    const glassGrades = [
        ['iv', GTValues.IV, 'gtceu:laminated_glass', 'gtceu:tungsten_steel_plate', 'gtceu:polytetrafluoroethylene'],
        ['luv', GTValues.LuV, 'gtcsolo:voltage_glass_iv', 'gtceu:rhodium_plated_palladium_plate', 'gtceu:polybenzimidazole'],
        ['zpm', GTValues.ZPM, 'gtcsolo:voltage_glass_luv', 'gtceu:naquadah_alloy_plate', 'gtceu:polybenzimidazole'],
        ['uv', GTValues.UV, 'gtcsolo:voltage_glass_zpm', 'gtceu:europium_plate', 'gtceu:polybenzimidazole'],
        ['uhv', GTValues.UHV, 'gtcsolo:voltage_glass_uv', 'gtcsolo:mithrilite_plate', 'gtceu:polybenzimidazole']
    ];
    glassGrades.forEach(([grade, tier, glass, plate, polymer]) => {
        gt.assembler('gtcsolo:voltage_glass_' + grade)
            .itemInputs('4x ' + glass, '2x ' + plate)
            .inputFluids(Fluid.of(polymer, 144))
            .itemOutputs('4x gtcsolo:voltage_glass_' + grade)
            .duration(160).EUt(GTValues.VA[tier]);
    });

    // Four batches of GregTech's sludge separation, with the original acid and material ratios.
    separation('gtcsolo:platinum_group_sludge')
        .itemInputs('24x gtceu:platinum_group_sludge_dust')
        .notConsumable('#forge:lenses/light_blue')
        .inputFluids(Fluid.of('gtceu:aqua_regia', 4800))
        .itemOutputs('12x gtceu:platinum_raw_dust', '12x gtceu:palladium_raw_dust', '8x gtceu:inert_metal_mixture_dust',
            '4x gtceu:rarest_metal_mixture_dust', '8x gtceu:platinum_sludge_residue_dust')
        .duration(360).EUt(GTValues.VA[GTValues.IV]);
});
