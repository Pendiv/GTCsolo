// Rock compositions are expressed in the same dust units used by their separation recipes.
global.createRockProfiles = [
    {id: 'limestone', color: 0xbdb9ad, components: [['calcite', 6], ['clay', 1], ['stone', 1]]},
    {id: 'scoria', color: 0x311d12, components: [['basalt', 3], ['flint', 2], ['dark_ash', 2], ['olivine', 1]]},
    {id: 'scorchia', color: 0x303437, components: [['basalt', 4], ['flint', 2], ['dark_ash', 2]], bonus: ['sulfur', 1000]},
    {id: 'crimsite', color: 0x7b363e, components: [['quartzite', 3], ['potassium_feldspar', 2], ['biotite', 1], ['hematite', 2]]},
    {id: 'veridium', color: 0x356d5d, components: [['quartzite', 3], ['potassium_feldspar', 2], ['calcite', 2], ['malachite', 1]]},
    {id: 'asurine', color: 0x5c7995, components: [['quartzite', 4], ['potassium_feldspar', 2], ['biotite', 1], ['sphalerite', 1]]},
    {id: 'ochrum', color: 0xa78454, components: [['quartzite', 3], ['potassium_feldspar', 2], ['yellow_limonite', 2], ['clay', 1]], bonus: ['gold', 1000]}
];

GTCEuStartupEvents.registry('gtceu:material', event => {
    global.createRockProfiles.forEach(rock => {
        event.create('gtcsolo:' + rock.id).dust().color(rock.color).iconSet(GTMaterialIconSet.ROUGH)
            .flags(GTMaterialFlags.NO_SMASHING, GTMaterialFlags.DISABLE_DECOMPOSITION);
    });
});

GTCEuStartupEvents.materialModification(event => {
    const MaterialStack = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack');
    global.createRockProfiles.forEach(rock => {
        const material = GTMaterials.get('gtcsolo:' + rock.id);
        const parts = rock.components.map(part => new MaterialStack(GTMaterials.get('gtceu:' + part[0]), part[1]));
        material.setComponents(parts);
        // MaterialStack provides GregTech's existing formula formatting and '?' for unknown composition.
        material.setFormula(parts.map(part => String(part)).join(''), false);
    });
});
