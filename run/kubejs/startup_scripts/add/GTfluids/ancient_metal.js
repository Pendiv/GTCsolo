// Fluid-only process materials; keep the existing ancient-debris dust and Sculk material.
GTCEuStartupEvents.registry('gtceu:material', event => {
    [
        ['liquid_ancient_debris', 0x665047],
        ['molten_ancient_metal_slurry', 0x86533E],
        ['chi_molten_magma', 0x275B59]
    ].forEach(entry => {
        event.create('gtcsolo:' + entry[0])
            .fluid()
            .color(entry[1])
            .iconSet(GTMaterialIconSet.FLUID)
            .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
    });
});
