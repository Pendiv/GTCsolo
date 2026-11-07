GTCEuServerEvents.oreVeins(event => {
    // Low-sulfur hydrothermal selenium deposits: uraninite and gold are documented associates.
    // Galena is the PbS endmember of the same mineral series as PbSe.
    // Depth, weight and size are game balance, not measured geological depth distributions.
    event.add('gtcsolo:selenium_hydrothermal_vein', vein => {
        vein.layer('deepslate').dimensions('minecraft:overworld').biomes('#minecraft:is_overworld')
            .weight(12).clusterSize(24).density(0.20).discardChanceOnAirExposure(0)
            .heightRangeUniform(-48, -8);
        vein.layeredVeinGenerator(generator => generator.buildLayerPattern(pattern => pattern
            .layer(l => l.weight(4).mat(GTMaterials.get('gtcsolo:clausthalite')).size(1, 2))
            .layer(l => l.weight(2).mat(GTMaterials.Uraninite).size(1, 2))
            .layer(l => l.weight(1).mat(GTMaterials.Gold).size(1, 1))
            .layer(l => l.weight(1).mat(GTMaterials.Galena).size(1, 2))));
        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:clausthalite'))
            .placement('above').density(0.2).radius(5));
    });

    // Calaverite is a gold telluride of hydrothermal veins, associated with pyrite/tetrahedrite.
    event.add('gtcsolo:gold_telluride_vein', vein => {
        vein.layer('stone').dimensions('minecraft:overworld').biomes('#minecraft:is_overworld')
            .weight(16).clusterSize(28).density(0.20).discardChanceOnAirExposure(0)
            .heightRangeUniform(8, 48);
        vein.layeredVeinGenerator(generator => generator.buildLayerPattern(pattern => pattern
            .layer(l => l.weight(3).mat(GTMaterials.get('gtcsolo:calaverite')).size(1, 2))
            .layer(l => l.weight(2).mat(GTMaterials.Gold).size(1, 2))
            .layer(l => l.weight(3).mat(GTMaterials.Pyrite).size(1, 3))
            .layer(l => l.weight(1).mat(GTMaterials.Tetrahedrite).size(1, 1))));
        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:calaverite'))
            .placement('above').density(0.2).radius(5));
    });
});
