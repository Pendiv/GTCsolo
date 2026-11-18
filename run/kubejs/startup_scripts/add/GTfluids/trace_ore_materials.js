GTCEuStartupEvents.registry('gtceu:material', event => {
    const dust = (id, color) => event.create('gtcsolo:' + id).dust().color(color)
        .iconSet(GTMaterialIconSet.DULL)
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION, GTMaterialFlags.NO_SMELTING);

    // The formulas describe the host minerals. The substituted elements are traces.
    dust('rhenium_bearing_molybdenite', 0x60677A).ore(1, 1, false).formula('MoS2');
    dust('germanium_bearing_sphalerite', 0x866645).ore(1, 1, false).formula('ZnS');
    dust('indium_bearing_sphalerite', 0x6E4A50).ore(1, 1, false).formula('ZnS');

    dust('molybdenum_trioxide', 0xE2DDD0).formula('MoO3');
    dust('zinc_oxide', 0xECE6D9).formula('ZnO');
    dust('rhenium_roasting_residue', 0x8C8073);
    dust('germanium_bearing_zinc_residue', 0x7E766A);
    dust('indium_bearing_zinc_residue', 0x896F81);
    dust('ammonium_perrhenate', 0xE5E3D6).formula('NH4ReO4');
    dust('germanium_dioxide', 0xDED9CF).formula('GeO2');
    event.create('gtcsolo:germanium_bearing_leachate').liquid(new GTFluidBuilder().temperature(300)).color(0x887948);
    event.create('gtcsolo:indium_bearing_leachate').liquid(new GTFluidBuilder().temperature(300)).color(0x78667F);
});
