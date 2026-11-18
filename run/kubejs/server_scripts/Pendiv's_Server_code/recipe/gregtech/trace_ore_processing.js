ServerEvents.recipes(event => {
    const recipes = event.recipes.gtceu;

    // MoS2 + 3.5 O2 -> MoO3 + 2 SO2. The volatile Re fraction is collected as flue dust.
    recipes.electric_blast_furnace('gtcsolo:roast_rhenium_bearing_molybdenite')
        .itemInputs('16x gtcsolo:rhenium_bearing_molybdenite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 56000))
        .itemOutputs('16x gtcsolo:molybdenum_trioxide_dust', 'gtcsolo:rhenium_roasting_residue_dust')
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 32000))
        .blastFurnaceTemp(1000).duration(400).EUt(480);

    recipes.electric_blast_furnace('gtcsolo:reduce_molybdenum_trioxide')
        .itemInputs('gtcsolo:molybdenum_trioxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 3000))
        .itemOutputs('gtceu:molybdenum_dust')
        .outputFluids(Fluid.of('minecraft:water', 3000))
        .blastFurnaceTemp(1200).duration(120).EUt(480);

    recipes.chemical_reactor('gtcsolo:crystallize_ammonium_perrhenate')
        .itemInputs('gtcsolo:rhenium_roasting_residue_dust')
        .inputFluids(Fluid.of('minecraft:water', 1000), Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('gtcsolo:tiny_ammonium_perrhenate_dust')
        .outputFluids(Fluid.of('gtcsolo:ammoniacal_condensate', 1000)).duration(200).EUt(480);

    recipes.electric_blast_furnace('gtcsolo:reduce_ammonium_perrhenate')
        .itemInputs('gtcsolo:tiny_ammonium_perrhenate_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 1000))
        .itemOutputs('gtceu:tiny_rhenium_dust')
        .outputFluids(Fluid.of('gtcsolo:ammoniacal_condensate', 250))
        .blastFurnaceTemp(1500).duration(200).EUt(480);

    // The roasting and recovery steps abstract industrial zinc leaching and residue treatment.
    ['germanium', 'indium'].forEach(element => {
        const residue = `gtcsolo:${element}_bearing_zinc_residue_dust`;
        recipes.electric_blast_furnace(`gtcsolo:roast_${element}_bearing_sphalerite`)
            .itemInputs(`16x gtcsolo:${element}_bearing_sphalerite_dust`)
            .inputFluids(Fluid.of('gtceu:oxygen', 24000))
            .itemOutputs('16x gtcsolo:zinc_oxide_dust', residue)
            .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 16000))
            .blastFurnaceTemp(1200).duration(320).EUt(120);

        recipes.chemical_reactor(`gtcsolo:leach_${element}_from_zinc_residue`)
            .itemInputs(residue)
            .inputFluids(Fluid.of('gtceu:sulfuric_acid', 1000))
            .outputFluids(Fluid.of(`gtcsolo:${element}_bearing_leachate`, 1000)).duration(200).EUt(480);
    });

    // Purification and precipitation of GeO2 are combined in this recipe.
    recipes.chemical_reactor('gtcsolo:recover_germanium_dioxide')
        .inputFluids(Fluid.of('gtcsolo:germanium_bearing_leachate', 1000), Fluid.of('minecraft:water', 1000))
        .itemOutputs('gtcsolo:tiny_germanium_dioxide_dust')
        .outputFluids(Fluid.of('gtceu:diluted_sulfuric_acid', 1000)).duration(160).EUt(480);

    recipes.electric_blast_furnace('gtcsolo:reduce_germanium_dioxide')
        .itemInputs('gtcsolo:tiny_germanium_dioxide_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 1000))
        .itemOutputs('gtceu:tiny_germanium_dust')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .blastFurnaceTemp(1100).duration(160).EUt(480);

    recipes.chemical_reactor('gtcsolo:cement_indium')
        .itemInputs('gtceu:zinc_dust')
        .inputFluids(Fluid.of('gtcsolo:indium_bearing_leachate', 1000))
        .itemOutputs('gtceu:tiny_indium_dust')
        .outputFluids(Fluid.of('gtcsolo:zinc_sulfate_solution', 1000)).duration(160).EUt(480);

    // ZnO + C -> Zn + CO. Condensation of zinc vapor is included in the operation.
    recipes.electric_blast_furnace('gtcsolo:reduce_zinc_oxide')
        .itemInputs('gtcsolo:zinc_oxide_dust', 'gtceu:carbon_dust')
        .itemOutputs('gtceu:zinc_dust')
        .outputFluids(Fluid.of('gtceu:carbon_monoxide', 1000))
        .blastFurnaceTemp(1400).duration(100).EUt(120);
});
