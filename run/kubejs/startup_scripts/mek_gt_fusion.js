// priority: 100
// MC 1.20.1 / Forge / GTCEu 1.6.4 / KubeJS 6 / Mekanism + Generators 10.4.x.
// Independent of GTCsolo. Processing recipes live in server_scripts/mek_gt_recipes.js.
(function () {
    var functional = global.mekGtModes.fusion === 'gt';
    var Component = Java.loadClass('net.minecraft.network.chat.Component');
    var MF = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
    var CM = Java.loadClass('com.gregtechceu.gtceu.api.recipe.content.ContentModifier');
    var PL = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic');
    var RH = Java.loadClass('com.gregtechceu.gtceu.api.recipe.RecipeHelper');
    var RD = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');

    StartupEvents.registry('fluid', function (event) {
        // RGB values are from GeneratorsGases / GeneratorsChemicalConstants.
        [
            ['deuterium', 0xFF3232, 'gas.mekanismgenerators.deuterium'],
            ['tritium', 0x64FF70, 'gas.mekanismgenerators.tritium'],
            ['fusion_fuel', 0x7E007D, 'gas.mekanismgenerators.fusion_fuel']
        ].forEach(function (entry) {
            event.create('kubejs:mek_gt_' + entry[0])
                .thinTexture(entry[1]).bucketColor(entry[1]).noBlock()
                .displayName(Component.translatable(entry[2]));
        });
        // Mek steam has an untinted custom texture rather than a flat RGB color.
        event.create('kubejs:mek_gt_steam')
            .stillTexture('mekanism:liquid/steam').flowingTexture('mekanism:liquid/steam')
            .color(0xFFFFFF).bucketColor(0xFFFFFF).noBlock()
            .displayName(Component.translatable('gas.mekanism.steam'));
    });

    GTCEuStartupEvents.registry('gtceu:recipe_type', function (event) {
        event.create('mek_gt_fusion').category('mek_gt_fusion')
            .setMaxIOSize(1, 0, 3, 1).setEUIO('out');
    });

    GTCEuStartupEvents.registry('gtceu:machine', function (event) {
        var builder = event.create('mek_gt_fusion_reactor', 'multiblock')
            .rotationState(RotationState.NON_Y_AXIS)
            .generator(true)
            .recipeType('mek_gt_fusion')
            .recipeModifier(function (machine, recipe) {
                if (!functional) return MF.NULL;
                var cap = recipe.data.getInt('mek_gt_parallel_limit');
                if (cap <= 0) return MF.NULL;
                // Keep the output within the installed dynamo's available voltage envelope.
                var perRecipeEU = Math.abs(RH.getRealEUt(recipe));
                if (perRecipeEU <= 0) return MF.NULL;
                cap = Math.min(cap, Math.floor(machine.getOverclockVoltage() / perRecipeEU));
                if (cap < 1) return MF.NULL;
                var parallels = PL.getParallelAmount(machine, recipe, cap);
                if (parallels < 1) return MF.NULL;
                return MF.builder().modifyAllContents(CM.multiplier(parallels))
                    .eutMultiplier(parallels).parallels(parallels).build();
            }, true)
            .appearanceBlock(function () { return Block.getBlock('mekanismgenerators:fusion_reactor_frame'); })
            .pattern(function (definition) {
                var face = global.mekGtPreviewBlocks('mekanismgenerators:reactor_glass')
                    .or(global.mekGtPreviewBlocks('mekanismgenerators:fusion_reactor_frame'));
                if (functional) face = face.or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setMaxGlobalLimited(3))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setExactLimit(1))
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setExactLimit(1))
                    .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(4));
                else face = face.or(global.mekGtPreviewBlocks('mekanismgenerators:fusion_reactor_port').setMinGlobalLimited(2));
                // Axes explicitly match blueprints/fusion_reactor.json: z aisles, y bottom-up, x rows.
                return FactoryBlockPattern.start(RD.RIGHT, RD.UP, RD.BACK)
                    .aisle('??F??', '?FWF?', 'FWWWF', '?FWF?', '??F??')
                    .aisle('?FWF?', 'F...F', 'W...W', 'F...F', '?FWF?')
                    .aisle('FWWWF', 'W...W', 'W...W', 'W...W', 'FWCWF')
                    .aisle('?FWF?', 'F...F', 'W...W', 'F...F', '?FWF?')
                    .aisle('??F??', '?FWF?', 'FWWWF', '?FWF?', '??F??')
                    .where('C', Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .where('F', global.mekGtPreviewBlocks('mekanismgenerators:fusion_reactor_frame'))
                    .where('W', face).where('.', Predicates.any()).where('?', Predicates.any())
                    .build();
            })
            ;
        global.mekGtApplyModel(builder, 'mekanismgenerators:block/reactor_frame', 'gtceu:block/multiblock/fusion_reactor');
    });
})();
