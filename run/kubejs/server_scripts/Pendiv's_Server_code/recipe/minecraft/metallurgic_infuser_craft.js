ServerEvents.recipes(event => {

    event.replaceInput(
        // Reject tag recipes before KJS resolves unrelated outputs on parallel workers.
        { output: 'mekanism:metallurgic_infuser', not: { or: [
            { type: 'cucumber:shaped_tag' },
            { type: 'cucumber:shapeless_tag' }
        ] } },
        'minecraft:redstone',
        '#gtceu:circuits/lv'
    )

})
