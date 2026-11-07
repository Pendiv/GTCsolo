GTCEuStartupEvents.registry('gtceu:machine', event => {
  event.create('infusion_gen', 'multiblock')
    .rotationState(RotationState.NON_Y_AXIS)
    .generator(true)
    .recipeType('infusion_gen')
    .appearanceBlock(() => Block.getBlock('gtceu:heatproof_machine_casing'))
    .pattern(definition => FactoryBlockPattern.start()
      .aisle('AAA', 'CCC', 'AAA')
      .aisle('AAA', 'CDC', 'AAA')
      .aisle('AXA', 'CCC', 'AAA')
      .where('X', Predicates.controller(Predicates.blocks(definition.getBlock())))
      .where('A', Predicates.blocks('gtceu:heatproof_machine_casing')
          .or(Predicates.autoAbilities(definition.getRecipeTypes()))
          .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1))
          .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))

      .where('C', Predicates.blocks('gtceu:solid_machine_casing'))
      .where('D', Predicates.blocks('gtceu:steel_pipe_casing'))
      .build()
    )
    .workableCasingRenderer(
      'gtceu:block/casings/solid/machine_casing_heatproof',
      'gtceu:block/multiblock/electric_blast_furnace',
      false
    )
})



