// Items from cmdex/export/scan_20261014_141141_4332227505434602248.txt.
// Preserve the 14 item IDs and totals; split 128 casings into two conventional AL input buses.
ServerEvents.recipes(event => {
    const icalUHV = 2097152;
    event.recipes.gtceu.assembly_line('gtcsolo:industrial_circuit_assembly_line')
        .itemInputs(
            '1x gtceu:large_circuit_assembler',
            '1x gtceu:uhv_circuit_assembler',
            '1x gtceu:assembly_line',
            '8x gtceu:wetware_processor_mainframe',
            '12x gtceu:uhv_field_generator',
            '32x gtceu:uhv_conveyor_module',
            '8x gtceu:uhv_electric_motor',
            '2x gtceu:uhv_sensor',
            '32x gtceu:uhv_robot_arm',
            '1x gtceu:max_battery',
            '64x gtceu:assembly_line_casing',
            '64x gtceu:assembly_line_casing',
            '16x gtceu:ruthenium_trinium_americium_neutronate_single_wire',
            '2x gtceu:neutronium_buzz_saw_blade',
            '8x gtceu:neutronium_gear'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 9216),
            Fluid.of('gtceu:europium', 18432),
            Fluid.of('gtceu:styrene_butadiene_rubber', 64000)
        )
        .itemOutputs('1x gtcsolo:industrial_circuit_assembly_line')
        .stationResearch(research => research
            // GT1.6.4 prefixes this ID with gtceu: when generating the research recipe.
            .researchId('gtcsolo_industrial_circuit_assembly_line_unlock')
            .researchStack(Item.of('gtceu:large_circuit_assembler'))
            .CWUt(512, 36864000)
            .EUt(icalUHV))
        .EUt(icalUHV)
        .duration(800);
});
