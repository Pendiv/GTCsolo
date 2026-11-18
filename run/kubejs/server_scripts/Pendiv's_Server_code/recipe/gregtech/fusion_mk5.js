ServerEvents.recipes(event => {
    if (!Item.exists('gtcsolo:fusion_reactor_mk5')) return;
    const voltage = GTValues.VA[GTValues.UIV];

    event.recipes.gtceu.assembler('gtcsolo:fusion_casing_mk5')
        .itemInputs('8x gtceu:fusion_casing_mk3', '1x gtceu:uiv_machine_hull')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs('8x gtcsolo:fusion_casing_mk5')
        .duration(200).EUt(voltage);

    event.recipes.gtceu.assembler('gtcsolo:advanced_fusion_coil')
        .itemInputs('4x gtceu:fusion_coil', '4x gtceu:uiv_field_generator')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs('4x gtcsolo:advanced_fusion_coil')
        .duration(400).EUt(voltage);

    event.recipes.gtceu.assembler('gtcsolo:fusion_reactor_mk5')
        .itemInputs('1x gtceu:uv_fusion_reactor', '8x gtcsolo:fusion_casing_mk5',
            '4x gtcsolo:advanced_fusion_coil', '4x #gtceu:circuits/uiv',
            '2x gtceu:uiv_sensor', '2x gtceu:uiv_field_generator')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2304))
        .itemOutputs('1x gtcsolo:fusion_reactor_mk5')
        .duration(1200).EUt(voltage);
});
