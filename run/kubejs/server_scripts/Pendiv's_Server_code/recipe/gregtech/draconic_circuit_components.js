ServerEvents.recipes(event => {
    if (!Item.exists('gtcsolo:draconic_epoxy_board')) return;

    const gt = event.recipes.gtceu;
    const zpm = GTValues.VA[GTValues.ZPM];
    const uv = GTValues.VA[GTValues.UV];
    const cleanroom = CleanroomType.CLEANROOM;
    const sterileCleanroom = CleanroomType.STERILE_CLEANROOM;
    const recipeId = name => 'gtcsolo:draconic_circuits/' + name;

    if (Fluid.exists('gtcsolo:dragon_breath')) {
        gt.electric_blast_furnace(recipeId('silicon_boule'))
            .itemInputs('64x gtceu:silicon_block', '8x #forge:ingots/draconium', '4x gtceu:gallium_arsenide_dust')
            .inputFluids(Fluid.of('gtcsolo:dragon_breath', 1000))
            .itemOutputs('gtcsolo:draconium_silicon_boule')
            .blastFurnaceTemp(9000).duration(24000).EUt(GTValues.VA[GTValues.UHV]);

        gt.autoclave(recipeId('raw_crystal_chip'))
            .itemInputs('2x gtceu:raw_crystal_chip', '2x gtceu:hafnium_foil')
            .inputFluids(Fluid.of('gtcsolo:dragon_breath', 250))
            .itemOutputs('2x gtcsolo:draconic_raw_crystal_chip')
            .cleanroom(cleanroom).duration(2400).EUt(uv);
    }

    gt.laser_engraver(recipeId('engraved_crystal_chip'))
        .itemInputs('gtcsolo:draconic_raw_crystal_chip')
        .notConsumable('#forge:lenses/purple')
        .itemOutputs('gtcsolo:draconic_engraved_crystal_chip')
        .cleanroom(cleanroom).duration(700).EUt(uv);

    gt.assembler(recipeId('silicon_interposer'))
        .itemInputs('2x gtcsolo:draconium_silicon_wafer', '4x gtceu:tantalum_foil')
        .inputFluids(Fluid.of('gtceu:polybenzimidazole', 144))
        .itemOutputs('4x gtcsolo:draconic_silicon_interposer')
        .cleanroom(cleanroom).duration(400).EUt(uv);

    gt.circuit_assembler(recipeId('crystal_soc'))
        .itemInputs('2x gtcsolo:draconic_engraved_crystal_chip', '4x gtcsolo:draconium_nano_cpu_chip',
            'gtcsolo:draconium_qbit_cpu_chip', '4x gtceu:fine_gold_wire')
        .inputFluids(Fluid.of('gtceu:indium', 144))
        .itemOutputs('2x gtcsolo:draconic_crystal_soc')
        .cleanroom(sterileCleanroom).duration(600).EUt(uv);

    gt.chemical_reactor(recipeId('epoxy_board'))
        .itemInputs('4x gtceu:fiber_reinforced_circuit_board', '8x gtcsolo:draconium_foil')
        .inputFluids(Fluid.of('gtceu:epoxy', 576))
        .itemOutputs('4x gtcsolo:draconic_epoxy_board')
        .cleanroom(cleanroom).duration(500).EUt(zpm);

    gt.chemical_reactor(recipeId('epoxy_printed_board'))
        .itemInputs('gtcsolo:draconic_epoxy_board', '8x gtceu:platinum_foil')
        .inputFluids(Fluid.of('gtceu:sodium_persulfate', 4000))
        .itemOutputs('gtcsolo:draconic_epoxy_printed_board')
        .cleanroom(cleanroom).duration(600).EUt(zpm);

    gt.circuit_assembler(recipeId('cpu'))
        .itemInputs('2x gtcsolo:draconic_crystal_soc', '2x gtcsolo:draconic_silicon_interposer',
            '2x gtcsolo:draconic_thermal_spreader', '4x gtcsolo:dsmd_capacitor', '4x gtcsolo:fine_tyrose_wire')
        .itemOutputs('2x gtcsolo:draconic_cpu')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .cleanroom(sterileCleanroom).duration(900).EUt(uv);

    const surfaceMountParts = [
        ['capacitor', '4x gtcsolo:alumina_dust', '4x gtcsolo:oxygen_free_copper_foil'],
        ['diode', '2x gtcsolo:draconium_ilc_chip', '4x gtceu:germanium_foil'],
        ['inductor', '4x gtcsolo:permalloy_foil', '8x gtcsolo:fine_oxygen_free_copper_wire'],
        ['resistor', '4x gtceu:carbon_fibers', '4x gtceu:fine_cupronickel_wire'],
        ['transistor', '2x gtcsolo:draconium_cpu_chip', '4x gtcsolo:tyrose_foil']
    ];
    surfaceMountParts.forEach(([name, primary, secondary]) => {
        gt.circuit_assembler(recipeId('dsmd_' + name))
            .itemInputs('4x gtceu:advanced_smd_' + name, primary, secondary)
            .itemOutputs('8x gtcsolo:dsmd_' + name)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 144)).cleanroom(cleanroom).duration(300).EUt(zpm);
    });

    ['nand', 'nor'].forEach(name => {
        gt.circuit_assembler(recipeId(name + '_memory_module'))
            .itemInputs('gtcsolo:draconic_epoxy_printed_board', '16x gtcsolo:draconium_' + name + '_memory_chip',
                '4x gtcsolo:dsmd_capacitor')
            .itemOutputs('4x gtcsolo:draconic_' + name + '_memory_module')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 144)).cleanroom(cleanroom).duration(300).EUt(uv);
    });

    gt.circuit_assembler(recipeId('memory_array'))
        .itemInputs('2x gtcsolo:draconic_nand_memory_module', '2x gtcsolo:draconic_nor_memory_module',
            '16x gtcsolo:draconium_ram_chip', '8x gtcsolo:fine_oxygen_free_copper_wire')
        .itemOutputs('2x gtcsolo:draconic_memory_array')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 216)).cleanroom(cleanroom).duration(400).EUt(uv);

    gt.circuit_assembler(recipeId('neural_board'))
        .itemInputs('gtceu:wetware_circuit_board', 'gtcsolo:draconic_epoxy_printed_board',
            '16x gtceu:stem_cells', '8x gtcsolo:draconium_foil')
        .inputFluids(Fluid.of('gtceu:sterilized_growth_medium', 1000))
        .itemOutputs('2x gtcsolo:dragon_neural_network_board')
        .cleanroom(sterileCleanroom).duration(800).EUt(uv);

    gt.chemical_reactor(recipeId('multilayer_board'))
        .itemInputs('2x gtcsolo:draconic_epoxy_printed_board', '4x gtcsolo:oxygen_free_copper_foil')
        .inputFluids(Fluid.of('gtcsolo:polyimide', 288))
        .itemOutputs('gtcsolo:draconic_multilayer_board')
        .cleanroom(cleanroom).duration(400).EUt(uv);

    gt.chemical_reactor(recipeId('multilayer_printed_board'))
        .itemInputs('gtcsolo:draconic_multilayer_board', '8x gtcsolo:draconium_foil')
        .inputFluids(Fluid.of('gtceu:sodium_persulfate', 4000))
        .itemOutputs('gtcsolo:draconic_multilayer_printed_board')
        .cleanroom(cleanroom).duration(600).EUt(uv);

    gt.assembler(recipeId('thermal_spreader'))
        .itemInputs('4x gtcsolo:cucr1zr_plate', '4x gtcsolo:rilun_foil',
            '4x gtcsolo:boron_nitride_filled_polyimide_foil')
        .inputFluids(Fluid.of('gtcsolo:galinstan', 144))
        .itemOutputs('4x gtcsolo:draconic_thermal_spreader')
        .duration(300).EUt(zpm);

    gt.circuit_assembler(recipeId('optical_bus'))
        .itemInputs('gtcsolo:draconic_multilayer_printed_board', '4x gtceu:engraved_crystal_chip',
            '4x gtcsolo:dsmd_diode', '8x gtceu:fine_borosilicate_glass_wire',
            '2x gtcsolo:aged_aluminium_yb_zr_plate')
        .itemOutputs('2x gtcsolo:draconic_optical_bus')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 216)).cleanroom(cleanroom).duration(400).EUt(uv);

    gt.circuit_assembler(recipeId('signal_coupler'))
        .itemInputs('gtcsolo:draconic_optical_bus', 'gtcsolo:draconic_cpu',
            '4x gtcsolo:dsmd_transistor', '4x gtcsolo:dsmd_resistor')
        .itemOutputs('2x gtcsolo:draconic_signal_coupler')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 216)).cleanroom(cleanroom).duration(400).EUt(uv);

    gt.circuit_assembler(recipeId('power_regulator'))
        .itemInputs('gtcsolo:draconic_multilayer_printed_board', '2x gtcsolo:draconium_uhpic_chip',
            '4x gtcsolo:dsmd_inductor', '8x gtcsolo:dsmd_capacitor', 'gtcsolo:draconic_thermal_spreader',
            '4x gtcsolo:permendur_foil')
        .itemOutputs('2x gtcsolo:draconic_power_regulator')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288)).cleanroom(cleanroom).duration(600).EUt(uv);
});
