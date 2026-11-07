ServerEvents.recipes(event => {

    event.remove({ id: 'mekanism:metallurgic_infusing/control_circuit/basic' })

    event.custom({
        type: 'mekanism:metallurgic_infusing',
        chemicalInput: {
            amount: 20,
            tag: 'mekanism:redstone'
        },
        itemInput: {
            ingredient: {
                tag: 'forge:plates/osmium'
            }
        },
        output: {
            item: 'mekanism:basic_control_circuit'
        }
    }).id('gtcsolo:metallurgic_infusing/control_circuit/basic')

})
