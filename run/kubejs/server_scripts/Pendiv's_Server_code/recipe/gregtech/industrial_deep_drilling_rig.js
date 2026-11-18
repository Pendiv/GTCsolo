ServerEvents.recipes(event => {
    event.recipes.gtceu.assembler('gtcsolo:industrial_deep_drilling_rig')
        .itemInputs('gtceu:ev_fluid_drilling_rig', '2x gtceu:ev_electric_piston',
            'gtceu:ev_electric_pump', '2x gtcsolo:delinoy_gearbox',
            '4x gtcsolo:sus316l_machine_casing')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('gtceu:industrial_deep_drilling_rig')
        .duration(400)
        .EUt(1920);
});
