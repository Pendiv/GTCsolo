ServerEvents.recipes(event => {

    event.remove({ id: 'mekanism:control_circuit/ultimate' });
    event.shaped(
        'mekanism:ultimate_control_circuit',
        ['ABA', 'CDC', 'EFE'],
        {A: 'gtceu:uranium_triplatinum_single_wire', B: 'gtceu:ev_sensor', C: 'gtcsolo:refined_obsidian_plate', D: 'mekanism:elite_control_circuit', E: '#gtceu:circuits/ev', F: 'gtceu:ev_electric_motor'}
    ).id('gtcsolo:pack/ultimate_control_circuit');
    event.remove({ id: 'mekanism:control_circuit/elite' });
    event.shaped(
        'mekanism:elite_control_circuit',
        ['ABA', 'CDC', 'EFE'],
        {A: 'mekanism:alloy_reinforced', B: 'gtceu:hv_sensor', C: 'gtceu:blue_steel_plate', D: 'mekanism:advanced_control_circuit', E: '#gtceu:circuits/hv', F: 'gtceu:hv_electric_motor'}
    ).id('gtcsolo:pack/elite_control_circuit');
    event.remove({ id: 'mekanism:control_circuit/advanced' });
    event.shaped(
        'mekanism:advanced_control_circuit',
        ['ABA', 'CDC', 'EFE'],
        {A: 'mekanism:alloy_infused', B: 'gtceu:mv_sensor', C: 'gtceu:aluminium_plate', D: 'mekanism:basic_control_circuit', E: '#gtceu:circuits/mv', F: 'gtceu:mv_electric_motor'}
    ).id('gtcsolo:pack/advanced_control_circuit');
    event.remove({ id: 'mekanism:metallurgic_infuser' });
    event.remove({ id: 'mekanism:logistical_sorter' });
    event.shaped(
        'mekanism:logistical_sorter',
        ['ABA', 'CDC', 'EFE'],
        {A: 'gtceu:ev_robot_arm', B: 'gtceu:ev_conveyor_module', C: 'gtceu:energy_crystal', D: 'mekanism:ultimate_control_circuit', E: 'gtceu:big_red_steel_plate', F: 'gtceu:ev_field_generator'}
    ).id('gtcsolo:pack/logistical_sorter');
    event.remove({ id: 'mekanism:processing/osmium/ingot/from_nuggets' });
    event.shaped(
        'gtcsolo:impure_osmium_ingot',
        ['AAA', 'AAA', 'AAA'],
        {A: 'gtcsolo:impure_osmium_nugget'}
    ).id('gtcsolo:pack/impure_osmium_ingot');
});
