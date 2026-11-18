ServerEvents.recipes(event => {
    if (!Item.exists('gtcsolo:industrial_superheated_mega_blast_furnace')) return;

    const gt = event.recipes.gtceu;
    const uv = GTValues.VA[GTValues.UV];
    const casingCount = Java.loadClass('com.gregtechceu.gtceu.config.ConfigHolder').INSTANCE.recipes.casingsPerCraft;

    gt.assembler('gtcsolo:mithrilite_pipe_casing')
        .itemInputs('gtcsolo:mithrilite_frame', '4x gtcsolo:mithrilite_plate',
            '4x gtcsolo:mithrilite_normal_fluid_pipe')
        .circuit(6)
        .itemOutputs(Item.of('gtcsolo:mithrilite_pipe_casing', casingCount))
        .duration(100).EUt(uv);

    gt.assembler('gtcsolo:mithrilite_extreme_engine_intake_casing')
        .itemInputs('gtcsolo:mithrilite_robust_machine_casing', '2x gtcsolo:mithrilite_rotor',
            '4x gtcsolo:mithrilite_normal_fluid_pipe')
        .itemOutputs(Item.of('gtcsolo:mithrilite_extreme_engine_intake_casing', casingCount))
        .duration(100).EUt(uv);

    gt.assembly_line('gtcsolo:industrial_superheated_mega_blast_furnace')
        .itemInputs('gtceu:alloy_blast_smelter', 'gtceu:uhv_machine_hull',
            '16x gtceu:high_temperature_smelting_casing', '8x gtcsolo:mithrilite_robust_machine_casing',
            '4x gtcsolo:draconium_coil_block', '4x #gtceu:circuits/uhv',
            '4x gtceu:uv_electric_pump', '4x gtceu:uv_electric_piston')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2304), Fluid.of('gtceu:polybenzimidazole', 1152))
        .itemOutputs('gtcsolo:industrial_superheated_mega_blast_furnace')
        .stationResearch(research => research
            .researchId('gtcsolo_industrial_superheated_mega_blast_furnace_unlock')
            .researchStack(Item.of('gtceu:alloy_blast_smelter'))
            .CWUt(128).EUt(uv))
        .duration(1600).EUt(GTValues.VA[GTValues.UHV]);
});
