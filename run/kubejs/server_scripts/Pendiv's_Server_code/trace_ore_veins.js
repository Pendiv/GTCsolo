GTCEuServerEvents.oreVeins(event => {
    // Zinc ore can contain both Ge- and In-bearing sphalerite.
    event.add('gtcsolo:trace_zinc_vein', vein => {
        vein.layer('stone').dimensions('minecraft:overworld').biomes('#minecraft:is_overworld')
            .weight(20).clusterSize(28).density(0.24).discardChanceOnAirExposure(0)
            .heightRangeUniform(8, 45);
        vein.layeredVeinGenerator(generator => generator.buildLayerPattern(pattern => pattern
            .layer(l => l.weight(4).mat(GTMaterials.Sphalerite).size(1, 2))
            .layer(l => l.weight(3).mat(GTMaterials.get('gtcsolo:germanium_bearing_sphalerite')).size(1, 2))
            .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:indium_bearing_sphalerite')).size(1, 1))
            .layer(l => l.weight(2).mat(GTMaterials.Galena).size(1, 1))));
        vein.surfaceIndicatorGenerator(indicator => indicator.surfaceRock(GTMaterials.Sphalerite)
            .placement('above').density(0.2).radius(5));
    });

    // Rhenium substitutes for molybdenum in the molybdenite of some porphyry Cu–Au deposits.
    event.add('gtcsolo:porphyry_copper_molybdenum_gold_vein', vein => {
        vein.layer('deepslate').dimensions('minecraft:overworld').biomes('#minecraft:is_overworld')
            .weight(16).clusterSize(38).density(0.26).discardChanceOnAirExposure(0)
            .heightRangeUniform(-48, -12);
        vein.layeredVeinGenerator(generator => generator.buildLayerPattern(pattern => pattern
            .layer(l => l.weight(5).mat(GTMaterials.Chalcopyrite).size(2, 3))
            .layer(l => l.weight(2).mat(GTMaterials.get('gtcsolo:rhenium_bearing_molybdenite')).size(1, 2))
            .layer(l => l.weight(2).mat(GTMaterials.Bornite).size(1, 2))
            .layer(l => l.weight(1).mat(GTMaterials.Gold).size(1, 1))));
        vein.surfaceIndicatorGenerator(indicator => indicator.surfaceRock(GTMaterials.Chalcopyrite)
            .placement('above').density(0.2).radius(5));
    });
});
