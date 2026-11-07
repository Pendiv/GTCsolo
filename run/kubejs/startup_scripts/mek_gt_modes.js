// priority: 200
// 各設備で有効な行を1つにする。変更後はゲーム再起動。
global.mekGtModes = {};
global.mekGtModes.fusion = 'gt';
// global.mekGtModes.fusion = 'display';
global.mekGtModes.induction = 'gt';
// global.mekGtModes.induction = 'display';
global.mekGtModes.turbine = 'gt';
// global.mekGtModes.turbine = 'display';
global.mekGtModes.evaporation = 'gt';
// global.mekGtModes.evaporation = 'display';
// gt: GTハッチで運転。display: Mekポートで構造とレシピを表示、GT運転なし。

// GTCEu 7.x renamed renderer registration to model registration.
global.mekGtApplyModel = function (builder, casing, overlay) {
    if (typeof builder.workableCasingModel === 'function') {
        builder.workableCasingModel(casing, overlay);
    } else {
        builder.workableCasingRenderer(casing, overlay, false);
    }
};

// GT 7.x recreates BlockInfo for its JEI world, so changing candidate metadata alone
// is not enough. Adjust only that dummy world's final BlockInfo during pattern checking.
global.mekGtPreparePreview = (function () {
    var Dummy = null, MekMultiblock = null, MekRotor = null, checked = null;
    return function (state) {
        var world = state.getWorld();
        if (!world.isClientSide) return;
        if (Dummy === null) {
            Dummy = Java.loadClass('com.lowdragmc.lowdraglib.utils.TrackedDummyWorld');
            MekMultiblock = Java.loadClass('mekanism.common.tile.prefab.TileEntityMultiblock');
            MekRotor = Java.loadClass('mekanism.generators.common.tile.turbine.TileEntityTurbineRotor');
            var WeakMap = Java.loadClass('java.util.WeakHashMap');
            checked = new WeakMap();
        }
        // No real world or dummy world backed by a real world may be changed.
        if (!(world instanceof Dummy) || world.proxyWorld.get() !== null) return;
        var marker = world.renderedBlocks.get(state.getPos());
        if (marker === null || checked.containsKey(marker)) return;
        world.renderedBlocks.forEach(function (pos, info) {
            if (checked.containsKey(info)) return;
            checked.put(info, true);
            if (!info.hasBlockEntity()) return;
            var tile = world.getBlockEntity(pos);
            if (tile instanceof MekMultiblock) {
                // Keep the actual block/item identity and static model. Only omit Mek's
                // formed-reactor renderer; cells, providers, rotors and GT BEs remain.
                info.setHasBlockEntity(false);
                world.blockEntities.remove(pos);
            } else if (tile instanceof MekRotor) {
                // GT rebuilds preview BEs without the candidate's NBT. Restore the
                // required two blades in the final dummy world before validation.
                var index = 0;
                var below = pos.below();
                while (world.getBlockEntity(below) instanceof MekRotor) {
                    index++;
                    below = below.below();
                }
                var tag = tile.getReducedUpdateTag();
                tag.putInt('blades', 2);
                tag.putInt('position', index);
                tile.handleUpdateTag(tag);
            }
        });
    };
})();

global.mekGtPreviewBlocks = function (id) {
    var predicate = Predicates.blocks(id);
    var rule = predicate.common.get(0);
    var original = rule.predicate;
    rule.predicate = function (state) {
        global.mekGtPreparePreview(state);
        return original.test(state);
    };
    return predicate;
};
