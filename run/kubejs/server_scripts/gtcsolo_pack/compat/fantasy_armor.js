ServerEvents.recipes(event => {
    // Register these recipes only when their required items exist.

    // Optional content is registered only when its output and all concrete ingredients exist.
    function optionalItemsPresent(ids) {
        return ids.every(function (id) { return Item.exists(id); });
    }

    if (optionalItemsPresent(['fantasy_armor:silver_knight_boots', 'apotheosis:epic_material', 'minecraft:echo_shard', 'l2complements:totemic_gold_ingot', 'minecraft:netherite_boots'])) {
        event.shaped(
            Item.of('fantasy_armor:silver_knight_boots', '{Damage:0}').strongNBT(),
            ['ABA', 'CDC'],
            {A: 'apotheosis:epic_material', B: 'minecraft:echo_shard', C: 'l2complements:totemic_gold_ingot', D: 'minecraft:netherite_boots'}
        ).id('gtcsolo:pack/silver_knight_boots');
    }
    if (optionalItemsPresent(['fantasy_armor:silver_knight_leggings', 'apotheosis:epic_material', 'minecraft:echo_shard', 'minecraft:netherite_leggings', 'l2complements:totemic_gold_ingot'])) {
        event.shaped(
            Item.of('fantasy_armor:silver_knight_leggings', '{Damage:0}').strongNBT(),
            ['ABA', 'ACA', 'D D'],
            {A: 'apotheosis:epic_material', B: 'minecraft:echo_shard', C: 'minecraft:netherite_leggings', D: 'l2complements:totemic_gold_ingot'}
        ).id('gtcsolo:pack/silver_knight_leggings');
    }
    if (optionalItemsPresent(['fantasy_armor:silver_knight_chestplate', 'apotheosis:epic_material', 'minecraft:netherite_chestplate', 'minecraft:echo_shard', 'l2complements:totemic_gold_ingot'])) {
        event.shaped(
            Item.of('fantasy_armor:silver_knight_chestplate', '{Damage:0}').strongNBT(),
            ['ABA', 'ACA', 'DDD'],
            {A: 'apotheosis:epic_material', B: 'minecraft:netherite_chestplate', C: 'minecraft:echo_shard', D: 'l2complements:totemic_gold_ingot'}
        ).id('gtcsolo:pack/silver_knight_chestplate');
    }
    if (optionalItemsPresent(['fantasy_armor:silver_knight_helmet', 'apotheosis:epic_material', 'minecraft:echo_shard', 'l2complements:totemic_gold_ingot', 'minecraft:netherite_helmet'])) {
        event.shaped(
            Item.of('fantasy_armor:silver_knight_helmet', '{Damage:0}').strongNBT(),
            ['ABA', 'CDC'],
            {A: 'apotheosis:epic_material', B: 'minecraft:echo_shard', C: 'l2complements:totemic_gold_ingot', D: 'minecraft:netherite_helmet'}
        ).id('gtcsolo:pack/silver_knight_helmet');
    }
    if (optionalItemsPresent(['fantasy_armor:hero_helmet', 'apotheosis:mythic_material', 'fantasy_armor:moon_crystal', 'create:precision_mechanism', 'fantasy_armor:silver_knight_helmet'])) {
        event.shaped(
            Item.of('fantasy_armor:hero_helmet', '{Damage:0}').strongNBT(),
            ['ABA', 'CDC'],
            {A: 'apotheosis:mythic_material', B: 'fantasy_armor:moon_crystal', C: 'create:precision_mechanism', D: 'fantasy_armor:silver_knight_helmet'}
        ).id('gtcsolo:pack/hero_helmet');
    }
    if (optionalItemsPresent(['fantasy_armor:hero_boots', 'apotheosis:mythic_material', 'fantasy_armor:moon_crystal', 'create:precision_mechanism', 'fantasy_armor:silver_knight_boots'])) {
        event.shaped(
            Item.of('fantasy_armor:hero_boots', '{Damage:0}').strongNBT(),
            ['ABA', 'CDC'],
            {A: 'apotheosis:mythic_material', B: 'fantasy_armor:moon_crystal', C: 'create:precision_mechanism', D: 'fantasy_armor:silver_knight_boots'}
        ).id('gtcsolo:pack/hero_boots');
    }
    if (optionalItemsPresent(['fantasy_armor:hero_chestplate', 'apotheosis:mythic_material', 'fantasy_armor:silver_knight_chestplate', 'fantasy_armor:moon_crystal', 'create:precision_mechanism'])) {
        event.shaped(
            Item.of('fantasy_armor:hero_chestplate', '{Damage:0}').strongNBT(),
            ['ABA', 'ACA', 'DDD'],
            {A: 'apotheosis:mythic_material', B: 'fantasy_armor:silver_knight_chestplate', C: 'fantasy_armor:moon_crystal', D: 'create:precision_mechanism'}
        ).id('gtcsolo:pack/hero_chestplate');
    }
    if (optionalItemsPresent(['fantasy_armor:hero_leggings', 'apotheosis:mythic_material', 'fantasy_armor:moon_crystal', 'fantasy_armor:silver_knight_leggings', 'create:precision_mechanism'])) {
        event.shaped(
            Item.of('fantasy_armor:hero_leggings', '{Damage:0}').strongNBT(),
            ['ABA', 'ACA', 'D D'],
            {A: 'apotheosis:mythic_material', B: 'fantasy_armor:moon_crystal', C: 'fantasy_armor:silver_knight_leggings', D: 'create:precision_mechanism'}
        ).id('gtcsolo:pack/hero_leggings');
    }
    if (optionalItemsPresent(['fantasy_armor:moon_crystal', 'create:experience_block', 'minecraft:nether_star', 'minecraft:chorus_fruit', 'minecraft:netherite_upgrade_smithing_template', 'botania:manasteel_ingot'])) {
        event.shaped(
            'fantasy_armor:moon_crystal',
            ['ABA', 'CDC', 'EEE'],
            {A: 'create:experience_block', B: 'minecraft:nether_star', C: 'minecraft:chorus_fruit', D: 'minecraft:netherite_upgrade_smithing_template', E: 'botania:manasteel_ingot'}
        ).id('gtcsolo:pack/moon_crystal/experience_block');
    }
    if (optionalItemsPresent(['fantasy_armor:moon_crystal', 'minecraft:echo_shard', 'minecraft:nether_star', 'l2complements:totemic_gold_ingot', 'minecraft:netherite_upgrade_smithing_template', 'botania:manasteel_ingot'])) {
        event.shaped(
            Item.of('fantasy_armor:moon_crystal', 4),
            ['ABA', 'CDC', 'EEE'],
            {A: 'minecraft:echo_shard', B: 'minecraft:nether_star', C: 'l2complements:totemic_gold_ingot', D: 'minecraft:netherite_upgrade_smithing_template', E: 'botania:manasteel_ingot'}
        ).id('gtcsolo:pack/moon_crystal/totemic_gold');
    }
    if (optionalItemsPresent(['fantasy_armor:moon_crystal', 'minecraft:echo_shard', 'minecraft:nether_star', 'ae2:singularity', 'minecraft:netherite_upgrade_smithing_template', 'botania:manasteel_ingot'])) {
        event.shaped(
            Item.of('fantasy_armor:moon_crystal', 3),
            ['ABA', 'CDC', 'EEE'],
            {A: 'minecraft:echo_shard', B: 'minecraft:nether_star', C: 'ae2:singularity', D: 'minecraft:netherite_upgrade_smithing_template', E: 'botania:manasteel_ingot'}
        ).id('gtcsolo:pack/moon_crystal/singularity');
    }
    if (optionalItemsPresent(['fantasy_armor:moon_crystal', 'minecraft:echo_shard', 'minecraft:nether_star', 'avaritia:neutron_ingot', 'minecraft:netherite_upgrade_smithing_template', 'botania:manasteel_ingot'])) {
        event.shaped(
            Item.of('fantasy_armor:moon_crystal', 8),
            ['ABA', 'CDC', 'EEE'],
            {A: 'minecraft:echo_shard', B: 'minecraft:nether_star', C: 'avaritia:neutron_ingot', D: 'minecraft:netherite_upgrade_smithing_template', E: 'botania:manasteel_ingot'}
        ).id('gtcsolo:pack/moon_crystal/neutron_ingot');
    }
});
