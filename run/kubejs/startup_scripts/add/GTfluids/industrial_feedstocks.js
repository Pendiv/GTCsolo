// Activate native placeholder elements required by the industrial alloy recipes.
GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:zircon').dust().color(0xB78D61).iconSet(GTMaterialIconSet.DULL)
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
});

GTCEuStartupEvents.materialModification(event => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    const IngotProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty');
    const FluidProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty');
    const FluidStorageKeys = Java.loadClass('com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys');
    [['germanium', 1250], ['zirconium', 2150]].forEach(entry => {
        const material = GTMaterials.get(entry[0]);
        if (!material.hasProperty(PropertyKey.DUST)) material.setProperty(PropertyKey.DUST, new DustProperty());
        if (!material.hasProperty(PropertyKey.INGOT)) material.setProperty(PropertyKey.INGOT, new IngotProperty());
        if (!material.hasProperty(PropertyKey.FLUID)) {
            material.setProperty(PropertyKey.FLUID,
                new FluidProperty(FluidStorageKeys.LIQUID, new GTFluidBuilder().temperature(entry[1])));
        }
    });
    const MaterialStack = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack');
    GTMaterials.get('gtcsolo:zircon').setComponents([
        new MaterialStack(GTMaterials.Zirconium, 1), new MaterialStack(GTMaterials.Silicon, 1),
        new MaterialStack(GTMaterials.Oxygen, 4)
    ]);
});
