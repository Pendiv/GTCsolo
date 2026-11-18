GTCEuStartupEvents.registry('gtceu:material', event => {
    const elements = GTMaterialFlags.DECOMPOSITION_BY_ELECTROLYZING;
    const closed = GTMaterialFlags.DISABLE_DECOMPOSITION;
    const dust = (id, color, decomposition) => event.create('gtcsolo:' + id).dust().color(color)
        .iconSet(GTMaterialIconSet.DULL).flags(decomposition, GTMaterialFlags.NO_SMELTING);

    // Fixed-composition sulfides use the native GregTech decomposition handler.
    dust('bismuthinite', 0x7E7781, elements).ore(1, 1, false);
    dust('gallite', 0x9A7563, elements).ore(1, 1, false);

    // Compositionally variable ores intentionally retain their refining routes.
    const ores = [
        ['loparite', 0x494449], ['eudialyte', 0xA84958], ['germanite', 0x67546B],
        ['heterogenite', 0x484751, 'CoO(OH)'], ['bertrandite', 0xD2C6A3, 'Be4Si2O7(OH)2'],
        ['celestite', 0xA3C6DD, 'SrSO4'], ['carnotite', 0xC8A745, 'K2(UO2)2(VO4)2·3H2O'],
        ['ulexite', 0xDFD8C4, 'NaCaB5O6(OH)6·5H2O'], ['colemanite', 0xD7CBB3, 'Ca2B6O11·5H2O'],
        ['cryolite', 0xE6ECEF, 'Na3AlF6']
    ];
    ores.forEach(([id, color, formula]) => {
        const mineral = dust(id, color, closed).ore(1, 1, false);
        if (formula) mineral.formula(formula);
    });

    [
        ['niobium_pentachloride', 0xC1A549, 'NbCl5'], ['tantalum_pentachloride', 0xE4D8AC, 'TaCl5'],
        ['zirconium_hafnium_concentrate', 0xB59785], ['beryllium_hydroxide', 0xD7D9CA, 'Be(OH)2'],
        ['strontium_sulfide', 0xD4C4AE, 'SrS'], ['alumina', 0xE1DFD9, 'Al2O3'],
        ['sodium_aluminium_sulfate', 0xE2DFD7, 'NaAl(SO4)2']
    ].forEach(([id, color, formula]) => {
        const intermediate = dust(id, color, closed);
        if (formula) intermediate.formula(formula);
    });
    dust('alumina_abrasive', 0xE7E0D3, closed);
    dust('strontium_carbonate', 0xD5DFE1, elements);
    dust('boric_acid', 0xE2DFD0, elements);

    [
        ['cobalt_sulfate_leachate', 0x9C5367], ['carnotite_leachate', 0x8D8145]
    ].forEach(([id, color]) => event.create('gtcsolo:' + id)
        .liquid(new GTFluidBuilder().temperature(320)).color(color));
});

GTCEuStartupEvents.materialModification(() => {
    const MaterialStack = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack');
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');

    GTMaterials.get('gtcsolo:bismuthinite').setComponents([
        new MaterialStack(GTMaterials.Bismuth, 2), new MaterialStack(GTMaterials.Sulfur, 3)
    ]);
    GTMaterials.get('gtcsolo:gallite').setComponents([
        new MaterialStack(GTMaterials.Copper, 1), new MaterialStack(GTMaterials.Gallium, 1),
        new MaterialStack(GTMaterials.Sulfur, 2)
    ]);
    const strontium = GTMaterials.Strontium;
    if (!strontium.hasProperty(PropertyKey.DUST)) strontium.setProperty(PropertyKey.DUST, new DustProperty());
    GTMaterials.get('gtcsolo:strontium_carbonate').setComponents([
        new MaterialStack(strontium, 1), new MaterialStack(GTMaterials.Carbon, 1), new MaterialStack(GTMaterials.Oxygen, 3)
    ]);
    GTMaterials.get('gtcsolo:boric_acid').setComponents([
        new MaterialStack(GTMaterials.Hydrogen, 3), new MaterialStack(GTMaterials.Boron, 1),
        new MaterialStack(GTMaterials.Oxygen, 3)
    ]);
});
