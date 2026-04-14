// priority: 1000

var AMEIJIA_TRANSFER_VEINS = [
  {
    "name": "Bauxite Vein End",
    "id": "bauxite_vein_end",
    "java_constant": "BAUXITE_VEIN_END",
    "dimension_layer": "ENDSTONE",
    "biome_condition": "IS_END",
    "height": {
      "min": 10,
      "max": 80
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.3,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Bauxite",
      "Ilmenite",
      "Aluminium"
    ],
    "gen_inferred": [
      "gtceu:endstone_bauxite_ore",
      "gtceu:endstone_ilmenite_ore",
      "gtceu:endstone_aluminium_ore"
    ],
    "surface_indicator_material": "Bauxite",
    "surface_indicator_inferred": "gtceu:endstone_bauxite_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      2.0,
      1.0,
      1.0
    ]
  },
  {
    id: 'ameijia_hafhnium',
    dimension_layer: 'MARBLE',
    biome_condition: 'IS_AMEIJIA',
    height: { min: 30, max: 80 },
    weight: 30,
    raw_materials: ['hafhnium', 'naquadah', 'platinum', 'aluminium'],
    gen_inferred: [
      'gtceu:marble_hafhnium_ore',
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
      'gtceu:marble_enigma_ore',
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
    raw_materials: ['stellarium', 'aluminium', 'manganese', 'bastnasite'],
    gen_inferred: [
      'gtceu:marble_stellarium_ore',
      'gtceu:marble_aluminium_ore',
      'gtceu:marble_manganese_ore',
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
      'gtceu:marble_bedrockium_ore',
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
      'gtceu:marble_valinium_ore',
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
      'gtceu:marble_stagnanted_neutronium_ore',
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
      'gtceu:marble_flaurite_ore',
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
      'gtceu:marble_exporonium_ore',
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
      'gtceu:marble_jupitate_ore',
      'gtceu:marble_bedrockium_ore',
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
      'gtceu:marble_stellarium_enigma_ore',
      'gtceu:marble_enigma_ore',
      'gtceu:marble_stellarium_ore',
      'gtceu:marble_bedrockium_ore'
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
      'gtceu:marble_stellarium_enigma_ore',
      'gtceu:marble_jupitate_ore'
    ],
    ore_weights: [3, 1]
  },
  {
    "name": "Magnetite Vein End",
    "id": "magnetite_vein_end",
    "java_constant": "MAGNETITE_VEIN_END",
    "dimension_layer": "ENDSTONE",
    "biome_condition": "IS_END",
    "height": {
      "min": 20,
      "max": 80
    },
    "cluster_size": {
      "min": 38,
      "max": 44
    },
    "density": 0.15,
    "weight": 30,
    "generator": "layered",
    "raw_materials": [
      "Magnetite",
      "VanadiumMagnetite",
      "Chromite",
      "Gold"
    ],
    "gen_inferred": [
      "gtceu:endstone_magnetite_ore",
      "gtceu:endstone_vanadium_magnetite_ore",
      "gtceu:endstone_chromite_ore",
      "gtceu:endstone_gold_ore"
    ],
    "surface_indicator_material": "Magnetite",
    "surface_indicator_inferred": "gtceu:endstone_magnetite_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  
  {
    "name": "Naquadah Vein",
    "id": "naquadah_vein",
    "java_constant": "NAQUADAH_VEIN",
    "dimension_layer": "ENDSTONE",
    "biome_condition": "IS_END",
    "height": {
      "min": 10,
      "max": 90
    },
    "cluster_size": {
      "min": 48,
      "max": 80
    },
    "density": 0.25,
    "weight": 30,
    "generator": "cuboid",
    "raw_materials": [
      "Naquadah",
      "Plutonium239"
    ],
    "gen_inferred": [
      "gtceu:endstone_naquadah_ore",
      "gtceu:endstone_plutonium_ore"
    ],
    "surface_indicator_material": "Naquadah",
    "surface_indicator_inferred": "gtceu:endstone_naquadah_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      7.0,
      1.0
    ]
  },
  {
    "name": "Pitchblende Vein End",
    "id": "pitchblende_vein_end",
    "java_constant": "PITCHBLENDE_VEIN",
    "dimension_layer": "ENDSTONE",
    "biome_condition": "IS_END",
    "height": {
      "min": 30,
      "max": 60
    },
    "cluster_size": {
      "min": 32,
      "max": 64
    },
    "density": 0.25,
    "weight": 30,
    "generator": "cuboid",
    "raw_materials": [
      "Pitchblende",
      "Uraninite"
    ],
    "gen_inferred": [
      "gtceu:endstone_pitchblende_ore",
      "gtceu:endstone_uraninite_ore"
    ],
    "surface_indicator_material": "Pitchblende",
    "surface_indicator_inferred": "gtceu:endstone_pitchblende_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      7.0,
      1.0
    ]
  },
  {
    "name": "Scheelite Vein",
    "id": "scheelite_vein",
    "java_constant": "SCHEELITE_VEIN",
    "dimension_layer": "ENDSTONE",
    "biome_condition": "IS_END",
    "height": {
      "min": 20,
      "max": 60
    },
    "cluster_size": {
      "min": 50,
      "max": 64
    },
    "density": 0.7,
    "weight": 20,
    "generator": "dike",
    "raw_materials": [
      "Scheelite",
      "Tungstate",
      "Lithium"
    ],
    "gen_inferred": [
      "gtceu:endstone_scheelite_ore",
      "gtceu:endstone_tungstate_ore",
      "gtceu:endstone_lithium_ore"
    ],
    "surface_indicator_material": "Scheelite",
    "surface_indicator_inferred": "gtceu:endstone_scheelite_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Sheldonite Vein",
    "id": "sheldonite_vein",
    "java_constant": "SHELDONITE_VEIN",
    "dimension_layer": "ENDSTONE",
    "biome_condition": "IS_END",
    "height": {
      "min": 5,
      "max": 50
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.2,
    "weight": 10,
    "generator": "layered",
    "raw_materials": [
      "Bornite",
      "Cooperite",
      "Platinum",
      "Palladium"
    ],
    "gen_inferred": [
      "gtceu:endstone_bornite_ore",
      "gtceu:endstone_cooperite_ore",
      "gtceu:endstone_platinum_ore",
      "gtceu:endstone_palladium_ore"
    ],
    "surface_indicator_material": "Platinum",
    "surface_indicator_inferred": "gtceu:endstone_platinum_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Banded Iron Vein",
    "id": "banded_iron_vein",
    "java_constant": "BANDED_IRON_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 20,
      "max": 40
    },
    "cluster_size": {
      "min": 40,
      "max": 52
    },
    "density": 1.0,
    "weight": 30,
    "generator": "veined",
    "raw_materials": [
      "Goethite",
      "Limonite",
      "Hematite",
      "Gold"
    ],
    "gen_inferred": [
      "gtceu:netherrack_goethite_ore",
      "gtceu:netherrack_limonite_ore",
      "gtceu:netherrack_hematite_ore",
      "gtceu:netherrack_gold_ore"
    ],
    "surface_indicator_material": "Goethite",
    "surface_indicator_inferred": "gtceu:netherrack_goethite_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      0.075
    ]
  },
  {
    "name": "Beryllium Vein",
    "id": "beryllium_vein",
    "java_constant": "BERYLLIUM_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 5,
      "max": 30
    },
    "cluster_size": {
      "min": 50,
      "max": 64
    },
    "density": 0.75,
    "weight": 30,
    "generator": "dike",
    "raw_materials": [
      "Beryllium",
      "Emerald",
      "Thorium"
    ],
    "gen_inferred": [
      "gtceu:netherrack_beryllium_ore",
      "gtceu:netherrack_emerald_ore",
      "gtceu:netherrack_thorium_ore"
    ],
    "surface_indicator_material": "Beryllium",
    "surface_indicator_inferred": "gtceu:netherrack_beryllium_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Certus Quartz",
    "id": "certus_quartz",
    "java_constant": "CERTUS_QUARTZ_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 80,
      "max": 120
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.25,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Quartzite",
      "CertusQuartz",
      "Barite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_quartzite_ore",
      "gtceu:netherrack_certus_quartz_ore",
      "gtceu:netherrack_barite_ore"
    ],
    "surface_indicator_material": "CertusQuartz",
    "surface_indicator_inferred": "gtceu:netherrack_certus_quartz_ore",
    "surface_indicator_placement": "BELOW",
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Manganese Vein",
    "id": "manganese_vein",
    "java_constant": "MANGANESE_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 20,
      "max": 30
    },
    "cluster_size": {
      "min": 50,
      "max": 64
    },
    "density": 0.75,
    "weight": 20,
    "generator": "dike",
    "raw_materials": [
      "Grossular",
      "Pyrolusite",
      "Tantalite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_grossular_ore",
      "gtceu:netherrack_pyrolusite_ore",
      "gtceu:netherrack_tantalite_ore"
    ],
    "surface_indicator_material": "Grossular",
    "surface_indicator_inferred": "gtceu:netherrack_grossular_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Molybdenum Vein",
    "id": "molybdenum_vein",
    "java_constant": "MOLYBDENUM_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 20,
      "max": 50
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.25,
    "weight": 5,
    "generator": "layered",
    "raw_materials": [
      "Wulfenite",
      "Molybdenite",
      "Molybdenum",
      "Powellite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_wulfenite_ore",
      "gtceu:netherrack_molybdenite_ore",
      "gtceu:netherrack_molybdenum_ore",
      "gtceu:netherrack_powellite_ore"
    ],
    "surface_indicator_material": "Molybdenum",
    "surface_indicator_inferred": "gtceu:netherrack_molybdenum_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0,
      1.0
    ]
  },
  {
    "name": "Monazite Vein",
    "id": "monazite_vein",
    "java_constant": "MONAZITE_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 20,
      "max": 40
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.25,
    "weight": 30,
    "generator": "layered",
    "raw_materials": [
      "Bastnasite",
      "Monazite",
      "Neodymium"
    ],
    "gen_inferred": [
      "gtceu:netherrack_bastnasite_ore",
      "gtceu:netherrack_monazite_ore",
      "gtceu:netherrack_neodymium_ore"
    ],
    "surface_indicator_material": "Bastnasite",
    "surface_indicator_inferred": "gtceu:netherrack_bastnasite_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      1.0,
      1.0
    ]
  },
  {
    "name": "Nether Quartz Vein",
    "id": "nether_quartz_vein",
    "java_constant": "NETHER_QUARTZ_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 40,
      "max": 80
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.2,
    "weight": 80,
    "generator": "layered",
    "raw_materials": [
      "NetherQuartz",
      "Quartzite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_nether_quartz_ore",
      "gtceu:netherrack_quartzite_ore"
    ],
    "surface_indicator_material": "NetherQuartz",
    "surface_indicator_inferred": "gtceu:netherrack_nether_quartz_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      1.0
    ]
  },
  {
    "name": "Redstone Vein",
    "id": "redstone_vein",
    "java_constant": "REDSTONE_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 5,
      "max": 40
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.2,
    "weight": 60,
    "generator": "layered",
    "raw_materials": [
      "Redstone",
      "Ruby",
      "Cinnabar"
    ],
    "gen_inferred": [
      "gtceu:netherrack_redstone_ore",
      "gtceu:netherrack_ruby_ore",
      "gtceu:netherrack_cinnabar_ore"
    ],
    "surface_indicator_material": "Redstone",
    "surface_indicator_inferred": "gtceu:netherrack_redstone_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Saltpeter Vein",
    "id": "saltpeter_vein",
    "java_constant": "SALTPETER_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 5,
      "max": 45
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.25,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Saltpeter",
      "Diatomite",
      "Electrotine",
      "Alunite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_saltpeter_ore",
      "gtceu:netherrack_diatomite_ore",
      "gtceu:netherrack_electrotine_ore",
      "gtceu:netherrack_alunite_ore"
    ],
    "surface_indicator_material": "Saltpeter",
    "surface_indicator_inferred": "gtceu:netherrack_saltpeter_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Sulfur Vein",
    "id": "sulfur_vein",
    "java_constant": "SULFUR_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 10,
      "max": 30
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.2,
    "weight": 100,
    "generator": "layered",
    "raw_materials": [
      "Sulfur",
      "Pyrite",
      "Sphalerite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_sulfur_ore",
      "gtceu:netherrack_pyrite_ore",
      "gtceu:netherrack_sphalerite_ore"
    ],
    "surface_indicator_material": "Sulfur",
    "surface_indicator_inferred": "gtceu:netherrack_sulfur_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Tetrahedrite Vein",
    "id": "tetrahedrite_vein",
    "java_constant": "TETRAHEDRITE_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 80,
      "max": 120
    },
    "cluster_size": {
      "min": 40,
      "max": 52
    },
    "density": 1.0,
    "weight": 70,
    "generator": "veined",
    "raw_materials": [
      "Tetrahedrite",
      "Copper",
      "Stibnite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_tetrahedrite_ore",
      "gtceu:netherrack_copper_ore",
      "gtceu:netherrack_stibnite_ore"
    ],
    "surface_indicator_material": "Tetrahedrite",
    "surface_indicator_inferred": "gtceu:netherrack_tetrahedrite_ore",
    "surface_indicator_placement": "BELOW",
    "ore_weights": [
      4.0,
      2.0,
      0.15
    ]
  },
  {
    "name": "Topaz Vein",
    "id": "topaz_vein",
    "java_constant": "TOPAZ_VEIN",
    "dimension_layer": "NETHERRACK",
    "biome_condition": "IS_NETHER",
    "height": {
      "min": 80,
      "max": 120
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.25,
    "weight": 70,
    "generator": "layered",
    "raw_materials": [
      "BlueTopaz",
      "Topaz",
      "Chalcocite",
      "Bornite"
    ],
    "gen_inferred": [
      "gtceu:netherrack_blue_topaz_ore",
      "gtceu:netherrack_topaz_ore",
      "gtceu:netherrack_chalcocite_ore",
      "gtceu:netherrack_bornite_ore"
    ],
    "surface_indicator_material": "Topaz",
    "surface_indicator_inferred": "gtceu:netherrack_topaz_ore",
    "surface_indicator_placement": "BELOW",
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Apatite Vein",
    "id": "apatite_vein",
    "java_constant": "APATITE_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 10,
      "max": 80
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.25,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Apatite",
      "TricalciumPhosphate",
      "Pyrochlore"
    ],
    "gen_inferred": [
      "gtceu:apatite_ore",
      "gtceu:tricalcium_phosphate_ore",
      "gtceu:pyrochlore_ore"
    ],
    "surface_indicator_material": "Apatite",
    "surface_indicator_inferred": "gtceu:apatite_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Cassiterite Vein",
    "id": "cassiterite_vein",
    "java_constant": "CASSITERITE_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 10,
      "max": 80
    },
    "cluster_size": {
      "min": 40,
      "max": 52
    },
    "density": 1.0,
    "weight": 80,
    "generator": "veined",
    "raw_materials": [
      "Tin",
      "Cassiterite"
    ],
    "gen_inferred": [
      "gtceu:tin_ore",
      "gtceu:cassiterite_ore"
    ],
    "surface_indicator_material": "Cassiterite",
    "surface_indicator_inferred": "gtceu:cassiterite_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      4.0,
      0.66
    ]
  },
  {
    "name": "Coal Vein",
    "id": "coal_vein",
    "java_constant": "COAL_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 10,
      "max": 140
    },
    "cluster_size": {
      "min": 38,
      "max": 44
    },
    "density": 0.25,
    "weight": 80,
    "generator": "layered",
    "raw_materials": [
      "Coal"
    ],
    "gen_inferred": [
      "gtceu:coal_ore"
    ],
    "surface_indicator_material": "Coal",
    "surface_indicator_inferred": "gtceu:coal_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0
    ]
  },
  {
    "name": "Copper Tin Vein",
    "id": "copper_tin_vein",
    "java_constant": "COPPER_TIN_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -10,
      "max": 160
    },
    "cluster_size": {
      "min": 40,
      "max": 52
    },
    "density": 1.0,
    "weight": 50,
    "generator": "veined",
    "raw_materials": [
      "Chalcopyrite",
      "Zeolite",
      "Cassiterite",
      "Realgar"
    ],
    "gen_inferred": [
      "gtceu:chalcopyrite_ore",
      "gtceu:zeolite_ore",
      "gtceu:cassiterite_ore",
      "gtceu:realgar_ore"
    ],
    "surface_indicator_material": "Chalcopyrite",
    "surface_indicator_inferred": "gtceu:chalcopyrite_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      5.0,
      2.0,
      2.0,
      0.1
    ]
  },
  {
    "name": "Galena Vein",
    "id": "galena_vein",
    "java_constant": "GALENA_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -15,
      "max": 45
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.25,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Galena",
      "Silver",
      "Lead"
    ],
    "gen_inferred": [
      "gtceu:galena_ore",
      "gtceu:silver_ore",
      "gtceu:lead_ore"
    ],
    "surface_indicator_material": "Galena",
    "surface_indicator_inferred": "gtceu:galena_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Garnet Tin Vein",
    "id": "garnet_tin_vein",
    "java_constant": "GARNET_TIN_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 30,
      "max": 60
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.4,
    "weight": 80,
    "generator": "layered",
    "raw_materials": [
      "CassiteriteSand",
      "GarnetSand",
      "Asbestos",
      "Diatomite"
    ],
    "gen_inferred": [
      "gtceu:cassiterite_sand_ore",
      "gtceu:garnet_sand_ore",
      "gtceu:asbestos_ore",
      "gtceu:diatomite_ore"
    ],
    "surface_indicator_material": "GarnetSand",
    "surface_indicator_inferred": "gtceu:garnet_sand_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Garnet Vein",
    "id": "garnet_vein",
    "java_constant": "GARNET_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -10,
      "max": 50
    },
    "cluster_size": {
      "min": 50,
      "max": 64
    },
    "density": 0.75,
    "weight": 40,
    "generator": "dike",
    "raw_materials": [
      "GarnetRed",
      "GarnetYellow",
      "Amethyst",
      "Opal"
    ],
    "gen_inferred": [
      "gtceu:garnet_red_ore",
      "gtceu:garnet_yellow_ore",
      "gtceu:amethyst_ore",
      "gtceu:opal_ore"
    ],
    "surface_indicator_material": "GarnetRed",
    "surface_indicator_inferred": "gtceu:garnet_red_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Iron Vein",
    "id": "iron_vein",
    "java_constant": "IRON_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -10,
      "max": 60
    },
    "cluster_size": {
      "min": 40,
      "max": 52
    },
    "density": 1.0,
    "weight": 120,
    "generator": "veined",
    "raw_materials": [
      "Goethite",
      "Limonite",
      "Hematite",
      "Malachite"
    ],
    "gen_inferred": [
      "gtceu:goethite_ore",
      "gtceu:limonite_ore",
      "gtceu:hematite_ore",
      "gtceu:malachite_ore"
    ],
    "surface_indicator_material": "Goethite",
    "surface_indicator_inferred": "gtceu:goethite_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      5.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Lubricant Vein",
    "id": "lubricant_vein",
    "java_constant": "LUBRICANT_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 0,
      "max": 50
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.25,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Soapstone",
      "Talc",
      "GlauconiteSand",
      "Pentlandite"
    ],
    "gen_inferred": [
      "gtceu:soapstone_ore",
      "gtceu:talc_ore",
      "gtceu:glauconite_sand_ore",
      "gtceu:pentlandite_ore"
    ],
    "surface_indicator_material": "Talc",
    "surface_indicator_inferred": "gtceu:talc_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Magnetite Vein Ow",
    "id": "magnetite_vein_ow",
    "java_constant": "MAGNETITE_VEIN_OW",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 10,
      "max": 60
    },
    "cluster_size": {
      "min": 38,
      "max": 44
    },
    "density": 0.15,
    "weight": 80,
    "generator": "layered",
    "raw_materials": [
      "Magnetite",
      "VanadiumMagnetite",
      "Gold"
    ],
    "gen_inferred": [
      "gtceu:magnetite_ore",
      "gtceu:vanadium_magnetite_ore",
      "gtceu:gold_ore"
    ],
    "surface_indicator_material": "Magnetite",
    "surface_indicator_inferred": "gtceu:magnetite_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Mineral Sand Vein",
    "id": "mineral_sand_vein",
    "java_constant": "MINERAL_SAND_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 15,
      "max": 60
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.2,
    "weight": 80,
    "generator": "layered",
    "raw_materials": [
      "BasalticMineralSand",
      "GraniticMineralSand",
      "FullersEarth",
      "Gypsum"
    ],
    "gen_inferred": [
      "gtceu:basaltic_mineral_sand_ore",
      "gtceu:granitic_mineral_sand_ore",
      "gtceu:fullers_earth_ore",
      "gtceu:gypsum_ore"
    ],
    "surface_indicator_material": "BasalticMineralSand",
    "surface_indicator_inferred": "gtceu:basaltic_mineral_sand_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Nickel Vein",
    "id": "nickel_vein",
    "java_constant": "NICKEL_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -10,
      "max": 60
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.25,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Garnierite",
      "Nickel",
      "Cobaltite",
      "Pentlandite"
    ],
    "gen_inferred": [
      "gtceu:garnierite_ore",
      "gtceu:nickel_ore",
      "gtceu:cobaltite_ore",
      "gtceu:pentlandite_ore"
    ],
    "surface_indicator_material": "Nickel",
    "surface_indicator_inferred": "gtceu:nickel_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Salts Vein",
    "id": "salts_vein",
    "java_constant": "SALTS_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 30,
      "max": 70
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.2,
    "weight": 50,
    "generator": "layered",
    "raw_materials": [
      "RockSalt",
      "Salt",
      "Lepidolite",
      "Spodumene"
    ],
    "gen_inferred": [
      "gtceu:rock_salt_ore",
      "gtceu:salt_ore",
      "gtceu:lepidolite_ore",
      "gtceu:spodumene_ore"
    ],
    "surface_indicator_material": "Salt",
    "surface_indicator_inferred": "gtceu:salt_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      1.0,
      1.0
    ]
  },
  {
    "name": "Oilsands Vein",
    "id": "oilsands_vein",
    "java_constant": "OILSANDS_VEIN",
    "dimension_layer": "STONE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": 30,
      "max": 80
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.3,
    "weight": 40,
    "generator": "layered",
    "raw_materials": [
      "Oilsands"
    ],
    "gen_inferred": [
      "gtceu:oilsands_ore"
    ],
    "surface_indicator_material": "Oilsands",
    "surface_indicator_inferred": "gtceu:oilsands_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      7.0
    ]
  },
  {
    "name": "Copper Vein",
    "id": "copper_vein",
    "java_constant": "COPPER_VEIN",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -40,
      "max": 10
    },
    "cluster_size": {
      "min": 40,
      "max": 52
    },
    "density": 1.0,
    "weight": 80,
    "generator": "veined",
    "raw_materials": [
      "Chalcopyrite",
      "Iron",
      "Pyrite",
      "Copper"
    ],
    "gen_inferred": [
      "gtceu:deepslate_chalcopyrite_ore",
      "gtceu:deepslate_iron_ore",
      "gtceu:deepslate_pyrite_ore",
      "gtceu:deepslate_copper_ore"
    ],
    "surface_indicator_material": "Copper",
    "surface_indicator_inferred": "gtceu:deepslate_copper_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      5.0,
      2.0,
      2.0,
      2.0
    ]
  },
  {
    "name": "Diamond Vein",
    "id": "diamond_vein",
    "java_constant": "DIAMOND_VEIN",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -55,
      "max": -30
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.25,
    "weight": 40,
    "generator": "classic",
    "raw_materials": [
      "Graphite",
      "Diamond",
      "Coal"
    ],
    "gen_inferred": [
      "gtceu:deepslate_graphite_ore",
      "gtceu:deepslate_diamond_ore",
      "gtceu:deepslate_coal_ore"
    ],
    "surface_indicator_material": "Diamond",
    "surface_indicator_inferred": "gtceu:deepslate_diamond_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      7.0,
      3.0,
      1.0
    ]
  },
  {
    "name": "Lapis Vein",
    "id": "lapis_vein",
    "java_constant": "LAPIS_VEIN",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -60,
      "max": 10
    },
    "cluster_size": {
      "min": 40,
      "max": 52
    },
    "density": 0.75,
    "weight": 40,
    "generator": "dike",
    "raw_materials": [
      "Lazurite",
      "Sodalite",
      "Lapis",
      "Calcite"
    ],
    "gen_inferred": [
      "gtceu:deepslate_lazurite_ore",
      "gtceu:deepslate_sodalite_ore",
      "gtceu:deepslate_lapis_ore",
      "gtceu:deepslate_calcite_ore"
    ],
    "surface_indicator_material": "Lapis",
    "surface_indicator_inferred": "gtceu:deepslate_lapis_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Manganese Vein Ow",
    "id": "manganese_vein_ow",
    "java_constant": "MANGANESE_VEIN_OW",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -30,
      "max": 0
    },
    "cluster_size": {
      "min": 50,
      "max": 64
    },
    "density": 0.75,
    "weight": 20,
    "generator": "dike",
    "raw_materials": [
      "Grossular",
      "Spessartine",
      "Pyrolusite",
      "Tantalite"
    ],
    "gen_inferred": [
      "gtceu:deepslate_grossular_ore",
      "gtceu:deepslate_spessartine_ore",
      "gtceu:deepslate_pyrolusite_ore",
      "gtceu:deepslate_tantalite_ore"
    ],
    "surface_indicator_material": "Grossular",
    "surface_indicator_inferred": "gtceu:deepslate_grossular_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Mica Vein",
    "id": "mica_vein",
    "java_constant": "MICA_VEIN",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -40,
      "max": -10
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.25,
    "weight": 20,
    "generator": "layered",
    "raw_materials": [
      "Kyanite",
      "Mica",
      "Bauxite",
      "Pollucite"
    ],
    "gen_inferred": [
      "gtceu:deepslate_kyanite_ore",
      "gtceu:deepslate_mica_ore",
      "gtceu:deepslate_bauxite_ore",
      "gtceu:deepslate_pollucite_ore"
    ],
    "surface_indicator_material": "Mica",
    "surface_indicator_inferred": "gtceu:deepslate_mica_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Olivine Vein",
    "id": "olivine_vein",
    "java_constant": "OLIVINE_VEIN",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -20,
      "max": 10
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.25,
    "weight": 20,
    "generator": "layered",
    "raw_materials": [
      "Bentonite",
      "Magnesite",
      "Olivine",
      "GlauconiteSand"
    ],
    "gen_inferred": [
      "gtceu:deepslate_bentonite_ore",
      "gtceu:deepslate_magnesite_ore",
      "gtceu:deepslate_olivine_ore",
      "gtceu:deepslate_glauconite_sand_ore"
    ],
    "surface_indicator_material": "Olivine",
    "surface_indicator_inferred": "gtceu:deepslate_olivine_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Redstone Vein Ow",
    "id": "redstone_vein_ow",
    "java_constant": "REDSTONE_VEIN_OW",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -65,
      "max": -10
    },
    "cluster_size": {
      "min": 32,
      "max": 40
    },
    "density": 0.2,
    "weight": 60,
    "generator": "layered",
    "raw_materials": [
      "Redstone",
      "Ruby",
      "Cinnabar"
    ],
    "gen_inferred": [
      "gtceu:deepslate_redstone_ore",
      "gtceu:deepslate_ruby_ore",
      "gtceu:deepslate_cinnabar_ore"
    ],
    "surface_indicator_material": "Redstone",
    "surface_indicator_inferred": "gtceu:deepslate_redstone_ore",
    "surface_indicator_placement": null,
    "ore_weights": [
      3.0,
      2.0,
      1.0
    ]
  },
  {
    "name": "Sapphire Vein",
    "id": "sapphire_vein",
    "java_constant": "SAPPHIRE_VEIN",
    "dimension_layer": "DEEPSLATE",
    "biome_condition": "IS_OVERWORLD",
    "height": {
      "min": -40,
      "max": 0
    },
    "cluster_size": {
      "min": 25,
      "max": 29
    },
    "density": 0.25,
    "weight": 60,
    "generator": "layered",
    "raw_materials": [
      "Almandine",
      "Pyrope",
      "Sapphire",
      "GreenSapphire"
    ],
    "gen_inferred": [
      "gtceu:deepslate_almandine_ore",
      "gtceu:deepslate_pyrope_ore",
      "gtceu:deepslate_sapphire_ore",
      "gtceu:deepslate_green_sapphire_ore"
    ],
    "surface_indicator_material": "Sapphire",
    "surface_indicator_inferred": "gtceu:deepslate_sapphire_ore",
    "surface_indicator_placement": "ABOVE",
    "ore_weights": [
      3.0,
      2.0,
      1.0,
      1.0
    ]
  }
]
;
var DIM_TRANSFER_TOTAL_BLOCKS = 16 * 30 * 16; // 7680
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

  var oreCount = Math.floor(DIM_TRANSFER_TOTAL_BLOCKS * (weight / 100));
  var fillerCount = DIM_TRANSFER_TOTAL_BLOCKS - oreCount;

  var outputs = [];
  var ores = vein.gen_inferred || [];
  var weights = vein.ore_weights || [];

  if (oreCount > 0 && ores.length > 0) {
    var sum = 0;
    for (var i = 0; i < ores.length; i++) {
      sum += weights[i] || 1;
    }

    for (var i = 0; i < ores.length; i++) {
      var w = weights[i] || 1;
      var count = Math.floor(oreCount * w / sum);
      if (count > 0) {
        outputs.push({ id: ores[i], count: count });
      }
    }
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