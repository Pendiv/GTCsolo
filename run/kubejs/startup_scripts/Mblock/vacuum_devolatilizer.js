// Temporary 5x5x5 chamber; replace only the pattern when the authored aisle arrives.
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('vacuum_devolatilization')
        .category('multiblock').setMaxIOSize(2, 2, 2, 3).setEUIO('in')
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT);
});

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const Direction = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    event.create('vacuum_devolatilizer', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
        .recipeTypes([GTRecipeTypes.get('vacuum_devolatilization')])
        .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:inert_machine_casing'))
        .pattern(definition => FactoryBlockPattern
            // Rows run bottom to top; the chamber extends behind the controller.
            .start(Direction.RIGHT, Direction.UP, Direction.BACK)
            .aisle('CCSCC', 'CGGGC', 'CGGGC', 'CGGGC', 'CCCCC')
            .aisle('CCCCC', 'CVVVC', 'CVVVC', 'CVVVC', 'CCCCC')
            .aisle('CCCCC', 'CVVVC', 'CVVVC', 'CVVVC', 'CCCCC')
            .aisle('CCCCC', 'CVVVC', 'CVVVC', 'CVVVC', 'CCCCC')
            .aisle('CCCCC', 'CCPCC', 'CCCCC', 'CCPCC', 'CCCCC')
            .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
            .where('C', Predicates.blocks('gtceu:inert_machine_casing').setMinGlobalLimited(48)
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(3))
                .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)))
            .where('G', Predicates.blocks('gtceu:laminated_glass'))
            .where('P', Predicates.blocks('gtceu:titanium_pipe_casing'))
            .where('V', Predicates.any())
            .build())
        .workableCasingRenderer('gtceu:block/casings/solid/machine_casing_inert_ptfe',
            'gtceu:block/multiblock/vacuum_freezer', false)
        .tooltips([Component.translatable('gtceu.machine.available_recipe_map_1.tooltip',
            Component.translatable('gtceu.vacuum_devolatilization')),
            Component.translatable('gtcsolo.jei.introduction.details_hint')]);
});
