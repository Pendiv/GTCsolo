ServerEvents.recipes(event => {
    // Replace the heat generator recipe.

    event.remove({ id: 'minecraft:kjs/mekanismgenerators_heat_generator' });
    event.shaped(
        'mekanismgenerators:heat_generator',
        ['AAA', 'BCB', 'DED'],
        {A: 'gtceu:iron_plate', B: 'minecraft:oak_planks', C: '#forge:ingots/osmium', D: 'gtceu:copper_plate', E: 'minecraft:furnace'}
    ).id('gtcsolo:pack/heat_generator');
});
