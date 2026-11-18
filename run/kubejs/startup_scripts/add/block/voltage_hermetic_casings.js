(function () {
    const JsonParser = Java.loadClass('com.google.gson.JsonParser');
    const tiers = ['uev', 'uiv', 'uxv', 'opv'];

    StartupEvents.registry('block', event => {
        tiers.forEach(tier => {
            const id = 'gtcsolo:' + tier + '_hermetic_casing';
            const texture = 'gtcsolo:block/casings/hermetic/' + tier + '/';
            const builder = event.create(id)
                .hardness(5.0).resistance(6.0).soundType('metal')
                .requiresTool(true).fullBlock(true).noValidSpawns(true)
                .tagBlock('minecraft:mineable/pickaxe')
                .tagBlock('gtceu:mineable/wrench')
                .renderType('cutout_mipped');
            builder.modelJson = JsonParser.parseString(JSON.stringify({
                parent: 'minecraft:block/cube_bottom_top',
                textures: {side: texture + 'side', top: texture + 'top', bottom: texture + 'bottom'}
            })).getAsJsonObject();
        });
    });

    StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', event => {
        tiers.forEach(tier => event.add(['gtcsolo:' + tier + '_hermetic_casing']));
    });
})();
