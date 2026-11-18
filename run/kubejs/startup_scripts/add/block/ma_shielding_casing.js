(function () {
    const id = 'gtcsolo:ma_shielding_casing';
    StartupEvents.registry('block', event => {
        event.create(id)
            .displayName('Heavy Shielding Casing')
            .hardness(5).resistance(6).soundType('metal')
            .requiresTool().noValidSpawns(true)
            .tagBlock('minecraft:mineable/pickaxe')
            .tagBlock('forge:mineable/wrench')
            .model('gtcsolo:block/ma_shielding_casing');
    });
    StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', event => event.add([id]));
})();
