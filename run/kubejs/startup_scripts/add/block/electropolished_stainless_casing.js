StartupEvents.registry('block', event => {
    event.create('gtcsolo:electropolished_stainless_casing')
        .displayName('Electropolished Stainless Steel Machine Casing')
        .hardness(5).resistance(6).soundType('metal')
        .requiresTool().noValidSpawns(true)
        .tagBlock('minecraft:mineable/pickaxe')
        .tagBlock('forge:mineable/wrench')
        .textureAll('gtcsolo:block/casings/industrial/electropolished_stainless_casing');
});

StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', event => {
    event.add(['gtcsolo:electropolished_stainless_casing']);
});
