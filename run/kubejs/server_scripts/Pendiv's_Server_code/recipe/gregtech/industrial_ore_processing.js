ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu;
    const fluid = (id, amount) => Fluid.of(id, amount);
    const refining = name => gt.rare_mineral_refining('gtcsolo:' + name);

    // Volatile chlorides are separated from the rare-earth fraction during chlorination.
    refining('chlorinate_loparite')
        .itemInputs('16x gtcsolo:loparite_dust', '8x gtceu:carbon_dust')
        .inputFluids(fluid('gtceu:chlorine', 24000))
        .itemOutputs('4x gtceu:rare_earth_dust', '2x gtcsolo:niobium_pentachloride_dust', 'gtcsolo:tantalum_pentachloride_dust')
        .outputFluids(fluid('gtceu:titanium_tetrachloride', 4000))
        .duration(480).EUt(7680);

    ['niobium', 'tantalum'].forEach(metal => {
        refining(`reduce_${metal}_pentachloride`)
            .itemInputs(`2x gtcsolo:${metal}_pentachloride_dust`, '5x gtceu:magnesium_dust')
            .itemOutputs(`2x gtceu:${metal}_dust`, '5x gtceu:magnesium_chloride_dust')
            .duration(240).EUt(7680);
    });

    refining('leach_eudialyte')
        .itemInputs('16x gtcsolo:eudialyte_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 16000))
        .itemOutputs('4x gtcsolo:zirconium_hafnium_concentrate_dust', 'gtcsolo:heavy_rare_earth_concentrate_dust', '4x gtceu:silicon_dioxide_dust')
        .outputFluids(fluid('gtceu:diluted_sulfuric_acid', 8000))
        .duration(480).EUt(7680);

    // Fractionation and recovery of the minor Hf fraction share this operation.
    refining('separate_zirconium_hafnium_concentrate')
        .itemInputs('4x gtcsolo:zirconium_hafnium_concentrate_dust', 'gtceu:magnesium_dust')
        .inputFluids(fluid('gtceu:chlorine', 8000))
        .itemOutputs('4x gtcsolo:zirconium_tetrachloride_dust', 'gtceu:tiny_hafnium_dust', 'gtceu:magnesium_chloride_dust')
        .duration(300).EUt(7680);

    // Oxidation and concentrate separation are represented as one refining step.
    refining('separate_germanite')
        .itemInputs('8x gtcsolo:germanite_dust')
        .inputFluids(fluid('gtceu:oxygen', 8000))
        .itemOutputs('6x gtceu:copper_dust', 'gtcsolo:tiny_germanium_dioxide_dust')
        .outputFluids(fluid('gtceu:sulfur_dioxide', 4000))
        .duration(280).EUt(1920);

    // Reductive acid leaching followed by cobalt electrowinning.
    gt.chemical_reactor('gtcsolo:leach_heterogenite')
        .itemInputs('4x gtcsolo:heterogenite_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 2000), fluid('gtceu:sulfur_dioxide', 2000))
        .outputFluids(fluid('gtcsolo:cobalt_sulfate_leachate', 4000))
        .duration(200).EUt(480);
    gt.electrolyzer('gtcsolo:electrowin_cobalt')
        .inputFluids(fluid('gtcsolo:cobalt_sulfate_leachate', 4000))
        .itemOutputs('4x gtceu:cobalt_dust')
        .outputFluids(fluid('gtceu:sulfuric_acid', 4000))
        .duration(240).EUt(480);

    // Leaching and hydroxide precipitation are combined. The metal step models a fluoride route.
    refining('leach_bertrandite')
        .itemInputs('4x gtcsolo:bertrandite_dust', '2x gtceu:sodium_hydroxide_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 4000))
        .itemOutputs('4x gtcsolo:beryllium_hydroxide_dust', '2x gtceu:silicon_dioxide_dust', 'gtcsolo:sodium_sulfate_dust')
        .outputFluids(fluid('gtceu:diluted_sulfuric_acid', 1000))
        .duration(260).EUt(1920);
    refining('reduce_beryllium_feedstock')
        .itemInputs('gtcsolo:beryllium_hydroxide_dust', 'gtceu:magnesium_dust')
        .inputFluids(fluid('gtceu:hydrofluoric_acid', 2000))
        .itemOutputs('gtceu:beryllium_dust', 'gtcsolo:magnesium_fluoride_dust')
        .outputFluids(fluid('minecraft:water', 2000)).duration(180).EUt(1920);

    // Black ash process: sulfate is roasted to sulfide before carbonation.
    gt.electric_blast_furnace('gtcsolo:roast_celestite')
        .itemInputs('gtcsolo:celestite_dust', '2x gtceu:carbon_dust')
        .itemOutputs('gtcsolo:strontium_sulfide_dust')
        .outputFluids(fluid('gtceu:carbon_dioxide', 2000))
        .blastFurnaceTemp(1300).duration(180).EUt(480);
    gt.chemical_reactor('gtcsolo:carbonate_strontium_sulfide')
        .itemInputs('gtcsolo:strontium_sulfide_dust')
        .inputFluids(fluid('gtceu:carbon_dioxide', 1000), fluid('minecraft:water', 1000))
        .itemOutputs('gtcsolo:strontium_carbonate_dust')
        .outputFluids(fluid('gtceu:hydrogen_sulfide', 1000))
        .duration(180).EUt(480);

    refining('leach_carnotite')
        .itemInputs('8x gtcsolo:carnotite_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 8000))
        .outputFluids(fluid('gtcsolo:carnotite_leachate', 8000)).duration(320).EUt(1920);
    refining('separate_carnotite_leachate')
        .inputFluids(fluid('gtcsolo:carnotite_leachate', 8000), fluid('gtceu:ammonia', 2000))
        .itemOutputs('2x gtceu:uranium_dust', '2x gtceu:vanadium_dust', 'gtcsolo:ammonium_sulfate_dust')
        .outputFluids(fluid('gtceu:diluted_sulfuric_acid', 2000)).duration(320).EUt(7680);

    ['ulexite', 'colemanite'].forEach(ore => {
        gt.chemical_reactor(`gtcsolo:boric_acid_from_${ore}`)
            .itemInputs(`2x gtcsolo:${ore}_dust`)
            .inputFluids(fluid('gtceu:sulfuric_acid', 2000), fluid('minecraft:water', 2000))
            .itemOutputs('4x gtcsolo:boric_acid_dust', 'gtceu:gypsum_dust').duration(200).EUt(480);
    });
    gt.chemical_reactor('gtcsolo:crystallize_borax')
        .itemInputs('4x gtcsolo:boric_acid_dust', '2x gtceu:sodium_hydroxide_dust')
        .inputFluids(fluid('minecraft:water', 1000))
        .itemOutputs('gtceu:borax_dust').duration(160).EUt(480);

    gt.electrolyzer('gtcsolo:electrolyze_sodium_sulfate')
        .itemInputs('gtcsolo:sodium_sulfate_dust')
        .inputFluids(fluid('minecraft:water', 2000))
        .itemOutputs('2x gtceu:sodium_hydroxide_dust')
        .outputFluids(fluid('gtceu:sulfuric_acid', 1000))
        .duration(160).EUt(120);

    gt.large_chemical_reactor('gtcsolo:digest_bauxite_for_alumina')
        .itemInputs('8x gtceu:bauxite_dust', '4x gtceu:sodium_hydroxide_dust')
        .inputFluids(fluid('minecraft:water', 4000))
        .itemOutputs('4x gtcsolo:alumina_dust').duration(320).EUt(480);

    gt.electric_blast_furnace('gtcsolo:reduce_alumina_with_carbon')
        .itemInputs('gtcsolo:alumina_dust', '3x gtceu:carbon_dust')
        .itemOutputs('2x gtceu:aluminium_ingot')
        .outputFluids(fluid('gtceu:carbon_monoxide', 3000))
        .blastFurnaceTemp(1800).duration(160).EUt(480);

    // The reusable cryolite bath is paired with an oxygen-evolving anode.
    gt.electrolyzer('gtcsolo:electrolyze_alumina_in_cryolite')
        .itemInputs('2x gtcsolo:alumina_dust')
        .notConsumable('gtcsolo:cryolite_dust')
        .itemOutputs('4x gtceu:aluminium_ingot')
        .outputFluids(fluid('gtceu:oxygen', 3000))
        .duration(60).EUt(480);

    gt.electric_blast_furnace('gtcsolo:roast_cryolite_with_sulfuric_acid')
        .itemInputs('gtcsolo:cryolite_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 3000))
        .itemOutputs('gtcsolo:sodium_sulfate_dust', 'gtcsolo:sodium_aluminium_sulfate_dust')
        .outputFluids(fluid('gtceu:hydrofluoric_acid', 6000))
        .blastFurnaceTemp(1000).duration(320).EUt(480);

    gt.chemical_reactor('gtcsolo:precipitate_aluminium_hydroxide_from_sulfate')
        .itemInputs('gtcsolo:sodium_aluminium_sulfate_dust', '3x gtceu:sodium_hydroxide_dust')
        .inputFluids(fluid('minecraft:water', 1000))
        .itemOutputs('gtcsolo:aluminium_hydroxide_dust', '2x gtcsolo:sodium_sulfate_dust')
        .duration(160).EUt(480);

    gt.electric_blast_furnace('gtcsolo:calcine_aluminium_hydroxide')
        .itemInputs('2x gtcsolo:aluminium_hydroxide_dust')
        .itemOutputs('gtcsolo:alumina_dust')
        .outputFluids(fluid('minecraft:water', 3000))
        .blastFurnaceTemp(1300).duration(120).EUt(120);

    gt.mixer('gtcsolo:mix_alumina_abrasive')
        .itemInputs('3x gtcsolo:alumina_dust', 'gtcsolo:cryolite_dust')
        .itemOutputs('4x gtcsolo:alumina_abrasive_dust')
        .duration(100).EUt(120);

    gt.assembler('gtcsolo:assemble_diamond_grinding_head_with_abrasive')
        .itemInputs('8x gtceu:steel_plate', '5x gtceu:diamond_dust', 'gtcsolo:alumina_abrasive_dust')
        .itemOutputs('gtceu:diamond_grinding_head')
        .duration(160).EUt(480);
});
