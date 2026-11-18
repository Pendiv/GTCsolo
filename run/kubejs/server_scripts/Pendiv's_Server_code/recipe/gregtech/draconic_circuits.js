ServerEvents.recipes(event => {
    if (!Item.exists('gtcsolo:draconic_processor') || !Item.exists('gtcsolo:draconic_epoxy_printed_board')) return;

    const gt = event.recipes.gtceu;
    const uv = GTValues.VA[GTValues.UV];
    const sterile = CleanroomType.STERILE_CLEANROOM;

    gt.circuit_assembler('gtcsolo:draconic_processor')
        .itemInputs('gtcsolo:draconic_epoxy_printed_board', 'gtcsolo:draconic_cpu',
            '8x gtcsolo:dsmd_capacitor', '8x gtcsolo:dsmd_transistor',
            '8x gtcsolo:fine_mithrilite_wire', 'gtcsolo:draconic_thermal_spreader')
        .itemOutputs('2x gtcsolo:draconic_processor')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144)).cleanroom(sterile).duration(200).EUt(uv);

    gt.circuit_assembler('gtcsolo:draconic_processor_assembly')
        .itemInputs('gtcsolo:dragon_neural_network_board', '2x gtcsolo:draconic_processor',
            '8x gtcsolo:dsmd_inductor', '16x gtcsolo:dsmd_capacitor',
            '4x gtcsolo:draconic_memory_array', '16x gtcsolo:fine_tyrose_wire')
        .itemOutputs('2x gtcsolo:draconic_processor_assembly')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288)).cleanroom(sterile).duration(400).EUt(uv);

    // The first computer is needed to build the industrial circuit assembly line.
    gt.assembly_line('gtcsolo:draconic_processor_computer')
        .itemInputs('gtcsolo:draconic_multilayer_printed_board', '2x gtcsolo:draconic_processor_assembly',
            '8x gtcsolo:dsmd_diode', '8x gtcsolo:draconic_memory_array',
            '4x gtcsolo:draconic_nor_memory_module', '8x gtcsolo:draconic_nand_memory_module',
            '4x gtcsolo:draconic_optical_bus', '2x gtcsolo:draconic_thermal_spreader')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152), Fluid.of('gtceu:polybenzimidazole', 576))
        .itemOutputs('gtcsolo:draconic_processor_computer')
        .stationResearch(research => research
            .researchId('gtcsolo_draconic_computer_unlock')
            .researchStack(Item.of('gtcsolo:draconic_processor_assembly'))
            .dataStack(Item.of('gtceu:data_orb'))
            .CWUt(128).EUt(uv))
        .duration(600).EUt(uv);

    if (!GTRecipeTypes.get('gtcsolo:circuit_assembly_line') || !Fluid.exists('gtcsolo:dma_fluid') ||
        !Item.exists('gtcsolo:circuit_research_storage')) return;

    event.recipes.gtcsolo.circuit_assembly_line('gtcsolo:draconic_processor_mainframe')
        .itemInputs('2x gtcsolo:yttrium_refractory_high_entropy_alloy_frame', '2x gtcsolo:draconic_processor_computer',
            'gtcsolo:draconic_star', '4x gtcsolo:draconic_power_regulator', '4x gtcsolo:draconic_signal_coupler',
            '8x gtcsolo:draconic_nor_memory_module', '16x gtcsolo:draconic_nand_memory_module',
            '16x gtcsolo:dsmd_diode', '32x gtcsolo:dsmd_capacitor', '16x gtcsolo:dsmd_transistor',
            '16x gtcsolo:dsmd_resistor', '16x gtcsolo:dsmd_inductor',
            '8x gtceu:ruthenium_trinium_americium_neutronate_single_wire')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2880), Fluid.of('gtceu:polybenzimidazole', 1152),
            Fluid.of('gtcsolo:dma_fluid', 250))
        .itemOutputs('gtcsolo:draconic_processor_mainframe')
        .stationResearch(research => research
            .researchId('gtcsolo_draconic_mainframe_unlock')
            .researchStack(Item.of('gtcsolo:draconic_processor_computer'))
            .dataStack(Item.of('gtcsolo:circuit_research_storage'))
            .CWUt(512).EUt(GTValues.VA[GTValues.UHV]))
        .duration(1200).EUt(GTValues.VA[GTValues.UEV]);
});
