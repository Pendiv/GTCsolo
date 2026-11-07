// Real isotope-enrichment feedstocks; process fractions keep their zinc-70 identity.
GTCEuStartupEvents.registry('gtceu:element', event => {
    event.create('gtcsolo:zinc_70').protons(30).neutrons(40).symbol('Zn-70').isIsotope(true);
});

GTCEuStartupEvents.registry('gtceu:material', event => {
    [
        ['diethyl_ether', 0xE5ECE2, 'C4H10O'],
        ['ethyl_iodide', 0xB8A8C9, 'C2H5I'],
        ['crude_diethylzinc', 0xADB6AE, null]
    ].forEach(entry => {
        const material = event.create('gtcsolo:' + entry[0]).liquid()
            .color(entry[1]).iconSet(GTMaterialIconSet.FLUID)
            .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
        if (entry[2] !== null) material.formula(entry[2]);
    });
    [
        ['diethylzinc', 0xDCE7DF],
        ['zinc_70_low_enriched_diethylzinc', 0xB7CFCA],
        ['zinc_70_enriched_diethylzinc', 0x90B8C5],
        ['zinc_70_high_enriched_diethylzinc', 0x749BC7],
        ['zinc_70_depleted_diethylzinc', 0xA4AAA6]
    ].forEach(entry => {
        event.create('gtcsolo:' + entry[0])
            .gas(new GTFluidBuilder().temperature(400))
            .color(entry[1]).iconSet(GTMaterialIconSet.FLUID).formula('Zn(C2H5)2')
            .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
    });
    event.create('gtcsolo:zinc_iodide').dust().color(0xD9D2E6).iconSet(GTMaterialIconSet.DULL)
        .formula('ZnI2').flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
    event.create('gtcsolo:zinc_70').ingot().dust()
        .liquid(new GTFluidBuilder().temperature(693))
        .color(0xEBEBFA).iconSet(GTMaterialIconSet.METALLIC).element('zinc_70')
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION, GTMaterialFlags.GENERATE_PLATE,
            GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_ROD);
    // Do not unify this tail product back into natural zinc and manufacture fresh 70Zn on every recycle.
    event.create('gtcsolo:zinc_70_depleted').dust().color(0xB4BDC1).iconSet(GTMaterialIconSet.METALLIC)
        .formula('Zn').flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
});

GTCEuStartupEvents.materialModification(event => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    const IngotProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty');
    const FluidProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty');
    const FluidStorageKeys = Java.loadClass('com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys');
    // Reuse the native element, ID, translations and speculative radioactive icon palette.
    const material = GTMaterials.Nihonium;
    if (!material.hasProperty(PropertyKey.DUST)) material.setProperty(PropertyKey.DUST, new DustProperty());
    if (!material.hasProperty(PropertyKey.INGOT)) material.setProperty(PropertyKey.INGOT, new IngotProperty());
    if (!material.hasProperty(PropertyKey.FLUID)) {
        // The fluid's 1000 K display temperature is a game setting, not a measured melting point.
        material.setProperty(PropertyKey.FLUID,
            new FluidProperty(FluidStorageKeys.LIQUID, new GTFluidBuilder().temperature(1000)));
    }
    material.addFlags(GTMaterialFlags.DISABLE_DECOMPOSITION, GTMaterialFlags.GENERATE_PLATE,
        GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_BOLT_SCREW,
        GTMaterialFlags.GENERATE_RING, GTMaterialFlags.GENERATE_FINE_WIRE);
});
