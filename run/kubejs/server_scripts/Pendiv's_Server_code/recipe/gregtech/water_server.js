ServerEvents.recipes(event => {
    if (!Item.exists('gtceu:water_server')) return;
    event.shaped('gtceu:water_server', ['PWP', 'WBW', 'PWP'], {
        P: '#forge:plates/steel', W: 'gtceu:wood_wall', B: 'minecraft:bucket'
    }).id('gtcsolo:water_server');
});
