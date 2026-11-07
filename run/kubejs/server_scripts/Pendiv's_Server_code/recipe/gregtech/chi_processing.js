// Chi processing with redissolution and residue recovery. Fluids are mB; time is ticks before native OC.
ServerEvents.recipes(event => {
    // A newly installed startup material cannot be registered by a server /reload.
    if (!Fluid.exists('gtcsolo:chi_recrystallization_solution') || !Item.exists('gtcsolo:crude_chi_dust')) {
        console.warn('[Chi processing] Restart Minecraft to register the new materials before loading these recipes.');
        return;
    }
    const ZPM = 122880;
    const UV = 491520;
    const UHV = 1966080;
    const fluid = (name, amount) => Fluid.of('gtcsolo:' + name, amount);

    // Native gem materials also receive implosion recipes without CRYSTALLIZABLE.
    // Keep regular gem/block/plate/lens working and recycling, but require this refining line.
    ['powderbarrel', 'tnt', 'dynamite', 'itnt'].forEach(explosive => {
        event.remove({id: 'gtceu:implosion_compressor/implode_dust_purified_chi_' + explosive});
    });

    event.recipes.gtceu.electric_blast_furnace('gtcsolo:chi/condensation')
        .inputFluids(fluid('chi_molten_magma', 144))
        .itemOutputs('gtcsolo:chi_mineral_residue')
        .outputFluids(fluid('chi_condensate', 250))
        .blastFurnaceTemp(10800).duration(400).EUt(UV);

    event.recipes.gtceu.electric_blast_furnace('gtcsolo:chi/sculk_recovery')
        .itemInputs('64x minecraft:sculk')
        .inputFluids(Fluid.of('gtceu:nitrogen', 1000))
        .itemOutputs('4x gtceu:ash_dust')
        .outputFluids(fluid('chi_condensate', 16))
        .blastFurnaceTemp(7200).duration(400).EUt(ZPM);

    event.recipes.gtceu.mixer('gtcsolo:chi/adsorbent_precursor')
        .itemInputs('8x gtceu:silicon_dioxide_dust', '4x gtceu:activated_carbon_dust')
        .inputFluids(fluid('stone_slurry', 2000))
        .itemOutputs('8x gtcsolo:chi_adsorbent_precursor')
        .duration(200).EUt(ZPM);

    // Recovered solids replace part of the mineral filler; carbon and slurry are still required.
    event.recipes.gtceu.mixer('gtcsolo:chi/adsorbent_precursor_from_residue')
        .itemInputs('4x gtcsolo:chi_process_residue', '4x gtceu:silicon_dioxide_dust',
            '4x gtceu:activated_carbon_dust')
        .inputFluids(fluid('stone_slurry', 2000))
        .itemOutputs('8x gtcsolo:chi_adsorbent_precursor')
        .duration(300).EUt(ZPM);

    event.recipes.gtceu.electric_blast_furnace('gtcsolo:chi/adsorbent_firing')
        .itemInputs('8x gtcsolo:chi_adsorbent_precursor')
        .inputFluids(Fluid.of('gtceu:nitrogen', 1000))
        .itemOutputs('8x gtcsolo:chi_porous_adsorbent')
        .blastFurnaceTemp(3600).duration(600).EUt(UV);

    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/extraction_medium')
        .inputFluids(Fluid.of('gtceu:sulfuric_acid', 1000), Fluid.of('gtceu:methanol', 1000))
        .outputFluids(fluid('chi_extraction_medium', 2000))
        .duration(200).EUt(UV);

    event.recipes.gtceu.mixer('gtcsolo:chi/pvc_coating_solution')
        .itemInputs('4x gtceu:polyvinyl_chloride_dust')
        .inputFluids(Fluid.of('gtceu:acetone', 1000))
        .outputFluids(fluid('chi_pvc_coating_solution', 1000))
        .duration(200).EUt(ZPM);

    // Apply the polymer after firing. Regeneration returns bare carriers, not coated ones.
    event.recipes.gtceu.chemical_bath('gtcsolo:chi/adsorbent_coating')
        .itemInputs('8x gtcsolo:chi_porous_adsorbent')
        .inputFluids(fluid('chi_pvc_coating_solution', 1000))
        .itemOutputs('8x gtcsolo:chi_pvc_coated_adsorbent')
        .outputFluids(fluid('chi_spent_coating_solution', 1000))
        .duration(1800).EUt(UV);

    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/growth_promoter')
        .inputFluids(fluid('chi_condensate', 1000), Fluid.of('gtceu:acetic_anhydride', 1000))
        .outputFluids(fluid('chi_growth_promoter', 1000), Fluid.of('gtceu:acetic_acid', 1000))
        .duration(1800).EUt(UHV);

    // Three item inputs and five fluids: the native large-reactor-only type.
    event.recipes.gtceu.large_chemical_reactor('gtcsolo:chi/selective_adsorption')
        .itemInputs('8x gtcsolo:chi_pvc_coated_adsorbent',
            '2x gtceu:polytetrafluoroethylene_dust', '2x gtceu:sodium_hydroxide_dust')
        .inputFluids(fluid('chi_condensate', 4000), fluid('chi_extraction_medium', 1000),
            Fluid.of('gtceu:hydrochloric_acid', 500), Fluid.of('gtceu:methanol', 1000),
            Fluid.of('gtceu:distilled_water', 1500))
        .outputFluids(fluid('chi_adsorption_slurry', 8000))
        .duration(1200).EUt(UHV);

    event.recipes.gtceu.centrifuge('gtcsolo:chi/adsorbent_separation')
        .inputFluids(fluid('chi_adsorption_slurry', 8000))
        .itemOutputs('8x gtcsolo:chi_loaded_adsorbent')
        .outputFluids(fluid('chi_acidic_filtrate', 8000))
        .duration(160).EUt(UV);

    event.recipes.gtceu.chemical_bath('gtcsolo:chi/adsorbent_washing')
        .itemInputs('8x gtcsolo:chi_loaded_adsorbent')
        .inputFluids(Fluid.of('gtceu:distilled_water', 2000))
        .itemOutputs('8x gtcsolo:chi_washed_adsorbent')
        .outputFluids(fluid('chi_wash_effluent', 2000))
        .duration(200).EUt(UV);

    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/elution')
        .itemInputs('8x gtcsolo:chi_washed_adsorbent')
        .inputFluids(Fluid.of('gtceu:diluted_hydrochloric_acid', 1000), fluid('chi_extraction_medium', 250))
        .itemOutputs('8x gtcsolo:chi_spent_adsorbent')
        .outputFluids(fluid('chi_concentrate', 1000), fluid('chi_wash_effluent', 250))
        .duration(400).EUt(UHV);

    // Split concentrate 750:250 between crude crystals and their growth solution.
    event.recipes.gtceu.autoclave('gtcsolo:chi/crude_crystallization')
        .inputFluids(fluid('chi_concentrate', 750))
        .itemOutputs('4x gtcsolo:crude_chi_crystal')
        .outputFluids(fluid('chi_crystallization_mother_liquor', 500))
        .duration(900).EUt(UV);

    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/growth_solution')
        .inputFluids(fluid('chi_concentrate', 250), fluid('chi_growth_promoter', 1000),
            Fluid.of('gtceu:distilled_water', 2750))
        .outputFluids(fluid('chi_growth_solution', 4000))
        .duration(200).EUt(UHV);

    event.recipes.gtceu.macerator('gtcsolo:chi/crude_crystal_grinding')
        .itemInputs('gtcsolo:crude_chi_crystal')
        .itemOutputs('gtcsolo:crude_chi_dust')
        .duration(200).EUt(UV);

    // Reserve some acidic filtrate before sending the rest to reagent recovery.
    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/crude_redissolution')
        .itemInputs('4x gtcsolo:crude_chi_dust')
        .inputFluids(fluid('chi_acidic_filtrate', 1000), fluid('chi_extraction_medium', 500),
            Fluid.of('gtceu:diluted_hydrochloric_acid', 500))
        .outputFluids(fluid('chi_redissolved_solution', 2000))
        .duration(600).EUt(UHV);

    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/recrystallization_solution')
        .inputFluids(fluid('chi_redissolved_solution', 2000), fluid('chi_growth_solution', 4000))
        .outputFluids(fluid('chi_impure_recrystallization_solution', 6000))
        .duration(400).EUt(UHV);

    // Purify after adding the growth solution so neither chi feed bypasses this separation.
    // Bare carriers remove residual inclusions; coated carriers remain specific to initial chi adsorption.
    event.recipes.gtceu.centrifuge('gtcsolo:chi/solution_purification')
        .itemInputs('gtcsolo:chi_porous_adsorbent')
        .inputFluids(fluid('chi_impure_recrystallization_solution', 6000))
        .itemOutputs('gtcsolo:chi_spent_adsorbent', 'gtcsolo:chi_process_residue')
        .outputFluids(fluid('chi_recrystallization_solution', 6000))
        .duration(400).EUt(UV);

    // The native autoclave has one fluid input: prepare and purify the combined feed beforehand.
    event.recipes.gtceu.autoclave('gtcsolo:chi/purified_crystallization')
        .inputFluids(fluid('chi_recrystallization_solution', 1500))
        .itemOutputs('gtcsolo:purified_chi_gem')
        .outputFluids(fluid('chi_spent_growth_solution', 1500))
        .duration(2400).EUt(UHV);

    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/mother_liquor_recovery')
        .itemInputs('gtceu:activated_carbon_dust')
        .inputFluids(fluid('chi_crystallization_mother_liquor', 500), Fluid.of('gtceu:hydrochloric_acid', 500))
        .outputFluids(fluid('chi_recovery_solution', 1000))
        .duration(200).EUt(UV);

    event.recipes.gtceu.distillation_tower('gtcsolo:chi/condensate_recovery')
        .inputFluids(fluid('chi_recovery_solution', 1000))
        .outputFluids(fluid('chi_condensate', 1000))
        .disableDistilleryRecipes(true).duration(400).EUt(UV);

    event.recipes.gtceu.distillation_tower('gtcsolo:chi/reagent_recovery')
        .inputFluids(fluid('chi_acidic_filtrate', 8000))
        .itemOutputs('gtcsolo:chi_process_residue')
        .outputFluids(fluid('chi_extraction_medium', 750), Fluid.of('gtceu:hydrochloric_acid', 400),
            Fluid.of('gtceu:methanol', 800), Fluid.of('gtceu:distilled_water', 6050))
        .disableDistilleryRecipes(true).duration(400).EUt(UV);

    event.recipes.gtceu.chemical_bath('gtcsolo:chi/adsorbent_regeneration')
        .itemInputs('8x gtcsolo:chi_spent_adsorbent')
        .inputFluids(Fluid.of('gtceu:distilled_water', 1000))
        .itemOutputs('6x gtcsolo:chi_porous_adsorbent', '2x gtceu:carbon_dust')
        .outputFluids(fluid('chi_wash_effluent', 1000))
        .duration(300).EUt(UV);

    event.recipes.gtceu.distillation_tower('gtcsolo:chi/growth_water_recovery')
        .inputFluids(fluid('chi_spent_growth_solution', 1000))
        .itemOutputs('gtcsolo:chi_process_residue')
        .outputFluids(Fluid.of('gtceu:distilled_water', 900))
        .disableDistilleryRecipes(true).duration(200).EUt(ZPM);

    event.recipes.gtceu.distillation_tower('gtcsolo:chi/wash_water_recovery')
        .inputFluids(fluid('chi_wash_effluent', 1000))
        .itemOutputs('gtcsolo:chi_process_residue')
        .outputFluids(Fluid.of('gtceu:distilled_water', 900))
        .disableDistilleryRecipes(true).duration(200).EUt(ZPM);

    event.recipes.gtceu.chemical_reactor('gtcsolo:chi/cutting_waste_recovery')
        .itemInputs('gtcsolo:purified_chi_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .outputFluids(fluid('chi_recovery_solution', 1000))
        .duration(400).EUt(UV);

    event.recipes.gtceu.distillation_tower('gtcsolo:chi/coating_solvent_recovery')
        .inputFluids(fluid('chi_spent_coating_solution', 1000))
        .outputFluids(Fluid.of('gtceu:acetone', 750), fluid('chi_wash_effluent', 250))
        .disableDistilleryRecipes(true).duration(400).EUt(ZPM);

    event.recipes.gtceu.chemical_bath('gtcsolo:chi/mineral_residue_recovery')
        .itemInputs('4x gtcsolo:chi_mineral_residue')
        .inputFluids(Fluid.of('gtceu:diluted_hydrochloric_acid', 1000))
        .itemOutputs('2x gtceu:silicon_dioxide_dust', '2x gtceu:stone_dust')
        .outputFluids(fluid('chi_wash_effluent', 1000))
        .duration(200).EUt(UV);
});
