// Generated from tools/gt-ore-seeds/catalog.json. Recipes for the GT ore-seed series.
// MA auto crafting/infusion/reprocessing is disabled in each crop config.
ServerEvents.recipes(event => {
    if (!Platform.isLoaded('gtceu') || !Platform.isLoaded('mysticalagriculture') || !Platform.isLoaded('mysticalcustomization')) return;
    var crops = [
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
];
    var profiles = {"1": {"fluid": "minecraft:water", "amount": 1000, "ticks": 3000, "alternatives": ["minecraft:bone_meal", "gtceu:fertilizer"]}, "2": {"fluid": "gtceu:distilled_water", "amount": 1000, "ticks": 2400, "item": "gtcsolo:ammonium_sulfate_dust"}, "3": {"fluid": "gtceu:distilled_water", "amount": 1000, "ticks": 1800, "item": "gtcsolo:compound_fertilizer_dust"}, "4": {"fluid": "gtcsolo:plant_nutrient_solution", "amount": 1000, "ticks": 1500, "item": "gtcsolo:compound_fertilizer_dust"}, "5": {"fluid": "gtcsolo:plant_culture_medium", "amount": 2000, "ticks": 1200, "item": "gtcsolo:plant_culture_substrate"}, "6": {"fluid": "gtcsolo:tissue_culture_medium", "amount": 2000, "ticks": 1200, "item": "gtcsolo:tissue_culture_substrate"}, "7": {"fluid": "gtcsolo:cell_culture_medium", "amount": 2000, "ticks": 1200, "item": "gtcsolo:cell_culture_substrate"}, "8": {"fluid": "gtcsolo:biomineralization_medium", "amount": 2000, "ticks": 1200, "item": "gtcsolo:mineralization_substrate"}};
    // Registration happens during startup, so a server /reload alone cannot add these items.
    var missing = crops.filter(crop => !Item.exists('mysticalagriculture:' + crop.id + '_seeds') || !Item.exists(crop.harvest) || !Item.exists(crop.essence));
    if (missing.length > 0) {
        console.warn('[GT ore seeds] Crop registration is incomplete; restart with the generated crop configs. Existing seed recipes were left unchanged.');
        return;
    }
    // Prevent the old pure-metal seed recipes bypassing the new GT-voltage gates.
    // Remove native recipe IDs directly. Broad output filters resolve unrelated
    // Cucumber tag recipes in parallel and can race while writing cucumber-tags.json.
    // Existing items/blocks are retained; unrelated MA crops are not touched.
    ["iron", "titanium", "tungsten", "platinum"].forEach(name => {
        event.remove({id: 'mysticalagriculture:seed/infusion/' + name});
        event.remove({id: 'mysticalagriculture:seed/crafting/' + name});
    });
    crops.forEach(crop => {
        var seed = 'mysticalagriculture:' + crop.id + '_seeds';
        var profile = profiles[String(crop.grade)];
        var creation = event.recipes.gtcsolo.cultivation_formation('gtcsolo:' + crop.id + '_seeds')
            .itemInputs('mysticalagriculture:prosperity_seed_base', '4x ' + crop.harvest, '4x mysticalagriculture:inferium_essence')
            .inputFluids(Fluid.of(profile.fluid, 500))
            .itemOutputs(seed)
            .duration(400)
            .EUt(crop.eut);
        if (crop.grade >= 5) creation.itemInputs('4x ' + profile.item);
        if (Platform.isLoaded('botanypots') && crop.grade < 5) {
            event.custom({
                type: 'botanypots:crop', seed: {item: seed},
                categories: ["dirt", "farmland", "inferium", "prudentium", "tertium", "imperium", "supremium", "awakened_supremium", "insanium"], growthTicks: 3000,
                display: {type: 'botanypots:aging', block: 'mysticalagriculture:' + crop.id + '_crop'},
                drops: [{chance: 1, output: {item: crop.essence}, minRolls: 1, maxRolls: 1}]
            }).id('gtcsolo:botany/' + crop.id);
        }
    });
});
