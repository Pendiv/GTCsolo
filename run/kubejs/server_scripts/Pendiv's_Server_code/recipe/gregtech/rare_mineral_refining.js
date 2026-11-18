ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu;
    const fluid = (id, amount) => Fluid.of(id, amount);
    const refining = name => gt.rare_mineral_refining('gtcsolo:' + name);

    gt.assembler('gtcsolo:rare_mineral_refinery')
        .itemInputs('gtceu:iv_chemical_reactor', '2x gtceu:ev_pump', '2x #gtceu:circuits/iv', '4x gtceu:clean_machine_casing')
        .inputFluids(fluid('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:rare_mineral_refinery').duration(400).EUt(7680);

    // Acid digestion transfers the rare-earth fraction into the sulfate liquor.
    refining('digest_xenotime')
        .itemInputs('8x gtcsolo:xenotime_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 12000))
        .outputFluids(fluid('gtcsolo:xenotime_sulfate_leachate', 8000), fluid('gtceu:phosphoric_acid', 8000))
        .duration(320).EUt(1920);

    // Separation and precipitation are combined at this recipe scale.
    refining('separate_xenotime_leachate')
        .inputFluids(fluid('gtcsolo:xenotime_sulfate_leachate', 8000), fluid('minecraft:water', 4000))
        .itemOutputs('4x gtcsolo:yttrium_oxide_dust', 'gtcsolo:heavy_rare_earth_concentrate_dust')
        .outputFluids(fluid('gtceu:diluted_sulfuric_acid', 8000))
        .duration(280).EUt(1920);

    // Fluorination and calcium reduction share one operation.
    refining('reduce_yttrium_oxide')
        .itemInputs('gtcsolo:yttrium_oxide_dust', '3x gtceu:calcium_dust')
        .inputFluids(fluid('gtceu:hydrofluoric_acid', 6000))
        .itemOutputs('2x gtceu:yttrium_dust', '3x gtcsolo:calcium_fluoride_dust')
        .outputFluids(fluid('minecraft:water', 3000))
        .duration(180).EUt(1920);

    refining('separate_heavy_rare_earths')
        .itemInputs('4x gtcsolo:heavy_rare_earth_concentrate_dust', 'gtceu:calcium_dust')
        .inputFluids(fluid('gtceu:hydrofluoric_acid', 2000))
        .itemOutputs('3x gtcsolo:tiny_dysprosium_dust', 'gtceu:tiny_terbium_dust', 'gtcsolo:calcium_fluoride_dust')
        .outputFluids(fluid('minecraft:water', 1000))
        .duration(320).EUt(7680);

    // Carbochlorination makes volatile zirconium tetrachloride and a trace-element residue.
    refining('chlorinate_scandium_bearing_baddeleyite')
        .itemInputs('8x gtcsolo:scandium_bearing_baddeleyite_dust', '8x gtceu:carbon_dust')
        .inputFluids(fluid('gtceu:chlorine', 16000))
        .itemOutputs('8x gtcsolo:zirconium_tetrachloride_dust', 'gtcsolo:scandium_refining_residue_dust')
        .outputFluids(fluid('gtceu:carbon_dioxide', 8000))
        .duration(360).EUt(1920);

    refining('reduce_zirconium_tetrachloride')
        .itemInputs('gtcsolo:zirconium_tetrachloride_dust', '2x gtceu:magnesium_dust')
        .itemOutputs('gtceu:zirconium_dust', '2x gtceu:magnesium_chloride_dust')
        .duration(160).EUt(1920);

    refining('recover_scandium_from_baddeleyite_residue')
        .itemInputs('2x gtcsolo:scandium_refining_residue_dust', 'gtceu:calcium_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 2000), fluid('gtceu:hydrofluoric_acid', 2000))
        .itemOutputs('gtceu:tiny_scandium_dust', 'gtcsolo:calcium_fluoride_dust')
        .outputFluids(fluid('gtceu:diluted_sulfuric_acid', 1000)).duration(200).EUt(7680);

    gt.electric_blast_furnace('gtcsolo:roast_thallium_bearing_orpiment')
        .itemInputs('8x gtcsolo:thallium_bearing_orpiment_dust')
        .inputFluids(fluid('gtceu:oxygen', 36000))
        .itemOutputs('8x gtcsolo:arsenic_trioxide_dust', 'gtcsolo:thallium_roasting_residue_dust')
        .outputFluids(fluid('gtceu:sulfur_dioxide', 24000))
        .blastFurnaceTemp(1000).duration(320).EUt(480);

    refining('recover_thallium_from_roasting_residue')
        .itemInputs('2x gtcsolo:thallium_roasting_residue_dust', 'gtceu:zinc_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 1000))
        .itemOutputs('gtceu:tiny_thallium_dust')
        .outputFluids(fluid('gtcsolo:zinc_sulfate_solution', 1000)).duration(180).EUt(1920);
});
