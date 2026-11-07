// Preserve the depleted isotope composition through oxidation; do not unify this with natural zinc.
GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:zinc_70_depleted_oxide')
        .dust().color(0xEEEDE3).iconSet(GTMaterialIconSet.DULL)
        .formula('ZnO').flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
});
