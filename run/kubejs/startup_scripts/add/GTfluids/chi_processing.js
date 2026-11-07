// Chi processing. Native material forms; no new machine or pure-chi fluid.
GTCEuStartupEvents.registry('gtceu:material', event => {
    [
        ['chi_condensate', 0x438C83],
        ['chi_adsorption_slurry', 0x364D47],
        ['chi_extraction_medium', 0xC3AE65],
        ['chi_acidic_filtrate', 0x777C50],
        ['chi_wash_effluent', 0x869B8A],
        ['chi_concentrate', 0x237A70],
        ['chi_crystallization_mother_liquor', 0x518D81],
        ['chi_growth_solution', 0x70BFA7],
        ['chi_spent_growth_solution', 0xA2B5AB],
        ['chi_recovery_solution', 0x567D6A],
        ['chi_pvc_coating_solution', 0xD3D0B5],
        ['chi_spent_coating_solution', 0x8D8C76],
        ['chi_growth_promoter', 0x67A58A],
        ['chi_redissolved_solution', 0x5B8972],
        ['chi_impure_recrystallization_solution', 0x669A82],
        ['chi_recrystallization_solution', 0x65BFA6]
    ].forEach(entry => {
        event.create('gtcsolo:' + entry[0])
            .fluid()
            .color(entry[1])
            .iconSet(GTMaterialIconSet.FLUID)
            .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
    });

    event.create('gtcsolo:purified_chi')
        .gem().dust()
        .color(0x4DCBB7)
        .iconSet(GTMaterialIconSet.EMERALD)
        .formula('χ')
        // GENERATE_LENS also enables GENERATE_PLATE. Water crystallization is not enabled.
        .flags(GTMaterialFlags.GENERATE_LENS, GTMaterialFlags.DISABLE_DECOMPOSITION);

    // A separate dust material keeps crude powder out of purified-chi unification.
    event.create('gtcsolo:crude_chi')
        .dust()
        .color(0x4F8E75)
        .iconSet(GTMaterialIconSet.ROUGH)
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);
});

// Process solids are distinct items: they must not unify with purified-chi dust or gems.
// Reuse the installed GregTech textures, with KubeJS's constant tint; no copied artwork.
StartupEvents.registry('item', event => {
    [
        ['chi_mineral_residue', 'rough/dust_impure', 0x756653],
        ['chi_adsorbent_precursor', 'rough/dust', 0xA79D83],
        ['chi_porous_adsorbent', 'rough/dust_pure', 0xC9C1A5],
        ['chi_pvc_coated_adsorbent', 'rough/dust_pure', 0xD7D2BB],
        ['chi_loaded_adsorbent', 'rough/dust_impure', 0x417D6E],
        ['chi_washed_adsorbent', 'rough/dust_pure', 0x69AA91],
        ['chi_spent_adsorbent', 'rough/dust_impure', 0x7B7A61],
        ['crude_chi_crystal', 'diamond/gem_chipped', 0x3B9981],
        ['chi_process_residue', 'rough/dust_impure', 0x77716C]
    ].forEach(entry => {
        event.create('gtcsolo:' + entry[0])
            .texture('gtceu:item/material_sets/' + entry[1])
            .color(0, entry[2]);
    });
});
