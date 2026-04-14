// priority: 0

GTCEuServerEvents.oreVeins(event => {



    event.add('kubejs:ameijia_hafhnium', vein => {
        vein.weight(30)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(30, 80)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(2).mat(GTMaterials.get('hafhnium')).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Naquadah).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Platinum).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('hafhnium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_enigma', vein => {
        vein.weight(10)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(2).mat(GTMaterials.get('enigma')).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Ilmenite).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Molybdenite).size(2, 3))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('enigma'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_stellarium', vein => {
        vein.weight(10)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('stellarium')).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Manganese).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.Bastnasite).size(1, 1))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('stellarium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_bedrockium', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 80)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('bedrockium')).size(1, 2))
                .layer(l => l.weight(2).mat(GTMaterials.Cobaltite).size(2, 3))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('bedrockium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_valinium', vein => {
        vein.weight(50)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(30, 120)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(3).mat(GTMaterials.get('valinium')).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Cobaltite).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('valinium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_stagnanted_neutronium', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-20, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('stagnanted_neutronium')).size(1, 2))
                .layer(l => l.weight(2).mat(GTMaterials.Naquadah).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('stagnanted_neutronium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_flaurite', vein => {
        vein.weight(40)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 80)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(3).mat(GTMaterials.get('flaurite')).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Cobaltite).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Bastnasite).size(1, 3))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('flaurite'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_exporonium', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-30, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(2).mat(GTMaterials.get('exporonium')).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Naquadah).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Neodymium).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('exporonium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_jupitate', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-60, -30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('jupitate')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('bedrockium')).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Platinum).size(1, 2))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('jupitate'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_stellarium_enigma', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-60, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('stellarium_enigma')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('enigma')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('stellarium')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('bedrockium')).size(1, 2))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('stellarium_enigma'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

    event.add('kubejs:ameijia_dense_stellarium_enigma', vein => {
        vein.weight(10)
        vein.clusterSize(24)
        vein.density(0.22)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-60, -30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(3).mat(GTMaterials.get('stellarium_enigma')).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.get('jupitate')).size(1, 2))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('stellarium_enigma'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    });

});