GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('gravity_tune_neutronium_event_horizon', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeType('gtneh')
          .recipeModifier(GTRecipeModifiers.OC_PERFECT)
     .appearanceBlock(() => Block.getBlock('gtceu:uhv_machine_casing'))
    .pattern(definition => FactoryBlockPattern.start()
        .aisle("XXAXX","XAZAX","AZZZA","XAZAX","XXAXX")
        .aisle("XAAAX","ABCBA","ZCCCZ","ABCBA","XAZAX")
        .aisle("AAAAA","ZCCCZ","ZCCCZ","ZCCCZ","AZZZA")
        .aisle("XAAAX","ABCBA","ZCCCZ","ABCBA","XAZAX")
        .aisle("XXAXX","XA#AX","AZZZA","XAZAX","XXAXX")

	    .where('A', Predicates.blocks('kubejs:gravity_tune_casing')
        .or(Predicates.autoAbilities(definition.getRecipeTypes()))
        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
      .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setExactLimit(2))
      )

      .where('B', Predicates.blocks('gtceu:atomic_casing'))
      .where('C', Predicates.blocks('gtceu:neutronium_block'))
      .where('Z', Predicates.blocks('gtceu:uhv_machine_casing'))

      // 空気
      .where('X', Predicates.air())

      // コア（controller）
      .where('#', Predicates.controller(Predicates.blocks(definition.getBlock())))

      .build()
    )
    .workableCasingRenderer(
     "kubejs:block/gravity_tune_casing",
      "gtceu:block/multiblock/electric_blast_furnace",
      false
    )
})