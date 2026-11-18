GTCEuStartupEvents.registry('gtceu:material', event => {
    [
        ['calcium_fluoride', 0xDCDCE4, 'CaF2'], ['magnesium_fluoride', 0xE8E3D4, 'MgF2'],
        ['sodium_sulfate', 0xECE9DA, 'Na2SO4']
    ].forEach(([id, color, formula]) => event.create('gtcsolo:' + id)
        .dust().color(color).iconSet(GTMaterialIconSet.DULL).formula(formula)
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION));

    [
        ['zinc_sulfate_solution', 0xAEBCC0], ['ammoniacal_condensate', 0xBCC8B5]
    ].forEach(([id, color]) => event.create('gtcsolo:' + id)
        .liquid(new GTFluidBuilder().temperature(300)).color(color));
});
