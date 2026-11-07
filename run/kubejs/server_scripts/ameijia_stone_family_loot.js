// Override the default self-drops created for KubeJS blocks.
(function () {
    var JsonParser = Java.loadClass('com.google.gson.JsonParser');
    ServerEvents.blockLootTables(function (event) {
        [['stone', 'cobblestone'], ['deepslate', 'cobbled_deepslate']].forEach(function (pair) {
            var original = 'gtcsolo:ameijia_' + pair[0];
            var cobbled = 'gtcsolo:ameijia_' + pair[1];
            var alternatives = {
                type: 'minecraft:alternatives',
                children: [
                    {type: 'minecraft:item', name: original, conditions: [{
                        condition: 'minecraft:match_tool',
                        predicate: {enchantments: [{enchantment: 'minecraft:silk_touch', levels: {min: 1}}]}
                    }]},
                    {type: 'minecraft:item', name: cobbled}
                ]
            };
            event.addBlock(original, function (loot) {
                loot.addPool(function (pool) {
                    pool.survivesExplosion();
                    pool.addEntry(JsonParser.parseString(JSON.stringify(alternatives)).getAsJsonObject());
                });
            });
        });
    });
})();
