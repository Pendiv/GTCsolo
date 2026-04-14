GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('local_transfer', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS)
        .appearanceBlock(() => Block.getBlock('gtceu:clean_machine_casing'))
        .recipeType('local_transfer')
  
        .pattern(definition => FactoryBlockPattern.start()
            .aisle("AAAAAAAAAAAA","B##########B","B##########B","B##########B","BBBBBBBBBBBB","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","BBBBBBBBBBBB")
            .aisle("AAAAAAAAAAAA","#DAAAAAAAAD#","############","############","BDCCCCCCCCDB","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#A########A#","############","############","BCCCCCCCCCCB","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","#C########C#","BCCCCCCCCCCB")
            .aisle("AAAAAAAAAAAA","#DAAAAAAAAD#","############","############","BDCCCCCCCCDB","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","#CCCCCCCCCC#","BCCCCCCCCCCB")
            .aisle("AAAAAXAAAAAA","B##########B","B##########B","B##########B","BBBBBBBBBBBB","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","B##########B","BBBBBBBBBBBB")
            .where('X', Predicates.controller(Predicates.blocks(definition.get())))
            
            .where('A',Predicates.blocks('gtceu:clean_machine_casing')
                   .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                   .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
            .where('B', Predicates.blocks('gtceu:monel_frame'))
            .where('C', Predicates.blocks('kubejs:dimensional_tearresistant_casing'))
            .where('D', Predicates.blocks('kubejs:local_trasfer_core_block'))
            .where('#', Predicates.any())

            .build()
        )
        .workableCasingRenderer(
            "gtceu:block/casings/solid/machine_casing_clean_stainless_steel",
            "gtceu:block/machines/assembler",
            false
        )
});
