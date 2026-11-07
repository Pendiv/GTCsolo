// Native material generation with the user's full-color, translucent icon set.
GTCEuStartupEvents.registry('gtceu:material_icon_set', event => {
    event.create('entronium').parent(GTMaterialIconSet.SHINY);
});

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Keep the solid separate from the liquid so the native extractor cannot bypass the UV furnace.
    event.create('gtcsolo:entro_crystal')
        .gem().dust()
        .color(0x038383)
        .iconSet(GTMaterialIconSet.EMERALD)
        .formula('=E')
        .ignoredTagPrefixes('block', 'chippedGem', 'flawedGem', 'flawlessGem', 'exquisiteGem')
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);

    event.create('gtcsolo:liquid_entro')
        .liquid()
        // Sampled from ExtendedAE's original Entro Crystal palette.
        .color(0x038383)
        .iconSet(GTMaterialIconSet.FLUID)
        .formula('=E')
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);

    event.create('gtcsolo:ma_fluid')
        .liquid(new GTFluidBuilder().temperature(293).textures(true, true))
        .color(0xDFFFF7)
        .iconSet(GTMaterialIconSet.FLUID)
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);

    event.create('gtcsolo:entronium')
        .ingot().dust()
        .liquid(new GTFluidBuilder().temperature(293).textures(true, true))
        // White leaves supplied pipe pixels unchanged; item models use an untinted layer.
        .color(0xFFFFFF)
        .iconSet(GTMaterialIconSet.getByName('entronium'))
        .formula('⇒E')
        // Provisional pipe/electrical ratings; no material acquisition shortcut is generated.
        .cableProperties(8388608, 1, 2)
        // GTCEu 1.6.4 forbids fluid and item pipe properties on the same material.
        .fluidPipeProperties(200000, 10000, true, true, true, true)
        .flags(
            GTMaterialFlags.DISABLE_DECOMPOSITION,
            GTMaterialFlags.GENERATE_PLATE,
            GTMaterialFlags.GENERATE_DENSE,
            GTMaterialFlags.GENERATE_FOIL,
            GTMaterialFlags.GENERATE_ROD,
            GTMaterialFlags.GENERATE_LONG_ROD,
            GTMaterialFlags.GENERATE_BOLT_SCREW,
            GTMaterialFlags.GENERATE_RING,
            GTMaterialFlags.GENERATE_SPRING,
            GTMaterialFlags.GENERATE_SPRING_SMALL,
            GTMaterialFlags.GENERATE_GEAR,
            GTMaterialFlags.GENERATE_SMALL_GEAR,
            GTMaterialFlags.GENERATE_FINE_WIRE,
            GTMaterialFlags.GENERATE_ROTOR,
            GTMaterialFlags.GENERATE_ROUND,
            GTMaterialFlags.GENERATE_FRAME,
            GTMaterialFlags.FORCE_GENERATE_BLOCK);
});
