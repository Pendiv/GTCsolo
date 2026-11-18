// Pr-Nd-Fe-B alloy and its magnetized form share the native material shapes.
GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:praseodymium_oxide').dust().color(0x849877)
        .iconSet(GTMaterialIconSet.DULL).formula('Pr2O3')
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION, GTMaterialFlags.NO_SMELTING);
    event.create('gtcsolo:praseodymium_rare_earth_leachate')
        .liquid(new GTFluidBuilder().temperature(320)).color(0xA69E71);
    event.create('gtcsolo:praseodymium_neodymium_iron_boron')
        .ingot().liquid(new GTFluidBuilder().temperature(1500))
        .color(0x839786).iconSet(GTMaterialIconSet.METALLIC)
        .flags(GTMaterialFlags.GENERATE_LONG_ROD, GTMaterialFlags.DISABLE_DECOMPOSITION);
    event.create('gtcsolo:magnetic_praseodymium')
        .ingot().color(0x718060).secondaryColor(0x3F3447)
        .iconSet(GTMaterialIconSet.MAGNETIC)
        .flags(GTMaterialFlags.GENERATE_LONG_ROD, GTMaterialFlags.IS_MAGNETIC,
            GTMaterialFlags.DISABLE_DECOMPOSITION);
});

GTCEuStartupEvents.materialModification(() => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    const FluidProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty');
    const FluidStorageKeys = Java.loadClass('com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys');
    const material = GTMaterials.Praseodymium;
    if (!material.hasProperty(PropertyKey.DUST)) material.setProperty(PropertyKey.DUST, new DustProperty());
    if (!material.hasProperty(PropertyKey.FLUID)) {
        material.setProperty(PropertyKey.FLUID, new FluidProperty(FluidStorageKeys.LIQUID,
            new GTFluidBuilder().temperature(1208)));
    }
    const alloy = GTMaterials.get('gtcsolo:praseodymium_neodymium_iron_boron');
    const magnetic = GTMaterials.get('gtcsolo:magnetic_praseodymium');
    const magneticIngot = magnetic.getProperty(PropertyKey.INGOT);
    magneticIngot.setSmeltingInto(alloy);
    magneticIngot.setArcSmeltingInto(alloy);
    magneticIngot.setMacerateInto(alloy);
    // Do not set the alloy's magneticMaterial: GT would generate a low-voltage polarizer route.
});
