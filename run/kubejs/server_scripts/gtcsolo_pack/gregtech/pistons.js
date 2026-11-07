ServerEvents.recipes(event => {
    // Use the corresponding gear material for each piston recipe.

    event.remove({ id: 'gtceu:shaped/piston_titanium' })
    event.shaped(
        Item.of('minecraft:piston', 8),
        ['AAA', 'BCB', 'BDB'],
        {A: '#minecraft:planks', B: '#minecraft:stone_crafting_materials', C: '#forge:gears/titanium', D: '#forge:double_plates/red_alloy'}
    ).id('gtcsolo:pack/piston/titanium');

    event.remove({ id: 'gtceu:shaped/piston_aluminium' })
    event.shaped(
        Item.of('minecraft:piston', 4),
        ['AAA', 'BCB', 'BDB'],
        {A: '#minecraft:planks', B: '#minecraft:stone_crafting_materials', C: '#forge:gears/aluminium', D: '#forge:double_plates/red_alloy'}
    ).id('gtcsolo:pack/piston/aluminium');

    event.remove({ id: 'gtceu:shaped/piston_bronze' })
    event.shaped(
        'minecraft:piston',
        ['AAA', 'BCB', 'BDB'],
        {A: '#minecraft:planks', B: '#minecraft:stone_crafting_materials', C: '#forge:gears/bronze', D: '#forge:double_plates/red_alloy'}
    ).id('gtcsolo:pack/piston/bronze');

    event.remove({ id: 'gtceu:shaped/piston_steel' })
    event.shaped(
        Item.of('minecraft:piston', 2),
        ['AAA', 'BCB', 'BDB'],
        {A: '#minecraft:planks', B: '#minecraft:stone_crafting_materials', C: '#forge:gears/steel', D: '#forge:double_plates/red_alloy'}
    ).id('gtcsolo:pack/piston/steel');

    event.remove({ id: 'minecraft:piston' })
    event.shaped(
        'minecraft:piston',
        ['AAA', 'BCB', 'BDB'],
        {A: '#minecraft:planks', B: '#minecraft:stone_crafting_materials', C: '#forge:gears/iron', D: '#forge:double_plates/red_alloy'}
    ).id('gtcsolo:pack/piston/iron');
});
