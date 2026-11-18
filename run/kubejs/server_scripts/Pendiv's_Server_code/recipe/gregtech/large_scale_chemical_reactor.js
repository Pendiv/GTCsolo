ServerEvents.recipes(event => {
    if (!Item.exists('gtcsolo:large_scale_chemical_reactor')) return;
    event.recipes.gtceu.assembler('gtcsolo:large_scale_chemical_reactor')
        .itemInputs('gtceu:large_chemical_reactor', '4x gtceu:inert_machine_casing',
            '2x gtceu:roentgenium_frame', '2x gtceu:ev_electric_pump', '2x #gtceu:circuits/ev')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtcsolo:large_scale_chemical_reactor')
        .duration(200).EUt(GTValues.VA[GTValues.EV]);
});
