GTCEuStartupEvents.registry('gtceu:machine', event => {
  event.create('extended_electric_blast_furnace', 'multiblock')
    .rotationState(RotationState.NON_Y_AXIS)
    .recipeType('electric_blast_furnace')
    .recipeModifier(GTRecipeModifiers.OC_PERFECT)
    .appearanceBlock(() => Block.getBlock('gtceu:atomic_casing'))

    .pattern(definition => FactoryBlockPattern.start()

      .aisle("OOOAOOO", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "OOOAOOO")
      .aisle("OBBBBBO", "XEBBBEX", "XFFFFFX", "XFFFFFX", "XFFFFFX", "XEBBBEX", "OBBBBBO")
      .aisle("OBBBBBO", "XBXXXBX", "XFXXXFX", "XFXXXFX", "XFXXXFX", "XBXXXBX", "OBBBBBO")
      .aisle("ABBBBBO", "DBXDXBD", "DFXDXFD", "DFXDXFD", "DFXDXFD", "DBXDXBD", "ABBCBBO")
      .aisle("OBBBBBO", "XBXXXBX", "XFXXXFX", "XFXXXFX", "XFXXXFX", "XBXXXBX", "OBBBBBO")
      .aisle("OBBBBBO", "XEBBBEX", "XFFFFFX", "XFFFFFX", "XFFFFFX", "XEBBBEX", "OBBBBBO")
      .aisle("OOOYOOO", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "XXXDXXX", "OOOAOOO")

      // ★blocksdefinition は存在しない。ここが壊れてると pattern 構築が途中で死ぬ
      .where('Y', Predicates.controller(Predicates.blocks(definition.getBlock())))

      .where('O', Predicates.blocks('gtceu:robust_machine_casing')
        .or(Predicates.autoAbilities(definition.getRecipeTypes()))
        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1).or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setExactLimit(2)))
      )
      .where('A', Predicates.blocks('gtceu:extreme_engine_intake_casing'))
      .where('B', Predicates.blocks('gtceu:atomic_casing'))
      .where('C', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
      .where('D', Predicates.blocks('gtceu:tungstensteel_pipe_casing'))
      .where('E', Predicates.blocks('gtceu:tungstensteel_firebox_casing'))
      .where('F', Predicates.heatingCoils())
      .where('X', Predicates.any())
      .build()
    )
    .workableCasingRenderer(
      "gtceu:block/casings/gcym/atomic_casing",
      "gtceu:block/multiblock/electric_blast_furnace",
      false
    )
})
