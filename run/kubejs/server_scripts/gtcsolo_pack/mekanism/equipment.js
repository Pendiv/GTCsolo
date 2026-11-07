ServerEvents.recipes(event => {

    event.remove({ id: 'mekanism:energy_tablet' })
    event.shaped('mekanism:energy_tablet', [
        'DPD',
        'ASA',
        'DPD'
    ], {
        D: 'gtceu:red_alloy_dust',
        P: 'gtceu:gold_plate',
        A: '#mekanism:alloys/infused',
        S: 'gtceu:double_steel_plate'
    }).id('gtcsolo:recipe_mod/mekanism_energy_tablet')

    event.remove({ id: 'mekanism:fluid_tank/basic' })
    event.shaped('mekanism:basic_fluid_tank', [
        'APA',
        'PHP',
        'APA'
    ], {
        A: '#mekanism:alloys/basic',
        P: '#forge:plates/iron',
        H: '#forge:tools/hammers'
    }).damageIngredient('#forge:tools/hammers')
      .id('gtcsolo:recipe_mod/mekanism_basic_fluid_tank')

    event.remove({ id: 'mekanism:chemical_tank/basic' })
    event.shaped(
        'mekanism:basic_chemical_tank',
        ['ABA', 'BCB', 'ABA'],
        {A: 'minecraft:redstone', B: '#forge:plates/osmium', C: Item.of('gtceu:aluminium_hammer', '{GT.Behaviours:{},GT.Tool:{AttackDamage:8.5f,AttackSpeed:-2.8f,Damage:0,HarvestLevel:2,MaxDamage:768,ToolSpeed:10.0f}}').strongNBT()}
    ).id('gtcsolo:recipe_mod/mekanism_basic_chemical_tank')

})
