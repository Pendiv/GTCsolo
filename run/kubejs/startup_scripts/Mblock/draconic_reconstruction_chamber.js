// Structure: cmdex/export/draconic_reconstruction_alter.txt, 2026-10-20 13:41.
// Dedicated processing and special performance will be defined separately.
GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    event.create('draconic_reconstruction')
        .category('multiblock')
        .setMaxIOSize(9, 3, 3, 3)
        .setEUIO('in')
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT);
});

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const RelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    event.create('draconic_reconstruction_chamber', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS)
        .allowExtendedFacing(false)
        .allowFlip(false)
        .recipeType('draconic_reconstruction')
        .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, GTRecipeModifiers.OC_NON_PERFECT])
        .appearanceBlock(() => Block.getBlock('gtcsolo:draconic_steel_casing'))
        .pattern(definition => FactoryBlockPattern
            // +aisle goes behind the core. Export rows run bottom to top.
            .start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            // S replaces one bottom casing directly below the front high-power casing.
            .aisle('#####AASAA#####','#######B#######','###############','###############','###############','###############','###############','#######B#######','#####AAAAA#####')
            .aisle('#AAAAAAAAAAAAA#','#####CCBCC#####','######DDD######','######D#D######','###############','######D#D######','#####EDDDE#####','#####CCBCC#####','#AAAAAAAAAAAAA#')
            .aisle('#AAAAAAAAAAAAA#','####BBBBBBB####','######EDE######','######EDE######','######EFE######','######EDE######','#####EEDEE#####','####BBBBBBB####','#AAAAAAAAAAAAA#')
            .aisle('#AAAAAAAAAAAAA#','#####CCBCC#####','######DDD######','######D#D######','###############','######D#D######','#####EDDDE#####','#####CCBCC#####','#AAAAAAAAAAAAA#')
            .aisle('#AAAAAAAAAAAAA#','##B####B####B##','###############','###############','###############','###############','###############','##B##E#B#E##B##','#AAAAAAAAAAAAA#')
            .aisle('AAAAAAAAAAAAAAA','#CBC#EEBEE#CBC#','#####E###E#####','#####E###E#####','#####E###E#####','#####E###E#####','#EEE#E###E#EEE#','#CBCEEEBEEECBC#','AAAAAAAAAAAAAAA')
            .aisle('AAAAAAAAAAAAAAA','#CBC#EABAE#CBC#','#DED##GGG##DED#','#DED##GGG##DED#','##E###GGG###E##','#DED##GGG##DED#','#DED##GGG##DED#','#CBC#EAAAE#CBC#','AAAAAAAAAAAAAAA')
            .aisle('AAAAAAAAAAAAAAA','BBBBBBBBBBBBBBB','#DDD##G#G##DDD#','##D###G#G###D##','##F###G#G###F##','##D###G#G###D##','#DDD##G#G##DDD#','BBBBBBA#ABBBBBB','AAAAAAAHAAAAAAA')
            .aisle('AAAAAAAAAAAAAAA','#CBC#EABAE#CBC#','#DED##GGG##DDD#','#DED##GGG##DED#','##E###GGG###E##','#DED##GGG##DED#','#DED##GGG##DED#','#CBC#EAAAE#CBC#','AAAAAAAAAAAAAAA')
            .aisle('AAAAAAAAAAAAAAA','#CBC#EEBEE#CBC#','#####E###E#####','#####E###E#####','#####E###E#####','#####E###E#####','#EEE#E###E#EEE#','#CBCEEEBEEECB##','AAAAAAAAAAAAAAA')
            .aisle('#AAAAAAAAAAAAA#','##B####B####B##','###############','###############','###############','###############','###############','##B##E#B#E##B##','#AAAAAAAAAAAAA#')
            .aisle('#AAAAAAAAAAAAA#','#####CCBCC#####','######DDD######','######D#D######','###############','######D#D######','#####EDDDE#####','#####CCBCC#####','#AAAAAAAAAAAAA#')
            .aisle('#AAAAAAAAAAAAA#','####BBBBBBB####','######EDE######','######EDE######','######EFE######','######EDE######','#####EEDEE#####','####BBBBBBB####','#AAAAAAAAAAAAA#')
            .aisle('#AAAAAAAAAAAAA#','#####CCBCC#####','######DDD######','######D#D######','###############','######D#D######','#####EDDDE#####','#####CCBCC#####','#AAAAAAAAAAAAA#')
            .aisle('#####AAAAA#####','#######B#######','###############','###############','###############','###############','###############','#######B#######','#####AAAAA#####')
            .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
            .where('A', Predicates.blocks('gtcsolo:draconic_steel_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)))
            .where('B', Predicates.blocks('gtceu:high_power_casing'))
            .where('C', Predicates.blocks('gtceu:atomic_casing'))
            .where('D', Predicates.blocks('draconicevolution:draconium_block'))
            .where('E', Predicates.blocks('gtcsolo:draconium_frame'))
            .where('F', Predicates.blocks('draconicevolution:awakened_draconium_block'))
            .where('G', Predicates.blocks('gtcsolo:draconium_coil_block'))
            .where('H', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
            // Empty areas in the export are unreserved, not sealed-air requirements.
            .where('#', Predicates.any())
            .build())
        .workableCasingRenderer(
            'gtcsolo:block/casings/solid/machine_casing_extreme_temperature_draconic_steel',
            'gtceu:block/multiblock/implosion_compressor', false);
});
