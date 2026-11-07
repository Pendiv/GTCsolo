// priority: 100
(function () {
    var functional = global.mekGtModes.evaporation === 'gt';
    var HEIGHT = 18; // 最大外寸4×18×4。回路1の最大並列数もこの段数。
    var RD = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    var MF = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
    var CM = Java.loadClass('com.gregtechceu.gtceu.api.recipe.content.ContentModifier');
    var PL = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic');
    var Mods = Java.loadClass('com.gregtechceu.gtceu.common.data.GTRecipeModifiers');
    var Component = Java.loadClass('net.minecraft.network.chat.Component');
    StartupEvents.registry('fluid', function (event) {
        // MekanismFluids / ChemicalConstants RGB, and native translated names.
        event.create('kubejs:mek_gt_brine').thinTexture(0xFEEF9C).bucketColor(0xFEEF9C).noBlock()
            .displayName(Component.translatable('block.mekanism.brine'));
        event.create('kubejs:mek_gt_lithium').thinTexture(0xEBA400).bucketColor(0xEBA400).noBlock()
            .displayName(Component.translatable('block.mekanism.lithium'));
    });
    GTCEuStartupEvents.registry('gtceu:recipe_type', function (event) {
        event.create('mek_gt_evaporation').category('mek_gt_evaporation').setMaxIOSize(1, 0, 1, 1).setEUIO('in');
    });
    GTCEuStartupEvents.registry('gtceu:machine', function (event) {
        var builder = event.create('mek_gt_thermal_evaporation', 'multiblock').rotationState(RotationState.NON_Y_AXIS)
            .recipeType('mek_gt_evaporation')
            .recipeModifier(function (machine, recipe) {
                if (!functional) return MF.NULL;
                if (recipe.data.getInt('mek_gt_passive') === 1) {
                    var n = PL.getParallelAmount(machine, recipe, HEIGHT);
                    if (n < 1) return MF.NULL;
                    // No EU multiplier: absent EU contents must remain absent.
                    return MF.builder().modifyAllContents(CM.multiplier(n)).parallels(n).build();
                }
                // Standard GT OC: 4x EU/t, half duration; sub-tick duration becomes additional throughput.
                return Mods.OC_NON_PERFECT_SUBTICK.getModifier(machine, recipe);
            }, true)
            .appearanceBlock(function () { return Block.getBlock('mekanism:thermal_evaporation_block'); })
            .pattern(function (definition) {
                var face = global.mekGtPreviewBlocks('mekanism:structural_glass').or(global.mekGtPreviewBlocks('mekanism:thermal_evaporation_block'));
                if (functional) face = face.or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setExactLimit(1))
                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1))
                    // Optional so circuit 1 also works without an energy hatch.
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2));
                else face = face.or(global.mekGtPreviewBlocks('mekanism:thermal_evaporation_valve').setMinGlobalLimited(2));
                var pattern = FactoryBlockPattern.start(RD.RIGHT, RD.UP, RD.BACK);
                for (var z = 0; z < 4; z++) {
                    var rows = [];
                    for (var y = 0; y < HEIGHT; y++) {
                        var row = '';
                        for (var x = 0; x < 4; x++) {
                            var bx = x === 0 || x === 3, bz = z === 0 || z === 3;
                            var edge = Number(bx) + Number(y === 0 || y === HEIGHT - 1) + Number(bz);
                            var c = edge >= 2 ? 'F' : edge ? 'W' : '.';
                            if (y === HEIGHT - 1) c = bx && bz ? '?' : !bx && !bz ? '.' : 'W';
                            row += x === 1 && y === 1 && z === 0 ? 'C' : c;
                        }
                        rows.push(row);
                    }
                    pattern.aisle(rows);
                }
                return pattern.where('C', Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .where('F', global.mekGtPreviewBlocks('mekanism:thermal_evaporation_block')).where('W', face)
                    .where('.', Predicates.any()).where('?', Predicates.any()).build();
            });
        global.mekGtApplyModel(builder, 'mekanism:block/thermal_evaporation_block', 'gtceu:block/multiblock/distillation_tower');
    });
})();
