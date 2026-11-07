// Supplement native item tooltips without replacing names, formulas or mod attribution.
ItemEvents.tooltip(event => {
    [
        ['gtcsolo:zinc_70_depleted_dust', 'gtcsolo.nihonium.depleted_zinc.description'],
        ['gtcsolo:zinc_70_depleted_oxide_dust', 'gtcsolo.nihonium.zinc_oxide.description'],
        ['gtceu:iodine_dust', 'gtcsolo.nihonium.iodine.description']
    ].forEach(entry => {
        if (Item.exists(entry[0])) event.add(entry[0], Component.translatable(entry[1]));
    });
});
