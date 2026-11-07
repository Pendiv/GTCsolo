// Material forms and symbols. Selected acquisition routes are defined in nuclear_fusion.js.
// Existing IDs, translations and palettes remain authoritative.
GTCEuStartupEvents.registry('gtceu:element', event => {
    event.create('gtcsolo:plutonium_238')
        .protons(94).neutrons(144).symbol('Pu-238').isIsotope(true);
});

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Distinct from native plutonium (239) and plutonium_241. No shared plutonium input tag.
    event.create('gtcsolo:plutonium_238')
        .ingot().dust().liquid(new GTFluidBuilder().temperature(913))
        .element('plutonium_238')
        .color(0xD83A36).secondaryColor(0x222730).iconSet(GTMaterialIconSet.RADIOACTIVE)
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION, GTMaterialFlags.GENERATE_PLATE,
            GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_ROD,
            GTMaterialFlags.GENERATE_RING, GTMaterialFlags.GENERATE_BOLT_SCREW);
});

GTCEuStartupEvents.materialModification(event => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    const IngotProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty');
    const FluidProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty');
    const FluidStorageKeys = Java.loadClass('com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys');
    const ForgeRegistries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries');
    const ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');
    const californiumFluid = new ResourceLocation('kubejs:liquid_californium');

    // [existing material ID, elemental formula, metal forms]
    // The four gtcsolo materials are existing StarForge outputs, not new duplicate elements.
    const materials = [
        ['gtcsolo:californium', 'Cf', true],
        ['gtceu:berkelium', 'Bk', true],
        ['gtcsolo:einsteinium', 'Es', true],
        ['gtceu:fermium', 'Fm', true],
        ['gtcsolo:promethium', 'Pm', true],
        ['gtceu:actinium', 'Ac', true],
        ['gtceu:astatine', 'At', false],
        ['gtceu:neptunium', 'Np', true],
        ['gtceu:curium', 'Cm', true],
        ['gtceu:darmstadtium', 'Ds', true],
        ['gtceu:roentgenium', 'Rg', true],
        ['gtceu:copernicium', 'Cn', true],
        ['gtcsolo:technetium', 'Tc', true],
        ['gtceu:rhenium', 'Re', true],
        ['gtceu:scandium', 'Sc', true],
        ['gtceu:protactinium', 'Pa', true],
        ['gtceu:mendelevium', 'Md', true],
        ['gtceu:nobelium', 'No', true],
        ['gtceu:lawrencium', 'Lr', true],
        ['gtceu:rutherfordium', 'Rf', true],
        ['gtceu:dubnium', 'Db', true],
        ['gtceu:seaborgium', 'Sg', true],
        ['gtceu:bohrium', 'Bh', true],
        ['gtceu:hassium', 'Hs', true],
        ['gtceu:meitnerium', 'Mt', true],
        ['gtceu:flerovium', 'Fl', true],
        ['gtceu:moscovium', 'Mc', true],
        ['gtceu:livermorium', 'Lv', true],
        ['gtceu:tennessine', 'Ts', false],
        ['gtceu:oganesson', 'Og', false],
        // Fluid-only support for the approved calcium/hydrogen and californium/boron routes.
        ['gtceu:calcium', 'Ca', false],
        ['gtceu:boron', 'B', false]
    ];
    materials.forEach(entry => {
        const material = GTMaterials.get(entry[0]);
        const fluidProperty = new FluidProperty();
        material.setFormula(entry[1]);
        if (!material.hasProperty(PropertyKey.DUST)) material.setProperty(PropertyKey.DUST, new DustProperty());
        if (entry[2] && !material.hasProperty(PropertyKey.INGOT)) material.setProperty(PropertyKey.INGOT, new IngotProperty());
        if (!material.hasProperty(PropertyKey.FLUID)) {
            if (entry[0] === 'gtcsolo:californium') {
                // Lazy binding preserves the established Mekanism rotary / Anteria path and its fluid ID.
                fluidProperty.store(FluidStorageKeys.LIQUID,
                    () => ForgeRegistries.FLUIDS.getValue(californiumFluid), null);
            } else {
                // Native default processing fluid; no experimental melting-point claim for ultraheavy elements.
                fluidProperty.enqueueRegistration(FluidStorageKeys.LIQUID, new GTFluidBuilder());
            }
            material.setProperty(PropertyKey.FLUID, fluidProperty);
        }
        material.addFlags(GTMaterialFlags.DISABLE_DECOMPOSITION);
        if (entry[2]) material.addFlags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.GENERATE_FOIL,
            GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_RING, GTMaterialFlags.GENERATE_BOLT_SCREW);
    });
});
