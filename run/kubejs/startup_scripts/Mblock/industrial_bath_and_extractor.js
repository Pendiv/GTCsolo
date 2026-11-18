GTCEuStartupEvents.registry('gtceu:machine', event => {
    const Direction = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    const Industrial = Java.loadClass('DIV.gtcsolo.util.GtcsoloRecipeModifiers');
    const ModifierFunction = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
    const ContentModifier = Java.loadClass('com.gregtechceu.gtceu.api.recipe.content.ContentModifier');

    function bathOverclock(machine, recipe) {
        const industrial = Industrial.industrialOverclock(machine, recipe);
        const adjusted = industrial.apply(recipe);
        if (adjusted === null) return ModifierFunction.NULL;
        const stages = adjusted.ocLevel - recipe.ocLevel;
        const duration = Math.max(1, Math.floor(recipe.duration * 0.85 *
            Math.pow(Industrial.INDUSTRIAL_DURATION_FACTOR, stages)));
        return industrial.andThen(ModifierFunction.builder()
            .durationModifier(ContentModifier.addition(duration - adjusted.duration))
            .build());
    }

    const extractionDurationMultiplier = 4;
    const maxExtractionRecipeTicks = Math.floor(0x7fffffff / extractionDurationMultiplier);
    const extractionBatch = Industrial.inputOutputBonus(16)
        .durationMultiplier(extractionDurationMultiplier)
        .build();

    function poweredCasing(block) {
        return Predicates.blocks(block)
            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
            .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false));
    }

    event.create('industrial_chemical_bath', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false)
        .recipeTypes([GTRecipeTypes.get('chemical_bath')])
        .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, bathOverclock])
        .appearanceBlock(() => Block.getBlock('gtceu:watertight_casing'))
        .pattern(definition => FactoryBlockPattern.start(Direction.RIGHT, Direction.UP, Direction.BACK)
            .aisle('##AAA##', '##ASA##', '##AAA##', '##AAA##', '###B###')
            .aisle('#AAAAA#', '#AC#CA#', '#A###A#', '#A###A#', '###B###')
            .aisle('AAAAAAA', 'AC###CA', 'A##D##A', 'A#####A', '###B###')
            .aisle('AAAAAAA', 'A#DDD#A', 'A##D##A', 'A#DDD#A', 'BBBDBBB')
            .aisle('AAAAAAA', 'AC###CA', 'A##D##A', 'A#####A', '###B###')
            .aisle('#AAAAA#', '#AC#CA#', '#A###A#', '#A###A#', '###B###')
            .aisle('##AAA##', '##AAA##', '##AAA##', '##AAA##', '###B###')
            .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
            .where('A', poweredCasing('gtceu:watertight_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1)))
            .where('B', Predicates.blocks('gtceu:naquadah_alloy_frame'))
            .where('C', Predicates.blocks('gtceu:titanium_pipe_casing'))
            .where('D', Predicates.blocks('gtceu:naquadah_alloy_block'))
            .where('#', Predicates.any())
            .build())
        .workableCasingRenderer('gtceu:block/casings/gcym/watertight_casing',
            'gtceu:block/multiblock/gcym/large_chemical_bath', false);

    event.create('heating_chamber', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false)
        .recipeTypes([GTRecipeTypes.get('extractor')])
        .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
            (machine, recipe) => recipe.duration <= maxExtractionRecipeTicks ? extractionBatch : ModifierFunction.NULL,
            GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtceu:stable_machine_casing'))
        .pattern(definition => FactoryBlockPattern.start(Direction.BACK, Direction.UP, Direction.RIGHT)
            .aisle('AAAAA', 'AAAAA', 'AAAAA', 'AAAAA', 'AAAAA')
            .aisle('AAAAA', 'A###A', 'B###B', 'A###A', '#AAA#')
            .aisle('AAAAA', 'A#C#A', 'B###B', 'A#C#A', '#AAA#')
            .aisle('AAAAA', 'A###A', 'B###B', 'A###A', '#AAA#')
            .aisle('AAAAA', 'S#C#A', 'B###B', 'A#C#A', '#AAA#')
            .aisle('AAAAA', 'A###A', 'B###B', 'A###A', '#AAA#')
            .aisle('AAAAA', 'A#C#A', 'B###B', 'A#C#A', '#AAA#')
            .aisle('AAAAA', 'A###A', 'B###B', 'A###A', '#AAA#')
            .aisle('AAAAA', 'AAAAA', 'AAAAA', 'AAAAA', 'AAAAA')
            .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
            .where('A', poweredCasing('gtceu:stable_machine_casing')
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1)))
            .where('B', Predicates.blocks('gtceu:tempered_glass'))
            .where('C', Predicates.blocks('gtceu:titanium_pipe_casing'))
            .where('#', Predicates.any())
            .build())
        .workableCasingRenderer('gtceu:block/casings/solid/machine_casing_stable_titanium',
            'gtceu:block/multiblock/gcym/large_extractor', false);
});
