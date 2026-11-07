// Steel foil is needed by the LV transmitter assembly recipes; use native foil generation.
GTCEuStartupEvents.materialModification(function (event) {
    GTMaterials.get('steel').addFlags(GTMaterialFlags.GENERATE_FOIL);
});
