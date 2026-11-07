// Generated from tools/gt-ore-seeds/catalog.json.
ServerEvents.tags('item', event => {
    if (!Platform.isLoaded('mysticalagriculture')) return;
    var registry = Java.loadClass('com.blakebr0.mysticalagriculture.registry.CropRegistry').getInstance();
    var overrides = {"osmium": 6, "iridium": 7, "draconium": 8};
    ["aluminum", "amethyst", "amethyst_bronze", "apatite", "aquamarine", "blazing_crystal", "brass", "bronze", "certus_quartz", "chrome", "coal", "cobalt", "compressed_iron", "conductive_alloy", "constantan", "copper", "copper_alloy", "cyanite", "dark_steel", "diamond", "draconium", "electrum", "elementium", "emerald", "end_steel", "enderium", "energetic_alloy", "energized_steel", "fiery_ingot", "fluix", "fluorite", "flux_infused_gem", "flux_infused_ingot", "glowstone", "gold", "grains_of_infinity", "graphite", "hepatizon", "hop_graphite", "invar", "iridium", "iron", "ironwood", "knightmetal", "lapis_lazuli", "lead", "lumium", "manasteel", "manyullyn", "mithril", "nether_quartz", "netherite", "nickel", "niotic_crystal", "obsidian", "osmium", "peridot", "pig_iron", "platinum", "prismarine", "pulsating_alloy", "quartz_enriched_iron", "queens_slime", "redstone", "redstone_alloy", "refined_glowstone", "refined_obsidian", "rock_crystal", "rose_gold", "ruby", "saltpeter", "sapphire", "signalum", "silicon", "silver", "slimesteel", "soularium", "soulium", "spirited_crystal", "starmetal", "steel", "steeleaf", "sulfur", "terrasteel", "tin", "titanium", "tungsten", "uraninite", "uranium", "vibrant_alloy", "yellorium", "zinc"].forEach(name => {
        var crop = registry.getCropByName(name);
        if (crop != null && crop.isEnabled() && (overrides[name] || crop.getTier().getValue()) >= 5)
            event.add('gtcsolo:industrial_cultivation_seeds', 'mysticalagriculture:' + name + '_seeds');
    });
    [
    {
        "id": "gtc_iron_ore",
        "harvest": "minecraft:raw_iron",
        "essence": "mysticalagriculture:gtc_iron_ore_essence",
        "eut": 30,
        "grade": 1
    },
    {
        "id": "gtc_iron",
        "harvest": "gtceu:iron_dust",
        "essence": "mysticalagriculture:gtc_iron_essence",
        "eut": 480,
        "grade": 3
    },
    {
        "id": "gtc_ilmenite",
        "harvest": "gtceu:raw_ilmenite",
        "essence": "mysticalagriculture:gtc_ilmenite_essence",
        "eut": 480,
        "grade": 3
    },
    {
        "id": "gtc_titanium",
        "harvest": "gtceu:titanium_dust",
        "essence": "mysticalagriculture:gtc_titanium_essence",
        "eut": 7680,
        "grade": 5
    },
    {
        "id": "gtc_scheelite",
        "harvest": "gtceu:raw_scheelite",
        "essence": "mysticalagriculture:gtc_scheelite_essence",
        "eut": 1920,
        "grade": 4
    },
    {
        "id": "gtc_tungsten",
        "harvest": "gtceu:tungsten_dust",
        "essence": "mysticalagriculture:gtc_tungsten_essence",
        "eut": 30720,
        "grade": 6
    },
    {
        "id": "gtc_platinum_group_sludge",
        "harvest": "gtceu:platinum_group_sludge_dust",
        "essence": "mysticalagriculture:gtc_platinum_group_sludge_essence",
        "eut": 1920,
        "grade": 4
    },
    {
        "id": "gtc_platinum",
        "harvest": "gtceu:platinum_dust",
        "essence": "mysticalagriculture:gtc_platinum_essence",
        "eut": 7680,
        "grade": 5
    }
].forEach(crop => {
        if (crop.grade >= 5) event.add('gtcsolo:industrial_cultivation_seeds', 'mysticalagriculture:' + crop.id + '_seeds');
    });
});
