GTCEuStartupEvents.materialModification(() => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const material = id => GTMaterials.get(id.includes(':') ? id : 'gtceu:' + id);

    // Native ore-processing byproduct indices 0, 1, 2. Use associated minerals
    // and gangue here; trace-metal recovery stays in the refining recipes.
    const byproducts = {
        zircon: ['rutile', 'ilmenite', 'monazite'],
        xenotime: ['gtcsolo:zircon', 'ilmenite', 'monazite'],
        scandium_bearing_baddeleyite: ['magnetite', 'apatite', 'gtcsolo:zircon'],
        thallium_bearing_orpiment: ['pyrite', 'cinnabar', 'realgar'],

        rhenium_bearing_molybdenite: ['pyrite', 'chalcopyrite', 'molybdenite'],
        germanium_bearing_sphalerite: ['galena', 'pyrite', 'sphalerite'],
        indium_bearing_sphalerite: ['chalcopyrite', 'cassiterite', 'sphalerite'],

        loparite: ['ilmenite', 'apatite', 'monazite'],
        eudialyte: ['silicon_dioxide', 'gtcsolo:zircon', 'magnetite'],
        bismuthinite: ['scheelite', 'molybdenite', 'pyrite'],
        gallite: ['bornite', 'galena', 'sphalerite'],
        germanite: ['bornite', 'pyrite', 'chalcopyrite'],
        heterogenite: ['malachite', 'goethite', 'pyrolusite'],
        bertrandite: ['calcite', 'quartzite', 'topaz'],
        celestite: ['gypsum', 'calcite', 'barite'],
        carnotite: ['quartzite', 'clay', 'uraninite'],
        ulexite: ['gypsum', 'salt', 'gtcsolo:colemanite'],
        colemanite: ['calcite', 'gypsum', 'gtcsolo:ulexite'],
        cryolite: ['quartzite', 'pyrite', 'galena'],

        germanium_tin_sulfide: ['chalcopyrite', 'sphalerite', 'cassiterite'],
        lithium_titanate: ['silicon_dioxide', 'ilmenite', 'rutile'],
        hafnium_bearing_zirconium_phosphate: ['apatite', 'gtcsolo:zircon', 'monazite'],
        potassium_scandium_sulfate: ['gypsum', 'alunite', 'potassium_sulfate'],
        rubidium_feldspar: ['quartzite', 'lepidolite', 'pollucite'],
        cobalt_blue_spinel: ['bauxite', 'magnetite', 'cobaltite'],
        strontium_powellite: ['gtcsolo:celestite', 'calcite', 'powellite'],
        zinc_germanate: ['sphalerite', 'galena', 'gtcsolo:zinc_oxide'],
        calcium_ferrite: ['calcite', 'hematite', 'magnetite'],
        copper_ferrite: ['magnetite', 'hematite', 'copper']
    };

    Object.keys(byproducts).forEach(id => {
        const ore = material('gtcsolo:' + id).getProperty(PropertyKey.ORE);
        if (ore.getOreByProducts().isEmpty()) {
            ore.setOreByProducts(byproducts[id].map(material));
        }
    });
});
