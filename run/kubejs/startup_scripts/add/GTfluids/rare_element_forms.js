GTCEuStartupEvents.materialModification(() => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    const IngotProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty');
    const FluidProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty');
    const FluidStorageKeys = Java.loadClass('com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys');
    const liquid = FluidStorageKeys.LIQUID;

    const forms = [GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_DENSE,
        GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_FINE_WIRE,
        GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_LONG_ROD,
        GTMaterialFlags.GENERATE_BOLT_SCREW, GTMaterialFlags.GENERATE_RING,
        GTMaterialFlags.GENERATE_ROUND, GTMaterialFlags.GENERATE_FRAME,
        GTMaterialFlags.GENERATE_GEAR, GTMaterialFlags.GENERATE_SMALL_GEAR,
        GTMaterialFlags.GENERATE_SPRING, GTMaterialFlags.GENERATE_SPRING_SMALL];

    function ensureMaterial(id, temperature, metalForms) {
        const material = GTMaterials.get(id);
        if (!material.hasProperty(PropertyKey.DUST)) material.setProperty(PropertyKey.DUST, new DustProperty());
        if (metalForms) {
            if (!material.hasProperty(PropertyKey.INGOT)) material.setProperty(PropertyKey.INGOT, new IngotProperty());
            material.addFlags(forms);
        }
        if (!material.hasProperty(PropertyKey.FLUID)) {
            material.setProperty(PropertyKey.FLUID, new FluidProperty(liquid, new GTFluidBuilder().temperature(temperature)));
            return;
        }
        const storage = material.getProperty(PropertyKey.FLUID);
        if (storage.getQueuedBuilder(liquid) == null && storage.getEntry(liquid) == null) {
            storage.enqueueRegistration(liquid, new GTFluidBuilder().temperature(temperature));
        }
    }

    [
        ['gtceu:rhenium', 3459], ['gtceu:indium', 430], ['gtceu:gallium', 303],
        ['gtceu:yttrium', 1799], ['gtcsolo:dysprosium', 1680], ['gtceu:terbium', 1629],
        ['gtceu:scandium', 1814], ['gtceu:thallium', 577], ['gtceu:zirconium', 2128],
        ['gtceu:hafnium', 2506], ['gtceu:niobium', 2750], ['gtceu:tantalum', 3290],
        ['gtceu:bismuth', 545], ['gtceu:cobalt', 1768], ['gtceu:beryllium', 1560],
        ['gtceu:strontium', 1050], ['gtceu:uranium', 1405], ['gtceu:vanadium', 2183],
        ['gtceu:germanium', 1211], ['gtceu:rubidium', 312]
    ].forEach(([id, temperature]) => ensureMaterial(id, temperature, true));

    [
        ['gtceu:selenium', 494], ['gtceu:tellurium', 723], ['gtceu:boron', 2349],
        ['gtcsolo:holmium', 1734]
    ].forEach(([id, temperature]) => ensureMaterial(id, temperature, false));
});
