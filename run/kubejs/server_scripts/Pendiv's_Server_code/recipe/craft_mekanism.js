ServerEvents.recipes(event => {

    event.remove({ id: 'mekanism:configurator' })
    event.shaped('mekanism:configurator', [
        ' B ',
        'RCR',
        ' S '
    ], {
        B: 'gtceu:blue_alloy_ingot',
        R: 'gtceu:red_alloy_ingot',
        C: 'minecraft:copper_ingot',
        S: 'minecraft:stick'
    })

    event.remove({ id: 'mekanismgenerators:generator/heat' })
    event.shaped('mekanismgenerators:heat_generator', [
        'III',
        'WOW',
        'CFC'
    ], {
        I: 'gtceu:iron_plate',
        W: '#minecraft:planks',
        O: '#forge:ingots/osmium',
        C: 'gtceu:copper_plate',
        F: 'minecraft:furnace'
    })

    event.remove({ id: 'mekanismgenerators:generator/wind' })
    event.shaped('mekanismgenerators:wind_generator', [
        'AOA',
        'OLO',
        'BEB'
    ], {
        A: 'mekanism:alloy_infused',
        O: 'gtceu:osmium_plate',
        L: 'gtceu:basic_electronic_circuit',
        B: 'gtceu:basic_integrated_circuit',
        E: 'mekanism:energy_tablet'
    })

})