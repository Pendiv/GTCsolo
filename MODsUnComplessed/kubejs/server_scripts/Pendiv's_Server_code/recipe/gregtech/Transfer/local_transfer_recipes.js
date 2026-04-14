// priority: 1000

var TRANSFER_VEINS = [
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
      "gtceu:netherrack_yellow_limonite_ore",
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
      "gtceu:red_garnet_ore",
      "gtceu:yellow_garnet_ore",
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

var TOTAL_BLOCKS = 8 * 8 * 20; // 1280
var AIR_AMOUNT = 128000;

var EUT_BY_BIOME = {
  IS_OVERWORLD: 8192,   // IV
  IS_NETHER: 32768,     // LuV
  IS_END: 131072        // ZPM
};

var FILLER_BY_LAYER = {
  STONE: 'minecraft:stone',
  DEEPSLATE: 'minecraft:deepslate',
  NETHERRACK: 'minecraft:netherrack',
  ENDSTONE: 'minecraft:end_stone'
};

var AIR_FLUID_BY_BIOME = {
  IS_OVERWORLD: 'gtceu:air',
  IS_NETHER: 'gtceu:nether_air',
  IS_END: 'gtceu:ender_air'
};

function getCircuitNumber(index) {
  return index % 33; // 0 ～ 32
}

function getEUt(vein) {
  if (vein && EUT_BY_BIOME[vein.biome_condition]) {
    return EUT_BY_BIOME[vein.biome_condition];
  }
  return 8192;
}

function getDuration(vein) {
  var minY = 0;
  if (vein && vein.height && typeof vein.height.min === 'number') {
    minY = vein.height.min;
  }
  if (minY < 0) {
    return 1800 * 20;
  }
  return 1200 * 20;
}

function getFiller(vein) {
  if (vein && FILLER_BY_LAYER[vein.dimension_layer]) {
    return FILLER_BY_LAYER[vein.dimension_layer];
  }
  return 'minecraft:stone';
}

function getAirFluid(vein) {
  var fluidId = 'gtceu:air';
  if (vein && AIR_FLUID_BY_BIOME[vein.biome_condition]) {
    fluidId = AIR_FLUID_BY_BIOME[vein.biome_condition];
  }
  return Fluid.of(fluidId, AIR_AMOUNT);
}

function normalizeWeights(ids, weights) {
  var safeWeights = [];
  var i;
  var w;

  for (i = 0; i < ids.length; i++) {
    w = 1;
    if (weights && i < weights.length) {
      w = Number(weights[i]);
      if (!(w > 0)) {
        w = 1;
      }
    }
    safeWeights.push(w);
  }

  return safeWeights;
}

function distributeByWeights(total, ids, weights) {
  var result = [];
  var safeWeights;
  var sum = 0;
  var raw = [];
  var i;
  var exact;
  var base;
  var used = 0;
  var remain;

  if (!ids || ids.length === 0 || total <= 0) {
    return result;
  }

  safeWeights = normalizeWeights(ids, weights);

  for (i = 0; i < safeWeights.length; i++) {
    sum += safeWeights[i];
  }

  for (i = 0; i < ids.length; i++) {
    exact = total * safeWeights[i] / sum;
    base = Math.floor(exact);
    raw.push({
      id: ids[i],
      count: base,
      frac: exact - base
    });
    used += base;
  }

  raw.sort(function(a, b) {
    return b.frac - a.frac;
  });

  remain = total - used;
  for (i = 0; i < remain; i++) {
    raw[i % raw.length].count++;
  }

  for (i = 0; i < raw.length; i++) {
    if (raw[i].count > 0) {
      result.push({
        id: raw[i].id,
        count: raw[i].count
      });
    }
  }

  return result;
}

function mergeSameOutputs(outputs) {
  var map = {};
  var order = [];
  var result = [];
  var i;
  var o;
  var id;

  for (i = 0; i < outputs.length; i++) {
    o = outputs[i];
    if (!o || !o.id || !o.count) {
      continue;
    }
    id = o.id;
    if (map[id] == null) {
      map[id] = 0;
      order.push(id);
    }
    map[id] += o.count;
  }

  for (i = 0; i < order.length; i++) {
    result.push({
      id: order[i],
      count: map[order[i]]
    });
  }

  return result;
}

function compressToMaxItemOutputs(outputs, maxSlots, fillerId) {
  var merged = mergeSameOutputs(outputs);
  var fillerIndex = -1;
  var i;
  var tail;

  if (merged.length <= maxSlots) {
    return merged;
  }

  for (i = 0; i < merged.length; i++) {
    if (merged[i].id === fillerId) {
      fillerIndex = i;
      break;
    }
  }

  if (fillerIndex < 0) {
    merged.push({ id: fillerId, count: 0 });
    fillerIndex = merged.length - 1;
  }

  while (merged.length > maxSlots) {
    tail = merged.pop();
    if (!tail) {
      break;
    }
    merged[fillerIndex].count += tail.count;
  }

  return merged;
}

function buildOutputs(vein) {
  var veinWeight = 0;
  var oreCount;
  var fillerCount;
  var ores = [];
  var oreWeights = [];
  var outputs = [];
  var i;

  if (vein && vein.weight != null) {
    veinWeight = Number(vein.weight);
    if (!(veinWeight >= 0)) {
      veinWeight = 0;
    }
  }

  oreCount = Math.floor(TOTAL_BLOCKS * (veinWeight / 100));
  fillerCount = TOTAL_BLOCKS - oreCount;

  if (vein && vein.gen_inferred && vein.gen_inferred.length) {
    for (i = 0; i < vein.gen_inferred.length; i++) {
      if (vein.gen_inferred[i]) {
        ores.push(vein.gen_inferred[i]);
      }
    }
  }

  if (vein && vein.ore_weights && vein.ore_weights.length) {
    oreWeights = vein.ore_weights;
  }

  if (oreCount > 0 && ores.length > 0) {
    outputs = outputs.concat(distributeByWeights(oreCount, ores, oreWeights));
  }

  if (fillerCount > 0) {
    outputs.push({
      id: getFiller(vein),
      count: fillerCount
    });
  }

  return compressToMaxItemOutputs(outputs, 15, getFiller(vein));
}

function applyOutputs(builder, outputs) {
  var i;
  for (i = 0; i < outputs.length; i++) {
    builder.itemOutputs(outputs[i].count + 'x ' + outputs[i].id);
  }
  return builder;
}

function stringifyOutputs(outputs) {
  var i;
  var parts = [];
  for (i = 0; i < outputs.length; i++) {
    parts.push(outputs[i].id + ' x' + outputs[i].count);
  }
  return parts.join(', ');
}

function stringifyWeights(weights) {
  var i;
  var parts = [];
  if (!weights) return '';
  for (i = 0; i < weights.length; i++) {
    parts.push(String(weights[i]));
  }
  return parts.join(',');
}

ServerEvents.recipes(function(event) {
  var i;
  var vein;
  var ores;
  var outputs;
  var builder;
  var total = 0;
  var built = 0;
  var byBiome = {};
  var byFluid = {};
  var byCircuit = {};
  var byRecipeId = {};
  var circuit;

  if (!TRANSFER_VEINS || TRANSFER_VEINS.length === 0) {
    console.log('[local_transfer] TRANSFER_VEINS is empty');
    return;
  }

  total = TRANSFER_VEINS.length;
  console.log('[local_transfer] total veins = ' + total);

  for (i = 0; i < TRANSFER_VEINS.length; i++) {
    vein = TRANSFER_VEINS[i];

    if (!vein || !vein.id) {
      console.log('[local_transfer] skip invalid vein at index ' + i);
      continue;
    }

    ores = vein.gen_inferred;
    if (!ores || ores.length === 0) {
      console.log('[local_transfer] skip no ores: ' + vein.id);
      continue;
    }

    outputs = buildOutputs(vein);
    circuit = getCircuitNumber(i);

    if (!byBiome[vein.biome_condition]) byBiome[vein.biome_condition] = 0;
    byBiome[vein.biome_condition]++;

    if (!byFluid[getAirFluid(vein).id]) byFluid[getAirFluid(vein).id] = 0;
    byFluid[getAirFluid(vein).id]++;

    if (!byCircuit[circuit]) byCircuit[circuit] = [];
    byCircuit[circuit].push(vein.id);

    if (byRecipeId['local_transfer/' + vein.id]) {
      console.log('[local_transfer] DUPLICATE RECIPE ID: local_transfer/' + vein.id);
    }
    byRecipeId['local_transfer/' + vein.id] = true;

    console.log('[local_transfer] BUILD ' + vein.id);
    console.log('  circuit=' + circuit);
    console.log('  biome=' + vein.biome_condition + ' layer=' + vein.dimension_layer);
    console.log('  fluid=' + getAirFluid(vein).id + ' amount=' + AIR_AMOUNT);
    console.log('  weight=' + vein.weight + ' ore_weights=' + stringifyWeights(vein.ore_weights));
    console.log('  outputs=' + stringifyOutputs(outputs));

    builder = event.recipes.gtceu.local_transfer('local_transfer/' + vein.id)
      .circuit(circuit)
      .inputFluids(getAirFluid(vein))
      .duration(getDuration(vein))
      .EUt(getEUt(vein));

    applyOutputs(builder, outputs);
    built++;
  }

  console.log('[local_transfer] built=' + built);
  console.log('[local_transfer] biome counts=' + JSON.stringify(byBiome));
  console.log('[local_transfer] fluid counts=' + JSON.stringify(byFluid));

  for (i = 0; i <= 32; i++) {
    if (byCircuit[i] && byCircuit[i].length > 1) {
      console.log('[local_transfer] circuit conflict ' + i + ' -> ' + byCircuit[i].join(', '));
    }
  }
});
