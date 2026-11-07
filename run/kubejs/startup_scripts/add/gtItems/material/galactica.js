// Ordinary material processing; portal animation is supplied by the client item model.
GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:galactica')
        .ingot().dust().fluid()
        .color(0xFFFFFF)
        .iconSet(GTMaterialIconSet.METALLIC)
        .flags(
            GTMaterialFlags.DISABLE_DECOMPOSITION,
            GTMaterialFlags.GENERATE_PLATE,
            GTMaterialFlags.GENERATE_DENSE,
            GTMaterialFlags.GENERATE_FOIL,
            GTMaterialFlags.GENERATE_ROD,
            GTMaterialFlags.GENERATE_LONG_ROD,
            GTMaterialFlags.GENERATE_BOLT_SCREW,
            GTMaterialFlags.GENERATE_RING,
            GTMaterialFlags.GENERATE_ROUND,
            GTMaterialFlags.GENERATE_GEAR,
            GTMaterialFlags.GENERATE_SMALL_GEAR,
            GTMaterialFlags.GENERATE_SPRING,
            GTMaterialFlags.GENERATE_SPRING_SMALL,
            GTMaterialFlags.GENERATE_ROTOR,
            GTMaterialFlags.GENERATE_FRAME,
            GTMaterialFlags.FORCE_GENERATE_BLOCK);
});
