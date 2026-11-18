GTCEuStartupEvents.registry('gtceu:material', event => {
    const dust = (id, color, formula, decomposition) => {
        const builder = event.create('gtcsolo:' + id).dust().color(color).iconSet(GTMaterialIconSet.DULL)
            .flags(decomposition, GTMaterialFlags.NO_SMELTING);
        if (formula) builder.formula(formula);
        return builder;
    };
    const closed = GTMaterialFlags.DISABLE_DECOMPOSITION;

    [
        ['germanium_tin_sulfide', 0x51474A, 'Cu2Zn(Sn1−xGex)S4'],
        ['lithium_titanate', 0xBCC7D8, 'Li4Ti5O12'],
        ['hafnium_bearing_zirconium_phosphate', 0xAFA3C7, 'Na(Zr1−xHfx)2(PO4)3'],
        ['potassium_scandium_sulfate', 0xD7C994, 'KSc(SO4)2'],
        ['rubidium_feldspar', 0xB29B8D, 'RbAlSi3O8'],
        ['cobalt_blue_spinel', 0x304C8C, 'CoAl2O4'],
        ['strontium_powellite', 0xAAB4A2, '(Ca1−xSrx)MoO4'],
        ['zinc_germanate', 0xB0C0A6, 'Zn2GeO4'],
        ['calcium_ferrite', 0x9B725E, 'CaFe2O4'],
        ['copper_ferrite', 0x634540, 'CuFe2O4']
    ].forEach(([id, color, formula]) => dust(id, color, formula, closed).ore(1, 1, false));

    dust('roasted_germanium_tin_ore', 0x866C62, null, closed);
    dust('tin_germanium_oxide_concentrate', 0xAAA08D, null, closed);
    dust('lithium_carbonate', 0xE5E2D3, 'Li2CO3', GTMaterialFlags.DECOMPOSITION_BY_ELECTROLYZING);
    dust('potassium_chloride', 0xE4D7B4, 'KCl', GTMaterialFlags.DECOMPOSITION_BY_ELECTROLYZING);
    dust('scandium_oxide', 0xE1DBD3, 'Sc2O3', closed);
    dust('rubidium_sulfate', 0xE0D2D9, 'Rb2SO4', GTMaterialFlags.DECOMPOSITION_BY_ELECTROLYZING);

    [
        ['lithium_titanate_leachate', 0xADB0C6], ['sodium_phosphate_liquor', 0xC4B1AF],
        ['scandium_sulfate_liquor', 0xBEB6A2], ['rubidium_sulfate_liquor', 0xB492A4],
        ['potassium_molybdate_liquor', 0xC6AF75]
    ].forEach(([id, color]) => event.create('gtcsolo:' + id)
        .liquid(new GTFluidBuilder().temperature(320)).color(color));
});

GTCEuStartupEvents.materialModification(() => {
    const MaterialStack = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack');
    GTMaterials.get('gtcsolo:lithium_carbonate').setComponents([
        new MaterialStack(GTMaterials.Lithium, 2), new MaterialStack(GTMaterials.Carbon, 1),
        new MaterialStack(GTMaterials.Oxygen, 3)
    ]);
    GTMaterials.get('gtcsolo:potassium_chloride').setComponents([
        new MaterialStack(GTMaterials.Potassium, 1), new MaterialStack(GTMaterials.Chlorine, 1)
    ]);
    GTMaterials.get('gtcsolo:rubidium_sulfate').setComponents([
        new MaterialStack(GTMaterials.Rubidium, 2), new MaterialStack(GTMaterials.Sulfur, 1),
        new MaterialStack(GTMaterials.Oxygen, 4)
    ]);
});
