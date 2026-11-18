GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('rare_mineral_refining').category('multiblock')
        .setMaxIOSize(3, 3, 2, 2).setEUIO('in')
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT);
});

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const Direction = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    const casing = 'gtceu:clean_machine_casing';
    event.create('rare_mineral_refinery', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
        .recipeType('rare_mineral_refining')
        .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock(casing))
        .pattern(definition => FactoryBlockPattern.start(Direction.RIGHT, Direction.UP, Direction.BACK)
            .aisle('AAA', 'ASA', 'AAA')
            .aisle('AAA', 'A#A', 'AAA')
            .aisle('AAA', 'AAA', 'AAA')
            .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
            .where('A', Predicates.blocks(casing).setMinGlobalLimited(15)
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)))
            .where('#', Predicates.any())
            .build())
        .workableCasingRenderer('gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
            'gtceu:block/machines/centrifuge', false);
});
