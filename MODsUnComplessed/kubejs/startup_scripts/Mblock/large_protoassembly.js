
GTCEuStartupEvents.registry('gtceu:machine', event => {
  event.create('large_protoassembly', 'multiblock')
    .rotationState(RotationState.NON_Y_AXIS)
    .recipeTypes(['assembler', 'prototype_assembler'])
     .recipeModifier(GTRecipeModifiers.OC_PERFECT)
     .recipeModifier(GTRecipeModifiers.PARALLEL_HATCH)
    .appearanceBlock(() => Block.getBlock('gtceu:large_scale_assembler_casing'))
    .pattern(definition => FactoryBlockPattern.start()
.aisle("AAAAAAAAAAAAAAAAA####","CBBBBBBBBBBBBBBBC####","CBBBBBBBBBBBBBBBC####","CBBBBBBBBBBBBBBBC####","#########AAA#########")
.aisle("AAAAAAAAAAAAAAAAA####","B###############B####","B#D###D###D###D#B####","B###############B####","AAAAAAAAAAAAAAAAA####")
.aisle("AAAAAAAAAAAAAAAAA####","B#D###D###D###D#B####","BDEEEEEEEEEEEEEDB####","B#D###D###D###D#B####","AAAAAAAAAAAAAAAAA####")
.aisle("AAAAAAAAAAAAAAAAA####","B###############B####","B#D###D###E###D#B####","B###############B####","AAAAAAAAAAAAAAAAA####")
.aisle("AAAAAAAAAAAAAAAAA####","CBBBBBBBC###CBBBC####","CBBBBBBBC#E#CBBBC####","CBBBBBBBC###CBBBC####","#########AAA#########")
.aisle("########AAAAA########","########BAAAB########","########BAEAB########","########BAAAB########","#########AAA#########")
.aisle("########AAAAA########","########BADAB########","########BDEDB########","########BADAB########","#########AAA#########")
.aisle("########AAAAA########","########BAAAB########","########BAEAB########","########BAAAB########","#########AAA#########")
.aisle("####AAAAAAAAAAAAAAAAA","####CBBBCAAACBBBBBBBC","####CBBBCAEACBBBBBBBC","####CBBBCAAACBBBBBBBC","#########AAA#########")
.aisle("####AAAAAAAAAAAAAAAAA","####B###############B","####B#D###E###D###D#B","####B###############B","####AAAAAAAAAAAAAAAAA")
.aisle("####AAAAAAAAAAAAAAAAA","####B#D###D###D###D#B","####BDEEEEEEEEEEEEEDB","####B#D###D###D###D#B","####AAAAAAAAAAAAAAAAA")
.aisle("####AAAAAAAAAAAAAAAAA","####B###############B","####B#D###D###D###D#B","####B###############B","####AAAAAAAAAAAAAAAAA")
.aisle("####AAAAAAXAAAAAAAAAA","####CBBBBBBBBBBBBBBBC","####CBBBBBBBBBBBBBBBC","####CBBBBBBBBBBBBBBBC","#########AAA#########")
      .where(
        'A',
        Predicates.blocks('gtceu:large_scale_assembler_casing')
          .or(Predicates.autoAbilities(definition.getRecipeTypes()))
          .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1).or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setExactLimit(8)))
          .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setExactLimit(1))
      )
      .where('B', Predicates.blocks('gtceu:laminated_glass'))
      .where('C', Predicates.blocks('gtceu:atomic_casing'))
      .where('D', Predicates.blocks('gtceu:high_power_casing'))
      .where('E', Predicates.blocks('gtceu:advanced_computer_casing'))
      .where('#', Predicates.air())
      .where('X', Predicates.controller(Predicates.blocks(definition.getBlock())))
      .build()
    )
      .workableCasingRenderer(
            "gtceu:block/casings/gcym/large_scale_assembling_casing", 
            "gtceu:block/machines/assembler", 
            false
        )
});