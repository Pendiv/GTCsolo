ServerEvents.recipes(event => {

  event.campfireCooking(
    'gtceu:wrought_iron_ingot',
    '#forge:ingots/iron'
  )
  .cookingTime(600)
  .xp(0.1)
  .id('kubejs:campfire/wrought_iron_from_iron_ingot')

})
ServerEvents.recipes(event => {
  event.remove({ output: 'minecraft:campfire', type: 'minecraft:crafting_shaped' })
  event.shaped('minecraft:campfire', [
    'ISI',
    'SCS',
    'LLL'
  ], {
    I: 'gtceu:wrought_iron_ingot',
    S: '#forge:rods/wooden',
    C: Ingredient.of(['minecraft:coal', 'minecraft:charcoal']),
    L: '#minecraft:logs'
  })
  .id('kubejs:crafting/campfire_with_wrought_iron')

})