// priority: 0

GTCEuServerEvents.oreVeins(event => {

    event.add('gtcsolo:ameijia_hafhnium', vein => {
        vein.weight(30)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(30, 80)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(2).mat(GTMaterials.get('gtceu:hafnium')).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Naquadah).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Platinum).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtceu:hafnium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_enigma', vein => {
        vein.weight(10)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(2).mat(GTMaterials.get('gtcsolo:enigma')).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Ilmenite).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Molybdenite).size(2, 3))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:enigma'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_stellarium', vein => {
        vein.weight(10)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:stellarium')).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Pyrolusite).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.Bastnasite).size(1, 1))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:stellarium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_bedrockium', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 80)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:bedrockium')).size(1, 2))
                .layer(l => l.weight(2).mat(GTMaterials.Cobaltite).size(2, 3))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:bedrockium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_valinium', vein => {
        vein.weight(50)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(30, 120)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(3).mat(GTMaterials.get('gtcsolo:valinium')).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Cobaltite).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:valinium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_stagnanted_neutronium', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-20, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:stagnanted_neutronium')).size(1, 2))
                .layer(l => l.weight(2).mat(GTMaterials.Naquadah).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:stagnanted_neutronium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_flaurite', vein => {
        vein.weight(40)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(0, 80)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(3).mat(GTMaterials.get('gtcsolo:flaurite')).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
                .layer(l => l.weight(3).mat(GTMaterials.Cobaltite).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Bastnasite).size(1, 3))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:flaurite'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_exporonium', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-30, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(2).mat(GTMaterials.get('gtcsolo:exporonium')).size(2, 4))
                .layer(l => l.weight(2).mat(GTMaterials.Naquadah).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Neodymium).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:exporonium'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_jupitate', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-60, -30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:jupitate')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:bedrockium')).size(1, 2))
                .layer(l => l.weight(3).mat(GTMaterials.Aluminium).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.Platinum).size(1, 2))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:jupitate'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_stellarium_enigma', vein => {
        vein.weight(20)
        vein.clusterSize(18)
        vein.density(0.15)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-60, 30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:stellarium_enigma')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:enigma')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:stellarium')).size(1, 2))
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:bedrockium')).size(1, 2))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:stellarium_enigma'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

    event.add('gtcsolo:ameijia_dense_stellarium_enigma', vein => {
        vein.weight(10)
        vein.clusterSize(24)
        vein.density(0.22)
        vein.discardChanceOnAirExposure(0)

        vein.layer('ameijia')
        vein.heightRangeUniform(-60, -30)

        vein.layeredVeinGenerator(generator => generator
            .buildLayerPattern(pattern => pattern
                .layer(l => l.weight(3).mat(GTMaterials.get('gtcsolo:stellarium_enigma')).size(2, 4))
                .layer(l => l.weight(1).mat(GTMaterials.get('gtcsolo:jupitate')).size(1, 2))
            )
        )

        vein.surfaceIndicatorGenerator(indicator => indicator
            .surfaceRock(GTMaterials.get('gtcsolo:stellarium_enigma'))
            .placement('above')
            .density(0.4)
            .radius(5)
        )
    })

})
