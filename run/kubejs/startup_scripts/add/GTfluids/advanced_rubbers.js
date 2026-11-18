// Final rubber products; manufacturing is in advanced_rubber_processing.js.
// Polymer formulas remain provisional. Consumption alternatives are derived in rubber_alternatives.js.
// Progression: existing rubber -> HNBR (/4) -> FFKM (/16); original recipes are retained.
GTCEuStartupEvents.registry('gtceu:material', event => {
    [
        ['hydrogenated_nitrile_butadiene_rubber', 0x3F4B45],
        ['perfluoroelastomer', 0x596C85]
    ].forEach(entry => {
        event.create('gtcsolo:' + entry[0])
            .polymer()
            // Follow the native SBR processing-fluid convention; this is not a service-temperature rating.
            .liquid(new GTFluidBuilder().temperature(1000))
            .color(entry[1]).iconSet(GTMaterialIconSet.DULL)
            .flags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_DENSE, GTMaterialFlags.GENERATE_FOIL,
                GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_LONG_ROD, GTMaterialFlags.GENERATE_ROUND,
                GTMaterialFlags.GENERATE_RING, GTMaterialFlags.GENERATE_BOLT_SCREW,
                GTMaterialFlags.GENERATE_GEAR, GTMaterialFlags.GENERATE_SMALL_GEAR,
                GTMaterialFlags.GENERATE_SPRING, GTMaterialFlags.GENERATE_SPRING_SMALL);
    });
});
