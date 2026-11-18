// The scandium-gadolinium-terbium-dysprosium-holmium alloy is a magnetic refrigerant.
GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:rare_earth_magnetocaloric_alloy')
        .ingot().liquid(new GTFluidBuilder().temperature(1850))
        .color(0xA8B6AA).iconSet(GTMaterialIconSet.METALLIC)
        .formula('ScGdTbDyHo')
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION, GTMaterialFlags.GENERATE_PLATE,
            GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_ROD,
            GTMaterialFlags.GENERATE_RING, GTMaterialFlags.GENERATE_BOLT_SCREW);
});

GTCEuStartupEvents.materialModification(() => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    const IngotProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty');
    const FluidProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty');
    const FluidStorageKeys = Java.loadClass('com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys');
    const material = GTMaterials.Gadolinium;

    if (!material.hasProperty(PropertyKey.DUST)) material.setProperty(PropertyKey.DUST, new DustProperty());
    if (!material.hasProperty(PropertyKey.INGOT)) material.setProperty(PropertyKey.INGOT, new IngotProperty());
    if (!material.hasProperty(PropertyKey.FLUID)) {
        material.setProperty(PropertyKey.FLUID,
            new FluidProperty(FluidStorageKeys.LIQUID, new GTFluidBuilder().temperature(1585)));
    }
    material.addFlags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL,
        GTMaterialFlags.GENERATE_ROD);
});
