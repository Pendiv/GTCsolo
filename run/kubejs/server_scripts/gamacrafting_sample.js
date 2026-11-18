ServerEvents.recipes(event => {
    if (!Platform.isLoaded('gamacrafting')) return;

    event.custom({
        type: 'gamacrafting:shaped_desk',
        pattern: ['PPPPPPPPP'],
        key: { P: { tag: 'minecraft:planks' } },
        result: { item: 'minecraft:chest' },
        minecraft: true
    }).id('gtcsolo:gamacrafting/sample_chest');
});
