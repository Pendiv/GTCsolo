// Generated from stone_gen.txt by scripts/generate-stone-generator.py.
(function () {
    const entry = {
    "aisles": [
        [
            "AAAASAA",
            "AAAAAAA",
            "AAAACAA",
            "AAAAAAA"
        ],
        [
            "AAAAEAA",
            "AAAHFAA",
            "AHHHGBA",
            "ACCCFCA"
        ],
        [
            "AAAAEAA",
            "AAAHFAA",
            "AHHHGBA",
            "ACCCFCA"
        ],
        [
            "AAAAEAA",
            "AAAHFAA",
            "AHHHGBA",
            "ACCCFCA"
        ],
        [
            "AAAAEAA",
            "AAAHFAA",
            "AHHHGBA",
            "ACCCFCA"
        ],
        [
            "AAAAEAA",
            "AAAHFAA",
            "AHHHGBA",
            "ACCCFCA"
        ],
        [
            "AAAAEAA",
            "AAAAAAA",
            "AAAAAAA",
            "AAAAAAA"
        ]
    ],
    "blocks": {
        "A": "gtcsolo:nimonic_80a_heatproof_casing",
        "B": "minecraft:lava",
        "C": "gtceu:tempered_glass",
        "E": "gtcsolo:sus316l_pipe_casing",
        "F": "gtcsolo:hyunoy_gearbox",
        "G": "minecraft:cobblestone",
        "H": "minecraft:water"
    },
    "main_block": "gtcsolo:nimonic_80a_heatproof_casing",
    "min_casings": 77
};
    let Machine;
    let Renderer;
    try {
        Machine = Java.loadClass('DIV.gtcsolo.machine.IndustrialRockBreakerMachine');
        Renderer = Java.loadClass('DIV.gtcsolo.render.IndustrialRockBreakerRenderer');
    } catch (error) {
        console.info('[Industrial rock breaker] Build the updated addon before the next restart; registration deferred.');
        return;
    }
    GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
        event.create('advanced_rock_breaker').category('multiblock')
            .setMaxIOSize(1, 4, 0, 0).setEUIO('in')
            .setProgressBar(GuiTextures.PROGRESS_BAR_MACERATE, FillDirection.LEFT_TO_RIGHT);
    });
    GTCEuStartupEvents.registry('gtceu:machine', event => {
        const NativeMachines = Java.loadClass('com.gregtechceu.gtceu.common.data.GTMachines');
        const Direction = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
        const Condition = Java.loadClass('com.gregtechceu.gtceu.common.recipe.condition.RockBreakerCondition');
        const Rules = Java.loadClass('DIV.gtcsolo.machine.StoneGeneratorRules');
        GTRecipeTypes.get('advanced_rock_breaker')
            .prepareBuilder(builder => builder.addCondition(Condition.INSTANCE))
            .addDataInfo(data => Rules.environmentDescription(data));
        event.create('industrial_rock_breaker', 'multiblock')
            .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
            .machine(holder => new Machine(holder))
            .recipeTypes([GTRecipeTypes.get('rock_breaker'), GTRecipeTypes.get('advanced_rock_breaker')])
            .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT])
            .appearanceBlock(() => Block.getBlock(entry.main_block))
            .pattern(definition => {
                let pattern = FactoryBlockPattern.start(Direction.RIGHT, Direction.UP, Direction.BACK);
                entry.aisles.forEach(rows => { pattern = pattern.aisle(rows); });
                pattern = pattern.where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .where('A', Predicates.blocks(entry.main_block).setMinGlobalLimited(entry.min_casings)
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                        .or(Predicates.blocks(NativeMachines.FLUID_IMPORT_HATCH[GTValues.ULV].getBlock())
                            .setMaxGlobalLimited(2).setPreviewCount(2))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                        .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)));
                Object.keys(entry.blocks).forEach(symbol => {
                    // Fluid and product cells are visual volumes, not construction materials or forced air.
                    if (['B', 'G', 'H'].indexOf(symbol) >= 0) pattern = pattern.where(symbol, Predicates.any());
                    else if (symbol !== 'A') pattern = pattern.where(symbol, Predicates.blocks(entry.blocks[symbol]));
                });
                return pattern.build();
            })
            .renderer(() => new Renderer()).hasTESR(true)
            .tooltips([Component.translatable('gtceu.machine.available_recipe_map_2.tooltip',
                Component.translatable('gtceu.rock_breaker'), Component.translatable('gtceu.advanced_rock_breaker')),
                Component.translatable('gtcsolo.jei.introduction.details_hint')]);
    });
})();
