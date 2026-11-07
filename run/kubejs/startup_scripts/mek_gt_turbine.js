// priority: 100
(function () {
    var functional = global.mekGtModes.turbine === 'gt';
    var Rotor = Java.loadClass('mekanism.generators.common.tile.turbine.TileEntityTurbineRotor');
    var List = Java.loadClass('java.util.ArrayList');
    var MF = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
    var CM = Java.loadClass('com.gregtechceu.gtceu.api.recipe.content.ContentModifier');
    var PL = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic');
    var RD = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    GTCEuStartupEvents.registry('gtceu:recipe_type', function (event) {
        event.create('mek_gt_turbine').category('mek_gt_turbine').setMaxIOSize(0, 0, 1, 1).setEUIO('out');
    });
    GTCEuStartupEvents.registry('gtceu:machine', function (event) {
        var builder = event.create('mek_gt_industrial_turbine', 'multiblock').rotationState(RotationState.NON_Y_AXIS)
            .generator(true).recipeType('mek_gt_turbine')
            .recipeModifier(function (machine, recipe) {
                if (!functional) return MF.NULL;
                var rotors = machine.getMultiblockState().getMatchContext().get('mek_gt_rotors');
                if (!rotors || rotors.size() !== 14) return MF.NULL;
                // Recipes last one tick: blade-only edits are checked at every operation.
                for (var r = 0; r < rotors.size(); r++) {
                    var rotor = rotors.get(r);
                    if (rotor.isRemoved() || rotor.getHousedBlades() !== 2) return MF.NULL;
                }
                // 345 vents * 32000 mB/t; recipe unit = 1000 mB, 1000 EU, 1 tick.
                var cap = Math.min(11040, Math.floor(machine.getOverclockVoltage() / 1000));
                if (cap < 1) return MF.NULL;
                var n = PL.getParallelAmount(machine, recipe, cap);
                if (n < 1) return MF.NULL;
                return MF.builder().modifyAllContents(CM.multiplier(n)).eutMultiplier(n).parallels(n).build();
            }, true)
            .appearanceBlock(function () { return Block.getBlock('mekanismgenerators:turbine_casing'); })
            .pattern(function (definition) {
                var face = global.mekGtPreviewBlocks('mekanism:structural_glass').or(global.mekGtPreviewBlocks('mekanismgenerators:turbine_casing'));
                if (functional) face = face.or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1))
                    .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1));
                else face = face.or(global.mekGtPreviewBlocks('mekanismgenerators:turbine_valve').setMinGlobalLimited(2));
                var rotorPredicate = Predicates.custom(function (state) {
                    global.mekGtPreparePreview(state);
                    var rotor = state.getTileEntity();
                    if (!(rotor instanceof Rotor) || rotor.getHousedBlades() !== 2) return false;
                    state.getMatchContext().getOrCreate('mek_gt_rotors', function () { return new List(); }).add(rotor);
                    return true;
                }, global.mekGtPreviewBlocks('mekanismgenerators:turbine_rotor').common.get(0).candidates);
                var pattern = FactoryBlockPattern.start(RD.RIGHT, RD.UP, RD.BACK);
                for (var z = 0; z < 17; z++) {
                    var rows = [];
                    for (var y = 0; y < 18; y++) {
                        var row = '';
                        for (var x = 0; x < 17; x++) {
                            var edge = Number(x === 0 || x === 16) + Number(y === 0 || y === 17) + Number(z === 0 || z === 16);
                            var c = edge >= 2 ? 'F' : edge ? (y >= 15 ? 'V' : 'W') : '.';
                            if (!edge) {
                                if (y <= 14 && x === 8 && z === 8) c = 'R';
                                else if (y === 15) c = x === 8 && z === 8 ? 'X' : 'D';
                                else if (y === 16) {
                                    // 28 blades / 4 per coil = 7 coils, connected through the center.
                                    // ceil(345 vents * 32000 / 64000) = 173 condensers.
                                    var coil = z === 8 && x >= 5 && x <= 11;
                                    var ordinal = (z - 1) * 15 + (x - 1);
                                    var precedingCoils = z < 8 ? 0 : z > 8 ? 7 : Math.max(0, Math.min(7, x - 5));
                                    c = coil ? 'K' : ordinal - precedingCoils < 173 ? 'N' : '.';
                                }
                            }
                            row += x === 8 && y === 1 && z === 0 ? 'C' : c;
                        }
                        rows.push(row);
                    }
                    pattern.aisle(rows);
                }
                return pattern.where('C', Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .where('F', global.mekGtPreviewBlocks('mekanismgenerators:turbine_casing')).where('W', face)
                    .where('V', global.mekGtPreviewBlocks('mekanismgenerators:turbine_vent')).where('.', Predicates.any())
                    .where('R', rotorPredicate).where('X', global.mekGtPreviewBlocks('mekanismgenerators:rotational_complex'))
                    .where('D', global.mekGtPreviewBlocks('mekanism:pressure_disperser'))
                    .where('K', global.mekGtPreviewBlocks('mekanismgenerators:electromagnetic_coil'))
                    .where('N', global.mekGtPreviewBlocks('mekanismgenerators:saturating_condenser')).build();
            });
        global.mekGtApplyModel(builder, 'mekanismgenerators:block/turbine_casing', 'gtceu:block/multiblock/generator/large_steam_turbine');
    });
})();
