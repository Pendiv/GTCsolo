ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu;
    const fluid = (id, amount) => Fluid.of(id, amount);

    // A separate hydrochloric route selects the gadolinium-bearing fraction of the existing concentrate.
    if (Item.exists('gtceu:tiny_gadolinium_dust')) {
        gt.rare_mineral_refining('gtcsolo:magnetocaloric/gadolinium_recovery')
            .itemInputs('4x gtcsolo:heavy_rare_earth_concentrate_dust', 'gtceu:calcium_dust')
            .inputFluids(fluid('gtceu:hydrochloric_acid', 2000))
            .itemOutputs('gtceu:tiny_gadolinium_dust', '2x gtcsolo:tiny_dysprosium_dust',
                '3x gtceu:calcium_chloride_dust')
            .duration(320).EUt(7680);
    } else {
        console.warn('[Magnetocaloric alloy] Restart Minecraft to register gadolinium forms.');
    }

    // Equiatomic Sc-Gd-Tb-Dy-Ho, using existing GT and GTCsolo element IDs.
    if (Fluid.exists('gtcsolo:rare_earth_magnetocaloric_alloy') && Item.exists('gtceu:gadolinium_dust')) {
        gt.precision_blast_furnace('gtcsolo:magnetocaloric/rare_earth_alloy')
            .itemInputs('gtceu:scandium_dust', 'gtceu:gadolinium_dust', 'gtceu:terbium_dust',
                'gtcsolo:dysprosium_dust', 'gtcsolo:holmium_dust')
            .inputFluids(fluid('gtceu:helium', 250))
            .outputFluids(fluid('gtcsolo:rare_earth_magnetocaloric_alloy', 720))
            .blastFurnaceTemp(2700).duration(600).EUt(7680);
    } else {
        console.warn('[Magnetocaloric alloy] Restart Minecraft to register the alloy and gadolinium forms.');
    }

    // UV pumps require only 32 CWU/t research; avoid a circular UHV-pump research dependency.
    if (Item.exists('gtcsolo:uhv_hpca_active_cooler_component') &&
        Item.exists('gtcsolo:rare_earth_magnetocaloric_alloy_plate')) {
        gt.assembler('gtcsolo:magnetocaloric/uhv_hpca_active_cooler')
            .itemInputs('gtceu:hpca_active_cooler_component', '4x gtcsolo:rare_earth_magnetocaloric_alloy_plate',
                '16x gtcsolo:rare_earth_magnetocaloric_alloy_foil', '2x gtceu:uv_electric_pump',
                '2x #gtceu:circuits/uv', '4x gtceu:osmium_plate')
            .inputFluids(fluid('gtceu:pcb_coolant', 1000))
            .itemOutputs('gtcsolo:uhv_hpca_active_cooler_component')
            .cleanroom(CleanroomType.CLEANROOM).duration(1200).EUt(491520);
    } else {
        console.warn('[Magnetocaloric alloy] Restart Minecraft to register the alloy and UHV cooling component.');
    }
});
