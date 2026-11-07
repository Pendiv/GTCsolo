// priority: -200
// Generated from tools/gt-ore-seeds/. Target: GTCEu 1.6.4 / MA 7.0.23 / KubeJS 22.
ServerEvents.recipes(event => {
    if (!Platform.isLoaded('gtceu') || !Platform.isLoaded('mysticalagriculture')) return;
    var CropRegistry = Java.loadClass('com.blakebr0.mysticalagriculture.registry.CropRegistry');
    var RecipeJS = Java.loadClass('dev.latvian.mods.kubejs.recipe.RecipeJS');
    var ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');
    var JsonParser = Java.loadClass('com.google.gson.JsonParser');
    var registry = CropRegistry.getInstance();
    var settings = {
    "growth": {
        "water_mb": 1000,
        "ticks": 3000
    },
    "construction": {
        "ticks": 400
    },
    "native_eut_by_tier": {
        "1": 30,
        "2": 120,
        "3": 480,
        "4": 1920,
        "5": 7680,
        "6": 30720,
        "7": 122880,
        "8": 491520
    }
};
    var profiles = {
    "1": {
        "fluid": "minecraft:water",
        "amount": 1000,
        "ticks": 3000,
        "alternatives": [
            "minecraft:bone_meal",
            "gtceu:fertilizer"
        ]
    },
    "2": {
        "fluid": "gtceu:distilled_water",
        "amount": 1000,
        "ticks": 2400,
        "item": "gtcsolo:ammonium_sulfate_dust"
    },
    "3": {
        "fluid": "gtceu:distilled_water",
        "amount": 1000,
        "ticks": 1800,
        "item": "gtcsolo:compound_fertilizer_dust"
    },
    "4": {
        "fluid": "gtcsolo:plant_nutrient_solution",
        "amount": 1000,
        "ticks": 1500,
        "item": "gtcsolo:compound_fertilizer_dust"
    },
    "5": {
        "fluid": "gtcsolo:plant_culture_medium",
        "amount": 2000,
        "ticks": 1200,
        "item": "gtcsolo:plant_culture_substrate"
    },
    "6": {
        "fluid": "gtcsolo:tissue_culture_medium",
        "amount": 2000,
        "ticks": 1200,
        "item": "gtcsolo:tissue_culture_substrate"
    },
    "7": {
        "fluid": "gtcsolo:cell_culture_medium",
        "amount": 2000,
        "ticks": 1200,
        "item": "gtcsolo:cell_culture_substrate"
    },
    "8": {
        "fluid": "gtcsolo:biomineralization_medium",
        "amount": 2000,
        "ticks": 1200,
        "item": "gtcsolo:mineralization_substrate"
    }
};
    var overrides = {
    "osmium": 6,
    "iridium": 7,
    "draconium": 8
};
    var blockedSeeds = {};
    var temperatures = {
    "unit": "K",
    "native": {
        "aluminum": 1700,
        "amethyst": 2000,
        "amethyst_bronze": 2000,
        "apatite": 2000,
        "aquamarine": 2000,
        "blazing_crystal": 4000,
        "brass": 1160,
        "bronze": 1357,
        "certus_quartz": 2000,
        "chrome": 2180,
        "coal": 1273,
        "cobalt": 1768,
        "compressed_iron": 1811,
        "conductive_alloy": 2273,
        "constantan": 1542,
        "copper": 1358,
        "copper_alloy": 2273,
        "cyanite": 1408,
        "dark_steel": 2046,
        "diamond": 4098,
        "draconium": 10800,
        "electrum": 1285,
        "elementium": 2711,
        "emerald": 2000,
        "end_steel": 2946,
        "enderium": 2700,
        "energetic_alloy": 1338,
        "energized_steel": 1811,
        "fiery_ingot": 4000,
        "fluix": 2273,
        "fluorite": 1691,
        "flux_infused_gem": 4098,
        "flux_infused_ingot": 2273,
        "glowstone": 500,
        "gold": 1338,
        "grains_of_infinity": 2700,
        "graphite": 4098,
        "hepatizon": 2700,
        "hop_graphite": 4098,
        "invar": 1916,
        "iridium": 4500,
        "iron": 1800,
        "ironwood": 1811,
        "knightmetal": 2700,
        "lapis_lazuli": 1473,
        "lead": 601,
        "lumium": 1235,
        "manasteel": 1811,
        "manyullyn": 4500,
        "mithril": 2700,
        "nether_quartz": 2000,
        "netherite": 3695,
        "nickel": 1728,
        "niotic_crystal": 4098,
        "obsidian": 1473,
        "osmium": 4500,
        "peridot": 2200,
        "pig_iron": 1811,
        "platinum": 2042,
        "prismarine": 1200,
        "pulsating_alloy": 1811,
        "quartz_enriched_iron": 2273,
        "queens_slime": 2046,
        "redstone": 500,
        "redstone_alloy": 2273,
        "refined_glowstone": 4500,
        "refined_obsidian": 3600,
        "rock_crystal": 2000,
        "rose_gold": 1600,
        "ruby": 2327,
        "saltpeter": 608,
        "sapphire": 2327,
        "signalum": 1358,
        "silicon": 2273,
        "silver": 1235,
        "slimesteel": 2046,
        "soularium": 1338,
        "soulium": 1811,
        "spirited_crystal": 4500,
        "starmetal": 2700,
        "steel": 2046,
        "steeleaf": 2046,
        "sulfur": 389,
        "terrasteel": 3000,
        "tin": 506,
        "titanium": 1943,
        "tungsten": 3695,
        "uraninite": 3150,
        "uranium": 1408,
        "vibrant_alloy": 2238,
        "yellorium": 1408,
        "zinc": 693
    },
    "custom": {
        "gtc_iron_ore": 1800,
        "gtc_iron": 1800,
        "gtc_ilmenite": 1943,
        "gtc_titanium": 1943,
        "gtc_scheelite": 3695,
        "gtc_tungsten": 3695,
        "gtc_platinum_group_sludge": 2042,
        "gtc_platinum": 2042
    }
}; // gtcsolo_room_temperature is always Kelvin.
    var nativeNames = [
    "aluminum",
    "amethyst",
    "amethyst_bronze",
    "apatite",
    "aquamarine",
    "blazing_crystal",
    "brass",
    "bronze",
    "certus_quartz",
    "chrome",
    "coal",
    "cobalt",
    "compressed_iron",
    "conductive_alloy",
    "constantan",
    "copper",
    "copper_alloy",
    "cyanite",
    "dark_steel",
    "diamond",
    "draconium",
    "electrum",
    "elementium",
    "emerald",
    "end_steel",
    "enderium",
    "energetic_alloy",
    "energized_steel",
    "fiery_ingot",
    "fluix",
    "fluorite",
    "flux_infused_gem",
    "flux_infused_ingot",
    "glowstone",
    "gold",
    "grains_of_infinity",
    "graphite",
    "hepatizon",
    "hop_graphite",
    "invar",
    "iridium",
    "iron",
    "ironwood",
    "knightmetal",
    "lapis_lazuli",
    "lead",
    "lumium",
    "manasteel",
    "manyullyn",
    "mithril",
    "nether_quartz",
    "netherite",
    "nickel",
    "niotic_crystal",
    "obsidian",
    "osmium",
    "peridot",
    "pig_iron",
    "platinum",
    "prismarine",
    "pulsating_alloy",
    "quartz_enriched_iron",
    "queens_slime",
    "redstone",
    "redstone_alloy",
    "refined_glowstone",
    "refined_obsidian",
    "rock_crystal",
    "rose_gold",
    "ruby",
    "saltpeter",
    "sapphire",
    "signalum",
    "silicon",
    "silver",
    "slimesteel",
    "soularium",
    "soulium",
    "spirited_crystal",
    "starmetal",
    "steel",
    "steeleaf",
    "sulfur",
    "terrasteel",
    "tin",
    "titanium",
    "tungsten",
    "uraninite",
    "uranium",
    "vibrant_alloy",
    "yellorium",
    "zinc"
];
    var customCrops = [
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
    var excludedIds = [
    "mysticalagriculture:essence/appliedenergistics2/calculation_press",
    "mysticalagriculture:essence/appliedenergistics2/engineering_press",
    "mysticalagriculture:essence/appliedenergistics2/logic_press",
    "mysticalagriculture:essence/appliedenergistics2/silicon_press"
];
    var mining = {};
    var growthCount = 0;
    var conversionCount = 0;
    var preferredTags = {};
    try {
        var preferences = JsonIO.readJson('config/cucumber-tags.json');
        if (preferences != null) preferredTags = JSON.parse(String(preferences));
    } catch (error) {
        console.warn('[Resource rooms] Cannot read explicit Cucumber output preferences: ' + error);
    }

    function nativeSettings(name) {
        var crop = registry.getCropByName(name);
        if (crop == null || !crop.isEnabled()) return null;
        var tier = overrides[name] || Math.max(1, crop.getTier().getValue());
        var eut = settings.native_eut_by_tier[String(tier)];
        var temperature = temperatures.native[name];
        if (eut == null || temperature == null) {
            console.warn('[Resource rooms] Missing voltage/temperature for ' + name + '; recipes retained.');
            return null;
        }
        var seed = 'mysticalagriculture:' + name + '_seeds';
        var essence = 'mysticalagriculture:' + name + '_essence';
        if (!Item.exists(seed) || !Item.exists(essence)) return null;
        return {id: name, seed: seed, essence: essence, eut: eut, grade: tier, temperature: temperature};
    }

    function addGrowth(crop) {
        var profile = profiles[String(crop.grade)];
        if (!profile) throw new Error('Missing cultivation profile for ' + crop.id);
        var growth = event.recipes.gtcsolo.faming_room('gtcsolo:' + crop.id)
            .notConsumable(crop.seed)
            .inputFluids(Fluid.of(profile.fluid, profile.amount))
            .itemOutputs(crop.essence)
            .duration(profile.ticks)
            .EUt(crop.eut);
        if (profile.alternatives) growth.itemInputs(Ingredient.of(profile.alternatives));
        else growth.itemInputs(profile.item);
        if (crop.grade >= 5) blockedSeeds[crop.seed] = true;
        growthCount++;
    }

    nativeNames.forEach(name => {
        var crop = nativeSettings(name);
        if (crop == null) return;
        mining[crop.essence] = crop;
        addGrowth(crop);
    });

    // The custom series has independently approved GT voltage gates. Its seed
    // assembler continues to consume the GT raw/dust material rather than essence.
    if (Platform.isLoaded('mysticalcustomization')) {
        customCrops.forEach(crop => {
            crop.seed = 'mysticalagriculture:' + crop.id + '_seeds';
            crop.temperature = temperatures.custom[crop.id];
            if (!Item.exists(crop.seed) || !Item.exists(crop.essence) || !Item.exists(crop.harvest) || crop.temperature == null) {
                console.warn('[Resource rooms] Custom crop unavailable: ' + crop.id + '; restart with its crop config.');
                return;
            }
            addGrowth(crop);
            event.recipes.gtcsolo.construction_room('gtcsolo:' + crop.id)
                .itemInputs(crop.essence)
                .itemOutputs(crop.harvest)
                .duration(settings.construction.ticks)
                .EUt(crop.eut)
                .addData('gtcsolo_room_temperature', crop.temperature);
            conversionCount++;
        });
    }

    // Preserve native ingredient conditions when moving mineral seed formation.
    var seedPlans = [];
    event.forEachRecipe({type:'mysticalagriculture:infusion'}, source => {
        var data = JSON.parse(String(source.json));
        if (!data.result || !data.result.item) return;
        var name = data.result.item.replace('mysticalagriculture:', '').replace(/_seeds$/, '');
        if (nativeNames.indexOf(name) < 0) return;
        var crop = nativeSettings(name);
        if (crop && data.input && data.ingredients) seedPlans.push({source:String(source.id), data:data, crop:crop});
    });
    seedPlans.forEach(plan => {
        var profile = profiles[String(plan.crop.grade)];
        var combined = {};
        [plan.data.input].concat(plan.data.ingredients).forEach(ingredient => {
            var key = JSON.stringify(ingredient);
            if (!combined[key]) combined[key] = {ingredient:ingredient, count:0};
            combined[key].count++;
        });
        function ic(ingredient, count) {
            return {content:{type:'gtceu:sized', count:count, ingredient:ingredient},chance:10000,maxChance:10000,tierChanceBoost:0};
        }
        var inputs = Object.keys(combined).map(key => ic(combined[key].ingredient,combined[key].count));
        if (plan.crop.grade >= 5) inputs.push(ic({item:profile.item},4));
        if (inputs.length > 9) throw new Error('Too many seed ingredients for ' + plan.source);
        var data = {type:'gtcsolo:cultivation_formation',duration:400,
            inputs:{item:inputs,fluid:[{content:{amount:500,value:{fluid:profile.fluid}},chance:10000,maxChance:10000,tierChanceBoost:0}]},
            outputs:{item:[ic({item:plan.crop.seed},1)]},
            tickInputs:{eu:[{content:plan.crop.eut,chance:10000,maxChance:10000,tierChanceBoost:0}]}};
        if (plan.data.conditions) data.conditions=plan.data.conditions;
        var raw=new RecipeJS();raw.type=event.getRecipeFunction('gtcsolo:cultivation_formation');
        raw.id=new ResourceLocation('gtcsolo:cultivation_formation/ma/'+plan.crop.id);
        raw.json=JsonParser.parseString(JSON.stringify(data)).getAsJsonObject();raw.newRecipe=true;event.addRecipe(raw,true);
        event.remove({id:plan.source});
        event.remove({id:'mysticalagriculture:seed/crafting/'+plan.crop.id});
    });
    if (Platform.isLoaded('botanypots')) {
        var removals=[];
        event.forEachRecipe({type:'botanypots:crop'}, recipe => {
            var data=JSON.parse(String(recipe.json));
            function blocked(value) {
                if (!value || typeof value !== 'object') return false;
                if (value.item && blockedSeeds[value.item]) return true;
                return Object.keys(value).some(key => blocked(value[key]));
            }
            if (blocked(data.seed)) removals.push(String(recipe.id));
        });
        removals.forEach(id => event.remove({id:id}));
    }

    // Inspect serialized data only. These filters never resolve recipe outputs.
    // KubeJS 2001.6.5-build.22 forEachRecipe is synchronous/non-parallel.
    // Never call Cucumber TagMapper here: native Forge tags are not yet bound at
    // this event. Preserve tag ingredients for the native GT serializer instead.
    var plans = [];
    function planConversion(recipe) {
        var sourceId = String(recipe.id);
        if (sourceId.indexOf('mysticalagriculture:essence/') !== 0 || excludedIds.indexOf(sourceId) !== -1) return;
        var data;
        try { data = JSON.parse(String(recipe.json)); }
        catch (error) {
            console.warn('[Resource rooms] Cannot read ' + sourceId + ': ' + error);
            return;
        }
        if (!data.result || (!data.result.item && !data.result.tag)) return;
        var inputs = [];
        if (data.pattern && data.key) {
            var counts = {};
            data.pattern.forEach(row => {
                for (var i = 0; i < row.length; i++) {
                    if (row[i] !== ' ') counts[row[i]] = (counts[row[i]] || 0) + 1;
                }
            });
            Object.keys(counts).sort().forEach(symbol => {
                if (data.key[symbol]) inputs.push({ingredient: data.key[symbol], count: counts[symbol]});
            });
        } else if (data.ingredients) {
            data.ingredients.forEach(ingredient => inputs.push({ingredient: ingredient, count: 1}));
        } else return;
        if (inputs.length === 0) return;
        var essences = [];
        function findEssences(value) {
            if (value == null || typeof value !== 'object') return;
            if (value.item && /_essence$/.test(value.item)) essences.push(value.item);
            Object.keys(value).forEach(key => {
                if (typeof value[key] === 'object') findEssences(value[key]);
            });
        }
        inputs.forEach(input => findEssences(input.ingredient));
        // Mixed stone/food/elemental/mob recipes retain their original crafting route.
        if (essences.length === 0 || !essences.every(essence => mining[essence] != null)) return;
        var eut = 0;
        var temperature = 0;
        essences.forEach(essence => {
            eut = Math.max(eut, mining[essence].eut);
            temperature = Math.max(temperature, mining[essence].temperature);
        });
        var fingerprint = inputs.map(input => JSON.stringify(input)).sort().join('|');
        plans.push({source: sourceId, data: data, inputs: inputs,
            eut: eut, temperature: temperature, fingerprint: fingerprint});
    }
    ['minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'cucumber:shaped_no_mirror', 'cucumber:shaped_tag', 'cucumber:shapeless_tag'].forEach(type => {
        event.forEachRecipe({mod: 'mysticalagriculture', type: type}, planConversion);
    });
    plans.sort((a, b) => a.source < b.source ? -1 : a.source > b.source ? 1 : 0);
    var groups = {};
    plans.forEach(plan => {
        if (!groups[plan.fingerprint]) groups[plan.fingerprint] = [];
        groups[plan.fingerprint].push(plan);
    });
    plans.forEach(plan => {
        var result = plan.data.result;
        var count = result.count == null ? 1 : result.count;
        var outputId = result.item;
        var preferred = result.tag ? preferredTags[result.tag] : null;
        if (typeof preferred === 'string' && preferred !== '' && preferred !== 'null' && preferred !== 'minecraft:air' && Item.exists(preferred)) {
            outputId = preferred;
        }
        if (outputId && !Item.exists(outputId)) {
            console.warn('[Resource rooms] Missing output item for ' + plan.source + '; crafting recipe retained.');
            return;
        }
        var outputIngredient = outputId ? {item: outputId} : {tag: result.tag};
        if (result.nbt != null) {
            if (!outputId) {
                console.warn('[Resource rooms] Tag output with NBT needs an explicit Cucumber item preference: ' + plan.source + '; crafting recipe retained.');
                return;
            }
            outputIngredient = {type: 'forge:nbt', item: outputId, nbt: result.nbt};
        }
        function content(ingredient, amount, chance) {
            return {content: {type: 'gtceu:sized', count: amount, ingredient: ingredient},
                chance: chance, maxChance: 10000, tierChanceBoost: 0};
        }
        var inputContents = plan.inputs.map(input => content(input.ingredient, input.count, 10000));
        var group = groups[plan.fingerprint];
        // Native shaped recipes may use the same ingredients in different shapes.
        // An integrated circuit preserves that choice in a shapeless GT machine.
        if (group.length > 1) inputContents.push(content({type: 'gtceu:circuit', configuration: group.indexOf(plan) + 1}, 1, 0));
        var data = {type: 'gtcsolo:construction_room', duration: settings.construction.ticks,
            inputs: {item: inputContents}, outputs: {item: [content(outputIngredient, count, 10000)]},
            tickInputs: {eu: [{content: plan.eut, chance: 10000, maxChance: 10000, tierChanceBoost: 0}]},
            data: {gtcsolo_room_temperature: plan.temperature}};
        if (plan.data.conditions) data.conditions = plan.data.conditions;
        // The normal GT KJS output adapter eagerly resolves getFirst(), even for
        // tag outputs. A plain KJS RecipeJS passes this native GT JSON unchanged
        // to its serializer after the event, preserving the lazy tag ingredient.
        // These public fields/addRecipe API are verified against exact KJS22 SDK.
        var raw = new RecipeJS();
        raw.type = event.getRecipeFunction('gtcsolo:construction_room');
        raw.id = new ResourceLocation('gtcsolo:construction_room/ma/' + plan.source.split(':')[1]);
        raw.json = JsonParser.parseString(JSON.stringify(data)).getAsJsonObject();
        raw.newRecipe = true;
        event.addRecipe(raw, true);
        // Remove only after a complete replacement exists. Never walk output filters.
        event.remove({id: plan.source});
        conversionCount++;
    });
    console.info('[Resource rooms] Added ' + growthCount + ' mining growth recipes and ' + conversionCount + ' construction recipes.');
});
