// Red gold. Native material forms, processing recipes and cable size scaling.
GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:auric')
        .ingot().dust().fluid()
        .color(0xE5A23D)
        .iconSet(GTMaterialIconSet.SHINY)
        .formula('𝄽Ag')
        // One ampere per conductor; the normal cable has a loss of 2 EU per block.
        .cableProperties(GTValues.V[GTValues.UEV], 1, 2)
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
