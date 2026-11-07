ServerEvents.recipes(event => {

    event.shaped(
        'mekanism:metallurgic_infuser',
        ['ABA', 'CDC', 'ABA'],
        {A: 'minecraft:iron_ingot', B: 'gtceu:double_bronze_plate', C: '#gtceu:circuits/lv', D: 'gtceu:lv_machine_hull'}
    ).id('gtcsolo:pack/metallurgic_infuser');
});
