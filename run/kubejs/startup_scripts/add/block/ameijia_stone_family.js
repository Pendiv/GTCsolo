// Keep the existing gtcsolo IDs so placed blocks and items retain their identity.
(function () {
    var AXIS = Java.loadClass('net.minecraft.world.level.block.state.properties.BlockStateProperties').AXIS;
    var Direction = Java.loadClass('net.minecraft.core.Direction');
    var JsonParser = Java.loadClass('com.google.gson.JsonParser');
    var materials = [
        {id: 'stone', name: 'Ameijia Stone', hardness: 1.5, sound: 'stone'},
        {id: 'cobblestone', name: 'Ameijia Cobblestone', hardness: 2.0, sound: 'stone'},
        {id: 'deepslate', name: 'Ameijia Deepslate', hardness: 3.0, sound: 'deepslate'},
        {id: 'cobbled_deepslate', name: 'Ameijia Cobbled Deepslate', hardness: 3.5, sound: 'deepslate'}
    ];

    StartupEvents.registry('block', function (event) {
        materials.forEach(function (entry) {
            var builder = event.create('gtcsolo:ameijia_' + entry.id)
                .displayName(entry.name).hardness(entry.hardness).resistance(6.0)
                .soundType(entry.sound).requiresTool(true)
                .tagBlock('minecraft:mineable/pickaxe')
                .textureAll('gtcsolo:block/ameijia/' + entry.id);
            if (entry.id !== 'deepslate') return;

            builder.property(AXIS)
                .defaultState(function (state) { state.setValue(AXIS, Direction.UP.getAxis()); })
                .placementState(function (state) { state.setValue(AXIS, state.getClickedFace().getAxis()); })
                .rotateState(function (state) {
                    var axis = String(state.getValue(AXIS));
                    var direction = axis === 'x' ? Direction.EAST : axis === 'z' ? Direction.SOUTH : Direction.UP;
                    state.setValue(AXIS, state.getRotation().rotate(direction).getAxis());
                });
            builder.modelJson = JsonParser.parseString(JSON.stringify({
                parent: 'minecraft:block/cube_column',
                textures: {side: 'gtcsolo:block/ameijia/deepslate', end: 'gtcsolo:block/ameijia/deepslate_top'}
            })).getAsJsonObject();
            builder.blockstateJson = JsonParser.parseString(JSON.stringify({
                variants: {
                    'axis=x': {model: 'gtcsolo:block/ameijia_deepslate', x: 90, y: 90},
                    'axis=y': {model: 'gtcsolo:block/ameijia_deepslate'},
                    'axis=z': {model: 'gtcsolo:block/ameijia_deepslate', x: 90}
                }
            })).getAsJsonObject();
        });
    });

    StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', function (event) {
        materials.forEach(function (entry) { event.add(['gtcsolo:ameijia_' + entry.id]); });
    });
})();
