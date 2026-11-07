// Base-world veins are shared with local_transfer_recipes.js. Only Ameijia additions belong here.
var AMEIJIA_TRANSFER_VEINS = [
  {
    id: 'ameijia_hafhnium',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: 30, max: 80 },
    weight: 30,
    raw_materials: ['hafnium', 'naquadah', 'platinum', 'aluminium'],
    gen_inferred: [
      'gtceu:marble_hafnium_ore',
      'gtceu:marble_naquadah_ore',
      'gtceu:marble_platinum_ore',
      'gtceu:marble_aluminium_ore'
    ],
    ore_weights: [2, 2, 1, 3]
  },
  {
    id: 'ameijia_enigma',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: 0, max: 30 },
    weight: 10,
    raw_materials: ['enigma', 'ilmenite', 'molybdenite', 'aluminium'],
    gen_inferred: [
      'gtcsolo:marble_enigma_ore',
      'gtceu:marble_ilmenite_ore',
      'gtceu:marble_molybdenite_ore',
      'gtceu:marble_aluminium_ore'
    ],
    ore_weights: [2, 3, 2, 3]
  },
  {
    id: 'ameijia_stellarium',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: 0, max: 30 },
    weight: 10,
    raw_materials: ['stellarium', 'aluminium', 'pyrolusite', 'bastnasite'],
    gen_inferred: [
      'gtcsolo:marble_stellarium_ore',
      'gtceu:marble_aluminium_ore',
      'gtceu:marble_pyrolusite_ore',
      'gtceu:marble_bastnasite_ore'
    ],
    ore_weights: [1, 3, 1, 1]
  },
  {
    id: 'ameijia_bedrockium',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: 0, max: 80 },
    weight: 20,
    raw_materials: ['bedrockium', 'cobaltite', 'aluminium'],
    gen_inferred: [
      'gtcsolo:marble_bedrockium_ore',
      'gtceu:marble_cobaltite_ore',
      'gtceu:marble_aluminium_ore'
    ],
    ore_weights: [1, 2, 3]
  },
  {
    id: 'ameijia_valinium',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: 30, max: 120 },
    weight: 50,
    raw_materials: ['valinium', 'cobaltite', 'aluminium'],
    gen_inferred: [
      'gtcsolo:marble_valinium_ore',
      'gtceu:marble_cobaltite_ore',
      'gtceu:marble_aluminium_ore'
    ],
    ore_weights: [3, 1, 3]
  },
  {
    id: 'ameijia_stagnanted_neutronium',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: -20, max: 30 },
    weight: 20,
    raw_materials: ['stagnanted_neutronium', 'naquadah', 'aluminium'],
    gen_inferred: [
      'gtcsolo:marble_stagnanted_neutronium_ore',
      'gtceu:marble_naquadah_ore',
      'gtceu:marble_aluminium_ore'
    ],
    ore_weights: [1, 2, 3]
  },
  {
    id: 'ameijia_flaurite',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: 0, max: 80 },
    weight: 40,
    raw_materials: ['flaurite', 'aluminium', 'cobaltite', 'bastnasite'],
    gen_inferred: [
      'gtcsolo:marble_flaurite_ore',
      'gtceu:marble_aluminium_ore',
      'gtceu:marble_cobaltite_ore',
      'gtceu:marble_bastnasite_ore'
    ],
    ore_weights: [3, 3, 3, 2]
  },
  {
    id: 'ameijia_exporonium',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: -30, max: 30 },
    weight: 20,
    raw_materials: ['exporonium', 'naquadah', 'neodymium', 'aluminium'],
    gen_inferred: [
      'gtcsolo:marble_exporonium_ore',
      'gtceu:marble_naquadah_ore',
      'gtceu:marble_neodymium_ore',
      'gtceu:marble_aluminium_ore'
    ],
    ore_weights: [2, 2, 1, 3]
  },
  {
    id: 'ameijia_jupitate',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: -60, max: -30 },
    weight: 20,
    raw_materials: ['jupitate', 'bedrockium', 'aluminium', 'platinum'],
    gen_inferred: [
      'gtcsolo:marble_jupitate_ore',
      'gtcsolo:marble_bedrockium_ore',
      'gtceu:marble_aluminium_ore',
      'gtceu:marble_platinum_ore'
    ],
    ore_weights: [1, 1, 3, 1]
  },
  {
    id: 'ameijia_stellarium_enigma',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: -60, max: 30 },
    weight: 20,
    raw_materials: ['stellarium_enigma', 'enigma', 'stellarium', 'bedrockium'],
    gen_inferred: [
      'gtcsolo:marble_stellarium_enigma_ore',
      'gtcsolo:marble_enigma_ore',
      'gtcsolo:marble_stellarium_ore',
      'gtcsolo:marble_bedrockium_ore'
    ],
    ore_weights: [1, 1, 1, 1]
  },
  {
    id: 'ameijia_dense_stellarium_enigma',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: -60, max: -30 },
    weight: 10,
    raw_materials: ['stellarium_enigma', 'jupitate'],
    gen_inferred: [
      'gtcsolo:marble_stellarium_enigma_ore',
      'gtcsolo:marble_jupitate_ore'
    ],
    ore_weights: [3, 1]
  }
];
var DIM_TRANSFER_TOTAL_BLOCKS = 16 * 30 * 16;
var DIM_TRANSFER_AIR_AMOUNT = 128000;

var DIM_TRANSFER_EUT = {
  IS_OVERWORLD: 131072,
  IS_NETHER: 524288,
  IS_END: 2097152,
  IS_AMEIJIA: 8388608
};

var DIM_TRANSFER_FILLER = {
  STONE: 'minecraft:stone',
  DEEPSLATE: 'minecraft:deepslate',
  NETHERRACK: 'minecraft:netherrack',
  ENDSTONE: 'minecraft:end_stone',
  MARBLE: 'gtceu:marble'
};

var DIM_TRANSFER_AIR = {
  IS_OVERWORLD: 'gtceu:air',
  IS_NETHER: 'gtceu:nether_air',
  IS_END: 'gtceu:ender_air',
  IS_AMEIJIA: 'gtceu:ameijia_air'
};

function dimTransferBiomeKey(vein) {
  if (vein && vein.biome_condition) return vein.biome_condition;
  return 'UNKNOWN';
}

function dimTransferEUt(vein) {
  var key = dimTransferBiomeKey(vein);
  return DIM_TRANSFER_EUT[key] || 131072;
}

function dimTransferDuration(vein) {
  var minY = 0;
  if (vein && vein.height && typeof vein.height.min === 'number') minY = vein.height.min;
  return minY < 0 ? 1800 * 20 : 1200 * 20;
}

function dimTransferFiller(vein) {
  if (vein && vein.dimension_layer && DIM_TRANSFER_FILLER[vein.dimension_layer]) {
    return DIM_TRANSFER_FILLER[vein.dimension_layer];
  }
  return 'minecraft:stone';
}

function dimTransferAirFluid(vein) {
  var key = dimTransferBiomeKey(vein);
  var fluidId = DIM_TRANSFER_AIR[key] || 'gtceu:air';
  return Fluid.of(fluidId, DIM_TRANSFER_AIR_AMOUNT);
}

function dimTransferBuildOutputs(vein) {
  var weight = vein && vein.weight ? Number(vein.weight) : 0;
  if (!(weight >= 0)) weight = 0;
  weight = Math.min(100, weight);

  var oreCount = Math.floor(DIM_TRANSFER_TOTAL_BLOCKS * (weight / 100));
  var fillerCount = DIM_TRANSFER_TOTAL_BLOCKS - oreCount;

  var outputs = [];
  var ores = vein.gen_inferred || [];
  var weights = vein.ore_weights || [];

  if (oreCount > 0 && ores.length > 0) {
    // Share the remainder allocation with local transfer, retaining this recipe's slot order.
    outputs = distributeByWeights(oreCount, ores, weights, true);
  }

  if (fillerCount > 0) {
    outputs.push({ id: dimTransferFiller(vein), count: fillerCount });
  }

  return outputs;
}

ServerEvents.recipes(function(event) {
  var merged = [];
  if (TRANSFER_VEINS) merged = merged.concat(TRANSFER_VEINS);
  if (AMEIJIA_TRANSFER_VEINS) merged = merged.concat(AMEIJIA_TRANSFER_VEINS);

  var usedIds = {};
  var circuitMap = {};

  for (var i = 0; i < merged.length; i++) {
    var vein = merged[i];
    if (!vein || !vein.id) continue;
    if (!vein.gen_inferred || vein.gen_inferred.length === 0) continue;

    var id = 'dimension_transfer/' + vein.id;
    if (usedIds[id]) continue;
    usedIds[id] = true;

    var biome = dimTransferBiomeKey(vein);
    if (circuitMap[biome] == null) circuitMap[biome] = 0;

    var circuit = circuitMap[biome];
    circuitMap[biome]++;

    if (circuit > 32) continue;

    var outputs = dimTransferBuildOutputs(vein);

    var r = event.recipes.gtceu.dimension_transfer(id)
      .circuit(circuit)
      .inputFluids(dimTransferAirFluid(vein))
      .duration(dimTransferDuration(vein))
      .EUt(dimTransferEUt(vein));

    for (var j = 0; j < outputs.length; j++) {
      r.itemOutputs(outputs[j].count + 'x ' + outputs[j].id);
    }
  }
});
