ServerEvents.recipes(event => {

  if (!Platform.isLoaded('evolvedmekanism')) return;

  event.custom({
    type: 'evolvedmekanism:apt',
    chemicalInput: { amount: 288000, gas: 'gtcsolo:neutronium' },
    itemInput:     { ingredient: { item: 'gtcsolo:stellarium_block' } },
    output:        { item: 'gtcsolo:hyperx_neutronium_block', count: 1 }
  }).id('kubejs:apt/hyperneutronium');

});
