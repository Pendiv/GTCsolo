// Authored structure: run/cmdex/export/singularity_reconstruction.txt (11 x 5 x 11).
// The front-bottom central casing is the controller. Export rows are bottom to top.
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('singularity_reconstruction')
        .category('multiblock')
        .setMaxIOSize(100, 1, 0, 0)
        .setEUIO('in')
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT);
});

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const RelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    event.create('singularity_reconstruction_machine', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS)
        .allowExtendedFacing(false).allowFlip(false)
        .recipeTypes([GTRecipeTypes.get('singularity_reconstruction')])
        .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtcsolo:mithrilite_robust_machine_casing'))
        .pattern(definition => FactoryBlockPattern
            .start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            .aisle('###AASAA###', '###ABBBA###', '###ABBBA###', '###ABBBA###', '###AAAAA###')
            .aisle('###AAAAA###', '###B###B###', '###B###B###', '###B###B###', '###AAAAA###')
            .aisle('##CAAAAAC##', '##B#DDD#B##', '##B#DDD#B##', '##B#DDD#B##', '##CAAAAAC##')
            .aisle('AAAEEEEEAAA', 'AB#D###D#BA', 'AB#D###D#BA', 'AB#D###D#BA', 'AAAEEEEEAAA')
            .aisle('AAAEEEEEAAA', 'B#D#####D#B', 'B#D#####D#B', 'B#D#####D#B', 'AAAEEEEEAAA')
            .aisle('AAAEEEEEAAA', 'B#D#####D#B', 'B#D#####D#B', 'B#D#####D#B', 'AAAEEEEEAAA')
            .aisle('AAAEEEEEAAA', 'B#D#####D#B', 'B#D#####D#B', 'B#D#####D#B', 'AAAEEEEEAAA')
            .aisle('AAAEEEEEAAA', 'AB#D###D#BA', 'AB#D###D#BA', 'AB#D###D#BA', 'AAAEEEEEAAA')
            .aisle('##CAAAAAC##', '##B#DDD#B##', '##B#DDD#B##', '##B#DDD#B##', '##CAAAAAC##')
            .aisle('###AAAAA###', '###B###B###', '###B###B###', '###B###B###', '###AAAAA###')
            .aisle('###AAAAA###', '###ABBBA###', '###ABBBA###', '###ABBBA###', '###AAAAA###')
            .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
            .where('A', Predicates.blocks('gtcsolo:mithrilite_robust_machine_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(2))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)))
            .where('B', Predicates.blocks('gtcsolo:voltage_glass_opv'))
            .where('C', Predicates.blocks('gtcsolo:mithrilite_firebox_casing'))
            .where('D', Predicates.blocks('gtcsolo:draconium_coil_block'))
            .where('E', Predicates.blocks('gtceu:atomic_casing'))
            .where('#', Predicates.any())
            .build())
        .workableCasingRenderer('gtcsolo:block/casings/solid/machine_casing_robust_mithrilite',
            'gtceu:block/multiblock/implosion_compressor', false)
        .tooltips([Component.translatable('gtceu.machine.available_recipe_map_1.tooltip',
            Component.translatable('gtceu.singularity_reconstruction'))]);
});
