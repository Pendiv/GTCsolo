// Materials: cmdex/export/ial.txt. Preserve first-slot order and every ID total.
ServerEvents.recipes(event => {
    const ialLuV = 32768;
    event.recipes.gtceu.assembly_line('gtcsolo:iterative_assembly_line')
        .itemInputs(
            '1x gtceu:assembly_line',
            '8x gtceu:wetware_processor',
            '12x gtceu:big_trinium_plate',
            '8x gtceu:big_europium_plate',
            '4x gtceu:big_naquadah_plate',
            '4x gtceu:luv_field_generator',
            '4x gtceu:luv_robot_arm',
            '16x gtceu:naquadah_spring',
            '64x gtceu:naquadah_foil',
            '64x gtceu:naquadah_foil',
            '64x gtceu:indium_tin_barium_titanium_cuprate_single_wire',
            '64x gtceu:fine_indium_tin_barium_titanium_cuprate_wire',
            '64x gtceu:fine_indium_tin_barium_titanium_cuprate_wire',
            '64x gtceu:fine_indium_tin_barium_titanium_cuprate_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:soldering_alloy', 1152),
            Fluid.of('gtceu:lubricant', 2000),
            Fluid.of('gtceu:styrene_butadiene_rubber', 2304)
        )
        .itemOutputs('1x gtcsolo:iterative_assembly_line')
        .stationResearch(research => research
            // A path-only ID avoids GT1.6.4's automatic namespace prefix collision.
            .researchId('gtcsolo_iterative_assembly_line_unlock')
            .researchStack(Item.of('gtceu:nano_processor_mainframe'))
            // Positive CWU forces Research Station, even with the smallest valid workload.
            .CWUt(1, 1)
            .EUt(1))
        .EUt(ialLuV)
        .duration(300);
});
