ServerEvents.recipes(event => {
    if (!Item.exists('gtcsolo:fusion_reactor_mk4')) return;

    const gt = event.recipes.gtceu;
    const uhv = GTValues.VA[GTValues.UHV];
    const casingCount = Java.loadClass('com.gregtechceu.gtceu.config.ConfigHolder').INSTANCE.recipes.casingsPerCraft;

    gt.assembler('gtcsolo:fusion_casing_mk4')
        .itemInputs('gtceu:uhv_machine_casing', 'gtceu:fusion_coil', '2x gtceu:uv_voltage_coil',
            'gtceu:uv_field_generator', '6x gtcsolo:mithrilite_plate')
        .inputFluids(Fluid.of('gtceu:polybenzimidazole', 1152))
        .itemOutputs(Item.of('gtcsolo:fusion_casing_mk4', casingCount))
        .duration(200).EUt(uhv)
        .cleanroom(CleanroomType.CLEANROOM);

    gt.assembly_line('gtcsolo:fusion_reactor_mk4')
        .itemInputs('gtceu:uv_fusion_reactor', 'gtceu:fusion_coil', '8x gtcsolo:fusion_casing_mk4',
            '8x #gtceu:circuits/uhv', '2x gtceu:quantum_star', '4x gtceu:uv_field_generator',
            '64x gtceu:uhpic_chip', '32x gtceu:ruthenium_trinium_americium_neutronate_single_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2304), Fluid.of('gtcsolo:mithrilite', 1152))
        .itemOutputs('gtcsolo:fusion_reactor_mk4')
        .stationResearch(research => research
            .researchId('gtcsolo_fusion_reactor_mk4_unlock')
            .researchStack(Item.of('gtceu:uv_fusion_reactor'))
            .CWUt(128).EUt(GTValues.VA[GTValues.UV]))
        .duration(1600).EUt(uhv);
});
