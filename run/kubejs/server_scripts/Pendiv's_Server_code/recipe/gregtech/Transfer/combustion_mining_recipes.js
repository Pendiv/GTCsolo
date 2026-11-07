ServerEvents.recipes(event => {
    var batchSizes = {};
    batchSizes[GTValues.HV] = 256;
    batchSizes[GTValues.IV] = 512;
    batchSizes[GTValues.LuV] = 1024;
    batchSizes[GTValues.UV] = 2048;
    var dimensionTiers = {
        IS_NETHER: GTValues.IV,
        IS_END: GTValues.LuV,
        IS_AMEIJIA: GTValues.UV
    };
    // Array position is the circuit number minus one. Append new veins to keep existing numbers stable.
    var circuitVeins = {};
    circuitVeins[GTValues.HV] = [
        'apatite_vein', 'cassiterite_vein', 'coal_vein', 'copper_tin_vein', 'galena_vein',
        'garnet_tin_vein', 'garnet_vein', 'iron_vein', 'lubricant_vein', 'magnetite_vein_ow',
        'mineral_sand_vein', 'nickel_vein', 'salts_vein'
    ];
    circuitVeins[GTValues.IV] = [
        'banded_iron_vein', 'beryllium_vein', 'certus_quartz', 'manganese_vein', 'molybdenum_vein',
        'monazite_vein', 'nether_quartz_vein', 'redstone_vein', 'saltpeter_vein', 'sulfur_vein',
        'tetrahedrite_vein', 'topaz_vein', 'copper_vein', 'diamond_vein', 'lapis_vein',
        'manganese_vein_ow', 'mica_vein', 'olivine_vein', 'redstone_vein_ow', 'sapphire_vein'
    ];
    circuitVeins[GTValues.LuV] = [
        'bauxite_vein_end', 'magnetite_vein_end', 'naquadah_vein', 'pitchblende_vein_end',
        'scheelite_vein', 'sheldonite_vein'
    ];
    circuitVeins[GTValues.UV] = [
        'ameijia_hafhnium', 'ameijia_enigma', 'ameijia_stellarium', 'ameijia_bedrockium',
        'ameijia_valinium', 'ameijia_stagnanted_neutronium', 'ameijia_flaurite', 'ameijia_exporonium',
        'ameijia_jupitate', 'ameijia_stellarium_enigma', 'ameijia_dense_stellarium_enigma'
    ];
    var drillingFluidPerBlock = 18;
    var batchTicks = 480 * 20;
    var maxOutputs = 12;

    for (var vein of TRANSFER_VEINS.concat(AMEIJIA_TRANSFER_VEINS)) {
        var tier = vein.biome_condition === 'IS_OVERWORLD'
            ? (vein.dimension_layer === 'DEEPSLATE' ? GTValues.IV : GTValues.HV)
            : dimensionTiers[vein.biome_condition];
        if (tier == null) continue;
        // Refining the oil sands would feed more energy back into the miner than it consumed.
        if (vein.id === 'oilsands_vein') continue;
        var circuitNumber = circuitVeins[tier].indexOf(vein.id) + 1;
        if (circuitNumber < 1 || circuitNumber > 32) {
            console.warn('[combustion_mining] No circuit number for vein ' + vein.id);
            continue;
        }

        var ores = vein.gen_inferred;
        var host = getFiller(vein);
        var density = vein.density;
        var veinName = vein.id;
        if (vein.biome_condition === 'IS_AMEIJIA' && ores) {
            // Match the current Ameijia terrain instead of the old Transfer marble outputs.
            var deep = vein.height.min + vein.height.max < 0;
            var orePrefix = deep ? 'ameijia_deepslate_' : 'ameijia_';
            host = deep ? 'gtcsolo:ameijia_deepslate' : 'gtcsolo:ameijia_stone';
            ores = ores.map((ore, index) => ore.split(':')[0] + ':' + orePrefix + vein.raw_materials[index] + '_ore');
            // The legacy Transfer catalog omits the densities defined in vein_ameijia.js.
            density = vein.id === 'ameijia_dense_stellarium_enigma' ? 0.22 : 0.15;
            if (vein.id === 'ameijia_hafhnium') veinName = 'ameijia_hafhhnium';
            if (vein.id === 'ameijia_stellarium_enigma') veinName = 'ameijia_stellarium-enigma';
        }
        if (!ores || !ores.length || ores.some(ore => !Item.exists(ore)) || !Item.exists(host)) {
            console.warn('[combustion_mining] Missing output block in vein ' + vein.id);
            continue;
        }

        // An excavation has a mineralized core and the host rock surrounding it.
        if (!(density > 0 && density <= 1)) {
            console.warn('[combustion_mining] Invalid density for vein ' + vein.id);
            continue;
        }
        var blocksPerBatch = batchSizes[tier];
        var oresPerBatch = Math.round(blocksPerBatch / 2 * density);
        var hostRockPerBatch = blocksPerBatch - oresPerBatch;
        var counts = mergeSameOutputs(distributeByWeights(oresPerBatch, ores, vein.ore_weights, true));
        counts.push({ id: host, count: hostRockPerBatch });
        if (counts.length > maxOutputs) {
            console.warn('[combustion_mining] Too many output entries for vein ' + vein.id);
            continue;
        }

        var eut = GTValues.VA[tier];
        var recipe = event.recipes.gtcsolo.combustion_mining('gtcsolo:' + vein.id)
            .circuit(circuitNumber)
            .inputFluids(Fluid.of('gtceu:drilling_fluid', blocksPerBatch * drillingFluidPerBlock))
            .duration(batchTicks)
            .EUt(eut)
            .addDataString('gtcsolo_vein_name', 'gtceu.jei.ore_vein.' + veinName);
        for (var output of counts) recipe.itemOutputs(output.count + 'x ' + output.id);
    }
});
