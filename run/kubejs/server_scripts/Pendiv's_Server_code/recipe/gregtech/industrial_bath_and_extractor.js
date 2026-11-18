ServerEvents.recipes(event => {
    if (Item.exists('gtceu:industrial_chemical_bath')) {
        event.recipes.gtceu.assembler('gtcsolo:industrial_machines/industrial_chemical_bath')
            .itemInputs('gtceu:large_chemical_bath', '2x gtceu:watertight_casing', '2x gtceu:naquadah_alloy_plate',
                '2x gtceu:iv_electric_pump', '2x #gtceu:circuits/iv')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
            .itemOutputs('gtceu:industrial_chemical_bath')
            .duration(200).EUt(GTValues.VA[GTValues.IV]);
    }
    if (Item.exists('gtceu:heating_chamber')) {
        event.recipes.gtceu.assembler('gtcsolo:industrial_machines/heating_chamber')
            .itemInputs('gtceu:large_extractor', '2x gtceu:stable_machine_casing', '2x gtceu:titanium_pipe_casing',
                '2x gtceu:iv_electric_pump', '2x #gtceu:circuits/iv')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
            .itemOutputs('gtceu:heating_chamber')
            .duration(200).EUt(GTValues.VA[GTValues.IV]);
    }
});
