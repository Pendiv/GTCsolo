ServerEvents.recipes(event => {
    event.recipes.gtceu.chemical_reactor('gtcsolo:grass_block')
        .itemInputs('minecraft:dirt', 'gtceu:fertilizer')
        .itemOutputs('minecraft:grass_block')
        .duration(60)
        .EUt(7);
});
