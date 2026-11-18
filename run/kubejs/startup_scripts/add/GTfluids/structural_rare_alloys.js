StartupEvents.registry('item', event => {
    event.create('yttrium_refractory_silicide_plate')
        .texture('gtceu:item/material_sets/metallic/plate');
});

GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:yttrium_refractory_high_entropy_alloy')
        .ingot().dust().liquid(new GTFluidBuilder().temperature(2600))
        .color(0x858A90).secondaryColor(0x5C6269).iconSet(GTMaterialIconSet.METALLIC)
        .flags(GTMaterialFlags.NO_SMELTING, GTMaterialFlags.DISABLE_DECOMPOSITION,
            GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL,
            GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_FRAME);

    event.create('gtcsolo:as_cast_aluminium_yb_zr')
        .ingot().dust().liquid(new GTFluidBuilder().temperature(933))
        .color(0xB0BBC0).secondaryColor(0x7D9098).iconSet(GTMaterialIconSet.METALLIC)
        .flags(GTMaterialFlags.NO_SMELTING, GTMaterialFlags.DISABLE_DECOMPOSITION);

    event.create('gtcsolo:aged_aluminium_yb_zr')
        .ingot().dust()
        .color(0xC3D1D6).secondaryColor(0x849DA5).iconSet(GTMaterialIconSet.METALLIC)
        .flags(GTMaterialFlags.NO_SMELTING, GTMaterialFlags.DISABLE_DECOMPOSITION,
            GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL,
            GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_FRAME);
});

GTCEuStartupEvents.materialModification(() => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    const IngotProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty');
    const FluidProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty');
    const FluidStorageKeys = Java.loadClass('com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys');
    const ytterbium = GTMaterials.get('gtceu:ytterbium');
    if (!ytterbium.hasProperty(PropertyKey.DUST)) ytterbium.setProperty(PropertyKey.DUST, new DustProperty());
    if (!ytterbium.hasProperty(PropertyKey.INGOT)) ytterbium.setProperty(PropertyKey.INGOT, new IngotProperty());
    if (!ytterbium.hasProperty(PropertyKey.FLUID)) {
        ytterbium.setProperty(PropertyKey.FLUID,
            new FluidProperty(FluidStorageKeys.LIQUID, new GTFluidBuilder().temperature(1097)));
    }
});
