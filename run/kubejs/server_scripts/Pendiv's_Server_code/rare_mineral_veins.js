GTCEuServerEvents.oreVeins(event => {
    function addVein(id, dimension, biomeTag, layer, height, size, density, weight, minerals) {
        event.add('gtcsolo:' + id, vein => {
            vein.layer(layer).dimensions(dimension).biomes(biomeTag)
                .weight(weight).clusterSize(size).density(density).discardChanceOnAirExposure(0)
                .heightRangeUniform(height[0], height[1]);
            vein.layeredVeinGenerator(generator => generator.buildLayerPattern(pattern => {
                minerals.forEach(entry => {
                    pattern.layer(l => l.weight(entry[1]).mat(entry[0]).size(1, entry[2]));
                });
                return pattern;
            }));
            vein.surfaceIndicatorGenerator(indicator => indicator.surfaceRock(minerals[0][0])
                .placement('above').density(0.2).radius(5));
        });
    }

    // The End placement is fictional. The mineral associations follow terrestrial deposits.
    addVein('end_heavy_mineral_sand_vein', 'minecraft:the_end', '#minecraft:is_end', 'endstone', [48, 72], 30, 0.24, 24, [
        [GTMaterials.Ilmenite, 4, 2], [GTMaterials.get('gtcsolo:zircon'), 3, 2],
        [GTMaterials.get('gtcsolo:xenotime'), 2, 1], [GTMaterials.Monazite, 1, 1]
    ]);
    addVein('end_baddeleyite_vein', 'minecraft:the_end', '#minecraft:is_end', 'endstone', [30, 55], 33, 0.23, 18, [
        [GTMaterials.get('gtcsolo:scandium_bearing_baddeleyite'), 2, 1],
        [GTMaterials.Magnetite, 4, 2], [GTMaterials.Apatite, 3, 2]
    ]);
    addVein('nether_thallium_hydrothermal_vein', 'minecraft:the_nether', '#minecraft:is_nether', 'netherrack', [18, 44], 23, 0.18, 10, [
        [GTMaterials.get('gtcsolo:thallium_bearing_orpiment'), 2, 1],
        [GTMaterials.Pyrite, 4, 2], [GTMaterials.Cinnabar, 2, 1], [GTMaterials.Gold, 1, 1]
    ]);

    const mineral = name => GTMaterials.get('gtcsolo:' + name);
    const end = ['minecraft:the_end', '#minecraft:is_end', 'endstone'];
    const nether = ['minecraft:the_nether', '#minecraft:is_nether', 'netherrack'];
    const surface = ['minecraft:overworld', '#minecraft:is_overworld', 'stone'];
    const dry = ['minecraft:overworld', 'minecraft:desert', 'stone'];

    addVein('end_loparite_vein', end[0], end[1], end[2], [30, 58], 32, 0.23, 16, [
        [mineral('loparite'), 4, 2], [GTMaterials.Ilmenite, 2, 2], [GTMaterials.Apatite, 1, 1]
    ]);
    addVein('end_eudialyte_vein', end[0], end[1], end[2], [38, 66], 28, 0.19, 14, [
        [mineral('eudialyte'), 4, 2], [mineral('zircon'), 2, 1], [GTMaterials.Magnetite, 1, 1]
    ]);
    addVein('end_bismuth_tungsten_vein', end[0], end[1], end[2], [24, 55], 32, 0.22, 17, [
        [GTMaterials.Scheelite, 5, 2], [mineral('bismuthinite'), 2, 1], [GTMaterials.Molybdenite, 1, 1]
    ]);
    addVein('nether_gallite_germanite_vein', nether[0], nether[1], nether[2], [15, 38], 29, 0.20, 15, [
        [GTMaterials.Bornite, 5, 2], [GTMaterials.Galena, 2, 1], [GTMaterials.Sphalerite, 2, 1],
        [mineral('gallite'), 1, 1], [mineral('germanite'), 1, 1]
    ]);
    addVein('copper_cobalt_oxide_vein', surface[0], surface[1], surface[2], [30, 62], 31, 0.23, 20, [
        [GTMaterials.Malachite, 5, 2], [mineral('heterogenite'), 3, 1], [GTMaterials.Chalcopyrite, 2, 1]
    ]);
    addVein('bertrandite_vein', surface[0], surface[1], surface[2], [20, 54], 27, 0.20, 17, [
        [mineral('bertrandite'), 4, 2], [GTMaterials.Quartzite, 3, 2], [GTMaterials.Calcite, 1, 1]
    ]);
    addVein('celestite_vein', surface[0], surface[1], surface[2], [15, 45], 29, 0.24, 21, [
        [mineral('celestite'), 4, 2], [GTMaterials.Gypsum, 3, 2], [GTMaterials.Calcite, 2, 1]
    ]);
    addVein('carnotite_sandstone_vein', dry[0], dry[1], dry[2], [35, 59], 29, 0.20, 36, [
        [mineral('carnotite'), 4, 1], [GTMaterials.Quartzite, 3, 1]
    ]);
    addVein('evaporite_borate_vein', dry[0], dry[1], dry[2], [38, 61], 32, 0.24, 40, [
        [mineral('ulexite'), 4, 2], [mineral('colemanite'), 3, 2], [GTMaterials.Gypsum, 2, 1]
    ]);
    addVein('end_cryolite_vein', end[0], end[1], end[2], [47, 69], 23, 0.18, 10, [
        [mineral('cryolite'), 5, 2], [GTMaterials.Quartzite, 2, 1], [GTMaterials.Pyrite, 1, 1]
    ]);

    addVein('nether_germanium_tin_sulfide_vein', nether[0], nether[1], nether[2], [24, 49], 28, 0.19, 12, [
        [mineral('germanium_tin_sulfide'), 3, 1], [GTMaterials.Cassiterite, 4, 2],
        [GTMaterials.Chalcopyrite, 3, 2], [GTMaterials.Sphalerite, 2, 1]
    ]);
    addVein('end_lithium_titanate_vein', end[0], end[1], end[2], [35, 61], 29, 0.21, 15, [
        [mineral('lithium_titanate'), 3, 2], [GTMaterials.Ilmenite, 4, 2], [GTMaterials.Quartzite, 2, 1]
    ]);
    addVein('end_hafnium_phosphate_vein', end[0], end[1], end[2], [25, 50], 26, 0.18, 10, [
        [mineral('hafnium_bearing_zirconium_phosphate'), 3, 1], [GTMaterials.Apatite, 4, 2],
        [mineral('zircon'), 2, 1]
    ]);
    addVein('nether_scandium_sulfate_vein', nether[0], nether[1], nether[2], [55, 83], 23, 0.16, 10, [
        [GTMaterials.Alunite, 5, 2], [mineral('potassium_scandium_sulfate'), 2, 1],
        [GTMaterials.Gypsum, 3, 2]
    ]);
    addVein('end_rubidium_pegmatite_vein', end[0], end[1], end[2], [40, 64], 27, 0.19, 13, [
        [mineral('rubidium_feldspar'), 3, 1], [GTMaterials.Quartzite, 5, 2],
        [GTMaterials.Lepidolite, 2, 1], [GTMaterials.Pollucite, 1, 1]
    ]);
    addVein('nether_cobalt_blue_spinel_vein', nether[0], nether[1], nether[2], [29, 56], 28, 0.21, 14, [
        [mineral('cobalt_blue_spinel'), 3, 1], [GTMaterials.Cobaltite, 2, 1], [GTMaterials.Bauxite, 4, 2]
    ]);
    addVein('end_strontium_powellite_vein', end[0], end[1], end[2], [26, 48], 26, 0.19, 12, [
        [mineral('strontium_powellite'), 3, 1], [GTMaterials.Powellite, 4, 2], [mineral('celestite'), 2, 1]
    ]);
    addVein('zinc_germanate_oxide_vein', surface[0], surface[1], surface[2], [22, 55], 24, 0.19, 12, [
        [mineral('zinc_germanate'), 2, 1], [GTMaterials.Sphalerite, 5, 2], [GTMaterials.Galena, 2, 1]
    ]);
    addVein('calcium_ferrite_vein', surface[0], surface[1], surface[2], [26, 56], 33, 0.25, 28, [
        [mineral('calcium_ferrite'), 5, 2], [GTMaterials.Calcite, 3, 2], [GTMaterials.Hematite, 2, 1]
    ]);
    addVein('nether_copper_ferrite_vein', nether[0], nether[1], nether[2], [25, 56], 32, 0.25, 25, [
        [mineral('copper_ferrite'), 5, 2], [GTMaterials.Magnetite, 3, 2], [GTMaterials.Bornite, 2, 1]
    ]);
});
