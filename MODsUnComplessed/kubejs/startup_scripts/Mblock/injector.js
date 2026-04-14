GTCEuStartupEvents.registry('gtceu:machine', event => {
  event.create('injector', 'multiblock')
    .rotationState(RotationState.NON_Y_AXIS)
    .recipeType('injection')
    .recipeModifier(GTRecipeModifiers.OC_PERFECT)
    .appearanceBlock(() => Block.getBlock('gtceu:atomic_casing'))
    .pattern(definition => FactoryBlockPattern.start()

      .aisle("OOO", "OCO", "OOO")
      .aisle("BFB", "FXF", "BFB")
      .aisle("OYO", "OOO", "OOO")

      // ★blocksdefinition は存在しない。ここが壊れてると pattern 構築が途中で死ぬ
      .where('Y', Predicates.controller(Predicates.blocks(definition.getBlock())))

      .where('O', Predicates.blocks('gtceu:heatproof_machine_casing')
        .or(Predicates.autoAbilities(definition.getRecipeTypes()))
        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1).or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setExactLimit(2)))
      )
      .where('B', Predicates.blocks('gtceu:steel_pipe_casing'))
      .where('C', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
      .where('F', Predicates.heatingCoils())
      .where('X', Predicates.any())
      .build()
    )
    .workableCasingRenderer(
      "gtceu:block/casings/solid/machine_casing_heatproof",
      "gtceu:block/multiblock/injector",
      false
    )
})
