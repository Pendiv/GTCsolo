ServerEvents.recipes(event => {
    // Replace the vanilla furnace recipe.

    event.remove({ id: 'minecraft:furnace' })
    event.shaped(
        'minecraft:furnace',
        ['AAA', 'ABA', 'AAA'],
        {A: '#minecraft:stone_crafting_materials', B: 'minecraft:flint'}
    ).id('gtcsolo:pack/furnace');
});
