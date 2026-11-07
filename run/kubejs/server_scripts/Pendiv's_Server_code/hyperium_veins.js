GTCEuServerEvents.oreVeins(event => {
    const Blocks = Java.loadClass('net.minecraft.world.level.block.Blocks');

    // New Nether chunks only. Netherrack replacement rules leave the bedrock roof intact.
    // Both the metal ore and vanilla ancient debris belong to this same shallow cluster.
    event.add('gtcsolo:hyperium_roof_vein', vein => {
        vein.layer('netherrack').dimensions('minecraft:the_nether').biomes('#minecraft:is_nether')
            .weight(8).clusterSize(12).density(0.15).discardChanceOnAirExposure(0)
            .heightRangeUniform(112, 122);
        vein.layeredVeinGenerator(generator => generator.buildLayerPattern(pattern => pattern
            .layer(l => l.weight(3).mat(GTMaterials.get('gtcsolo:hyperium')).size(1, 2))
            .layer(l => l.weight(1).state(Blocks.ANCIENT_DEBRIS.defaultBlockState()).size(1, 1))));
    });
});
