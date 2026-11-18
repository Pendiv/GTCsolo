ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu;
    gt.assembler('gtcsolo:electropolished_stainless_casing')
        .itemInputs('gtceu:clean_machine_casing', '2x gtceu:stainless_steel_foil')
        .inputFluids(Fluid.of('gtceu:phosphoric_acid', 144))
        .itemOutputs('gtcsolo:electropolished_stainless_casing')
        .duration(100).EUt(GTValues.VA[GTValues.IV]);

    gt.assembler('gtcsolo:advanced_semiconductor_fab')
        .itemInputs('gtceu:uhv_machine_hull', 'gtceu:uv_laser_engraver', 'gtceu:uv_circuit_assembler',
            '4x #gtceu:circuits/uhv', '4x gtceu:uv_sensor', '8x gtcsolo:electropolished_stainless_casing')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs('gtceu:advanced_semiconductor_fab')
        .duration(1200).EUt(GTValues.VA[GTValues.UHV]);
});
