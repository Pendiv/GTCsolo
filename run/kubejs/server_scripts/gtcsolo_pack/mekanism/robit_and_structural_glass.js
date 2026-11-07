ServerEvents.recipes(event => {
    // Add the pack recipes for the Robit and structural glass.

    event.shaped(
        'mekanism:robit',
        ['ABA', 'CDC', 'EFE'],
        {A: 'mekanism:ultimate_control_circuit', B: 'gtceu:computer_monitor_cover', C: 'gtceu:ev_robot_arm', D: 'gtceu:ev_voltage_coil', E: 'gtceu:ev_conveyor_module', F: 'gtceu:ev_field_generator'}
    ).id('gtcsolo:pack/robit');
    event.shaped(
        Item.of('mekanism:structural_glass', 4),
        ['ABA', 'BCB', 'ABA'],
        {A: '#forge:plates/steel', B: 'gtceu:tempered_glass', C: 'gtceu:steel_frame'}
    ).id('gtcsolo:pack/structural_glass');
});
