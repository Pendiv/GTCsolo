ServerEvents.recipes(event => {

  
event.custom({
    type: 'evolvedmekanism:apt',
    chemicalInput: { amount: 288000, gas: 'kubejs:neutronium' },
    itemInput:     { ingredient: { item: 'gtceu:stellarium_block' } },
    output:        { item: 'gtceu:hyperx_neutronium_block', count: 1 }
  }).id('kubejs:apt/hyperneutronium');

});