ServerEvents.recipes(event => {
    // Replace IV energy hatches with pack-specific components.

    event.remove({ id: 'gtceu:assembler/energy_hatch_iv' });
    event.recipes.gtceu.assembler('gtcsolo:pack/iv_energy_input_hatch')
        .itemInputs('gtceu:iv_machine_hull', '2x gtceu:tungsten_single_cable', '2x gtceu:hpic_chip', 'gtceu:iv_voltage_coil', '4x gtcsolo:boron_nitride_filled_polyimide_foil')
        .inputFluids(Fluid.of('gtceu:sodium_potassium', 3000))
        .itemOutputs('gtceu:iv_energy_input_hatch')
        .duration(200)
        .EUt(7680);

    event.remove({ id: 'gtceu:assembler/dynamo_hatch_iv' });
    event.recipes.gtceu.assembler('gtcsolo:pack/iv_energy_output_hatch')
        .itemInputs('gtceu:iv_machine_hull', '2x gtceu:tungsten_spring', '2x gtceu:hpic_chip', 'gtceu:iv_voltage_coil', '4x gtcsolo:boron_nitride_filled_polyimide_foil')
        .inputFluids(Fluid.of('gtceu:sodium_potassium', 3000))
        .itemOutputs('gtceu:iv_energy_output_hatch')
        .duration(200)
        .EUt(7680);
});
