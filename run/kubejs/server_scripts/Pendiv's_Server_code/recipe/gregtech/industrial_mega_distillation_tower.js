ServerEvents.recipes(event => {
    event.recipes.gtceu.assembler('gtcsolo:industrial_mega_distillation_tower')
        .itemInputs('4x gtceu:distillation_tower', '4x gtceu:iv_electric_pump',
            '4x #gtceu:circuits/iv', '16x gtceu:clean_machine_casing',
            '8x gtceu:stainless_evaporation_casing')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:industrial_mega_distillation_tower')
        .duration(1200)
        .EUt(7680);
});
