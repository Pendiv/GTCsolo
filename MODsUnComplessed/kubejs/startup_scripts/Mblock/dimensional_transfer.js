GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('dimensional_transfer', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS)
        .appearanceBlock(() => Block.getBlock('gtceu:large_scale_assembler_casing'))
        .recipeType('dimension_transfer')
         .recipeModifier(GTRecipeModifiers.PARALLEL_HATCH)
            .recipeModifier(GTRecipeModifiers.OC_PERFECT)
        .pattern(definition => FactoryBlockPattern.start()
      .aisle("OOO", "OOO", "OOO")
      .aisle("OOO", "OOO", "OOO")
      .aisle("OOO", "OOO", "OOO")
      .aisle("OOO", "OOO", "OOO")
      .aisle("OOO", "OOO", "OOO")
      .aisle("OOO", "OOO", "OOO")
      .aisle("OOO", "OOO", "OOO")
      .aisle("OOO", "OOO", "OOO")
      .aisle("OXO", "OOO", "OOO")
            .where('X',Predicates.controller(Predicates.blocks(definition.getBlock())))
            .where('O', Predicates.blocks('gtceu:large_scale_assembler_casing')
                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setExactLimit(1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setExactLimit(2))
            )
            .build()
        )
        .workableCasingRenderer(
                  "gtceu:block/casings/solid/machine_casing_clean_stainless_steel",
            "gtceu:block/machines/assembler",
            false
        )
});