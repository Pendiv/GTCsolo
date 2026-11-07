// Exact scanned solids, in slot order; stacks above 64 are split for normal AL buses.
// The pump uses the corrected UHV_electric_pump scan, not the old duplicate piston scan.
ServerEvents.recipes(event => {

    // cmdex/export/UHV_electric_motor.txt
    event.recipes.gtceu.assembly_line('gtcsolo:uhv_electric_motor')
        .itemInputs(
            '2x gtceu:long_magnetic_samarium_rod',
            '4x gtcsolo:long_anteria_rod',
            '8x gtcsolo:anteria_ring',
            '32x gtcsolo:anteria_round',
            '64x gtcsolo:fine_mithrilite_wire',
            '64x gtcsolo:fine_mithrilite_wire',
            '2x gtceu:ruthenium_trinium_americium_neutronate_single_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 144),
            Fluid.of('gtcsolo:mithlite', 144),
            Fluid.of('gtceu:soldering_alloy', 1152),
            Fluid.of('gtceu:lubricant', 2000)
        )
        .itemOutputs('gtceu:uhv_electric_motor')
        .stationResearch(research => research
            .researchId('gtcsolo_uhv_electric_motor_unlock')
            .researchStack(Item.of('gtceu:uv_electric_motor'))
            .CWUt(100)
            .EUt(491520))
        .duration(1200).EUt(2097152);

    // cmdex/export/UHV_electric_pump.txt (corrected scan, 2026-10-20 01:24)
    event.recipes.gtceu.assembly_line('gtcsolo:uhv_electric_pump')
        .itemInputs(
            '1x gtceu:uhv_electric_piston',
            '4x gtceu:uhv_electric_motor',
            '4x gtcsolo:anteria_large_fluid_pipe',
            '16x gtcsolo:mithrilite_plate',
            '32x gtcsolo:anteria_screw',
            '64x gtceu:silicone_rubber_ring',
            '2x gtcsolo:anteria_rotor',
            '4x gtcsolo:hyperx_neutronium_octal_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 144),
            Fluid.of('gtcsolo:mithlite', 144),
            Fluid.of('gtceu:soldering_alloy', 1152),
            Fluid.of('gtceu:lubricant', 2000)
        )
        .itemOutputs('gtceu:uhv_electric_pump')
        .stationResearch(research => research
            .researchId('gtcsolo_uhv_electric_pump_unlock')
            .researchStack(Item.of('gtceu:uv_electric_pump'))
            .CWUt(112)
            .EUt(491520))
        .duration(1200).EUt(2097152);

    // cmdex/export/UHV_electric_piston.txt
    event.recipes.gtceu.assembly_line('gtcsolo:uhv_electric_piston')
        .itemInputs(
            '1x gtceu:uhv_electric_motor',
            '8x gtcsolo:mithrilite_plate',
            '8x gtcsolo:anteria_ring',
            '32x gtcsolo:anteria_round',
            '2x gtcsolo:long_anteria_rod',
            '2x gtcsolo:mithrilite_gear',
            '4x gtcsolo:small_mithrilite_gear',
            '2x gtceu:ruthenium_trinium_americium_neutronate_single_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 144),
            Fluid.of('gtcsolo:mithlite', 144),
            Fluid.of('gtceu:soldering_alloy', 1152),
            Fluid.of('gtceu:lubricant', 2000)
        )
        .itemOutputs('gtceu:uhv_electric_piston')
        .stationResearch(research => research
            .researchId('gtcsolo_uhv_electric_piston_unlock')
            .researchStack(Item.of('gtceu:uv_electric_piston'))
            .CWUt(108)
            .EUt(491520))
        .duration(1200).EUt(2097152);

    // cmdex/export/UHV_conveyor.txt
    event.recipes.gtceu.assembly_line('gtcsolo:uhv_conveyor_module')
        .itemInputs(
            '6x gtceu:uhv_electric_motor',
            '8x gtcsolo:mithrilite_plate',
            '32x gtcsolo:anteria_ring',
            '32x gtcsolo:anteria_round',
            '16x gtcsolo:anteria_screw',
            '2x gtceu:ruthenium_trinium_americium_neutronate_single_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 144),
            Fluid.of('gtcsolo:mithlite', 144),
            Fluid.of('gtceu:soldering_alloy', 1152),
            Fluid.of('gtceu:styrene_butadiene_rubber', 6912)
        )
        .itemOutputs('gtceu:uhv_conveyor_module')
        .stationResearch(research => research
            .researchId('gtcsolo_uhv_conveyor_module_unlock')
            .researchStack(Item.of('gtceu:uv_conveyor_module'))
            .CWUt(112)
            .EUt(491520))
        .duration(1200).EUt(2097152);

    // cmdex/export/UHV_robot_arm.txt
    event.recipes.gtceu.assembly_line('gtcsolo:uhv_robot_arm')
        .itemInputs(
            '8x gtcsolo:long_mithrilite_rod',
            '16x gtcsolo:mithrilite_gear',
            '32x gtcsolo:small_mithrilite_gear',
            '8x gtceu:uhv_electric_motor',
            '8x gtceu:uhv_electric_piston',
            '3x gtcsolo:draconic_processor_computer',
            '6x gtcsolo:draconic_processor_assembly',
            '12x gtcsolo:draconic_processor',
            '2x gtceu:ruthenium_trinium_americium_neutronate_single_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 144),
            Fluid.of('gtcsolo:mithlite', 144),
            Fluid.of('gtceu:soldering_alloy', 3456),
            Fluid.of('gtceu:lubricant', 2000)
        )
        .itemOutputs('gtceu:uhv_robot_arm')
        .stationResearch(research => research
            .researchId('gtcsolo_uhv_robot_arm_unlock')
            .researchStack(Item.of('gtceu:uv_robot_arm'))
            .CWUt(128)
            .EUt(491520))
        .duration(1200).EUt(2097152);

    // cmdex/export/UHV_censer.txt
    event.recipes.gtceu.assembly_line('gtcsolo:uhv_sensor')
        .itemInputs(
            '6x gtceu:uhv_electric_motor',
            '8x gtcsolo:mithrilite_plate',
            '32x gtcsolo:anteria_ring',
            '32x gtcsolo:anteria_round',
            '16x gtcsolo:anteria_screw',
            '4x gtcsolo:draconic_processor_computer',
            '2x draconicevolution:dragon_heart',
            '64x gtcsolo:mithrilite_foil',
            '64x gtcsolo:mithrilite_foil',
            '2x gtceu:ruthenium_trinium_americium_neutronate_single_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 144),
            Fluid.of('gtcsolo:mithlite', 144),
            Fluid.of('gtceu:soldering_alloy', 2304),
            Fluid.of('gtceu:naquadria', 1152)
        )
        .itemOutputs('gtceu:uhv_sensor')
        .stationResearch(research => research
            .researchId('gtcsolo_uhv_sensor_unlock')
            .researchStack(Item.of('gtceu:uv_sensor'))
            .CWUt(120)
            .EUt(491520))
        .duration(1200).EUt(2097152);

    // cmdex/export/UHV_emitter.txt
    event.recipes.gtceu.assembly_line('gtcsolo:uhv_emitter')
        .itemInputs(
            '2x gtcsolo:draconium_frame',
            '4x gtceu:uhv_electric_motor',
            '8x gtcsolo:long_mithrilite_rod',
            '2x draconicevolution:dragon_heart',
            '4x gtcsolo:draconic_processor_computer',
            '64x gtcsolo:mithrilite_foil',
            '64x gtcsolo:mithrilite_foil',
            '1x gtceu:big_neutronium_plate',
            '2x gtceu:ruthenium_trinium_americium_neutronate_single_wire'
        )
        .inputFluids(
            Fluid.of('gtceu:neutronium', 144),
            Fluid.of('gtcsolo:mithlite', 144),
            Fluid.of('gtceu:soldering_alloy', 2304),
            Fluid.of('gtceu:naquadria', 1152)
        )
        .itemOutputs('gtceu:uhv_emitter')
        .stationResearch(research => research
            .researchId('gtcsolo_uhv_emitter_unlock')
            .researchStack(Item.of('gtceu:uv_emitter'))
            .CWUt(124)
            .EUt(491520))
        .duration(1200).EUt(2097152);

    // This startup material requires a complete restart; never register an empty fluid input.
    if (Fluid.exists('gtcsolo:ma_fluid')) {
        // cmdex/export/UHV_fierd_gen.txt. The two 128-item inputs each occupy two AL buses.
        event.recipes.gtceu.assembly_line('gtcsolo:uhv_field_generator')
            .itemInputs(
                '2x gtcsolo:draconium_frame',
                '2x draconicevolution:dragon_heart',
                '4x gtceu:uhv_emitter',
                '6x gtceu:uhv_electric_motor',
                '4x gtcsolo:draconic_processor_computer',
                '8x gtcsolo:draconic_processor_assembly',
                '64x gtcsolo:fine_hyperx_neutronium_wire',
                '64x gtcsolo:fine_mithrilite_wire',
                '64x gtcsolo:fine_mithrilite_wire',
                '64x gtcsolo:mithrilite_foil',
                '64x gtcsolo:mithrilite_foil',
                '1x gtceu:big_neutronium_plate',
                '2x gtceu:ruthenium_trinium_americium_neutronate_single_wire'
            )
            .inputFluids(
                Fluid.of('gtceu:neutronium', 144),
                Fluid.of('gtcsolo:mithlite', 144),
                Fluid.of('gtceu:soldering_alloy', 3456),
                Fluid.of('gtcsolo:ma_fluid', 4)
            )
            .itemOutputs('gtceu:uhv_field_generator')
            .stationResearch(research => research
                .researchId('gtcsolo_uhv_field_generator_unlock')
                .researchStack(Item.of('gtceu:uv_field_generator'))
                .CWUt(128)
                .EUt(491520))
            .duration(1200).EUt(2097152);
    } else {
        console.warn('[UHV components] Restart Minecraft to register MA Fluid before loading the field generator recipe.');
    }
});
