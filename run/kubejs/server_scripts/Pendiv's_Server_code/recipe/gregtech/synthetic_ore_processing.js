ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu;
    const fluid = (id, amount) => Fluid.of(id, amount);
    const refining = name => gt.rare_mineral_refining('gtcsolo:' + name);

    gt.electric_blast_furnace('gtcsolo:roast_germanium_tin_sulfide')
        .itemInputs('8x gtcsolo:germanium_tin_sulfide_dust')
        .inputFluids(fluid('gtceu:oxygen', 26000))
        .itemOutputs('8x gtcsolo:roasted_germanium_tin_ore_dust')
        .outputFluids(fluid('gtceu:sulfur_dioxide', 16000))
        .blastFurnaceTemp(1150).duration(320).EUt(480);

    refining('leach_roasted_germanium_tin_ore')
        .itemInputs('8x gtcsolo:roasted_germanium_tin_ore_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 8000))
        .itemOutputs('4x gtcsolo:tin_germanium_oxide_concentrate_dust')
        .outputFluids(fluid('gtceu:sulfuric_copper_solution', 4000), fluid('gtcsolo:zinc_sulfate_solution', 2000))
        .duration(320).EUt(1920);

    // Separation and reduction of the tin-rich fraction share one operation.
    refining('separate_tin_germanium_oxide_concentrate')
        .itemInputs('4x gtcsolo:tin_germanium_oxide_concentrate_dust', '2x gtceu:carbon_dust')
        .inputFluids(fluid('gtceu:hydrochloric_acid', 2000))
        .itemOutputs('2x gtceu:tin_dust', 'gtcsolo:tiny_germanium_dioxide_dust')
        .outputFluids(fluid('gtceu:carbon_dioxide', 2000), fluid('gtceu:diluted_hydrochloric_acid', 1000))
        .duration(240).EUt(1920);

    refining('leach_lithium_titanate')
        .itemInputs('4x gtcsolo:lithium_titanate_dust')
        .inputFluids(fluid('gtceu:hydrochloric_acid', 8000))
        .outputFluids(fluid('gtcsolo:lithium_titanate_leachate', 8000))
        .duration(320).EUt(1920);

    // Precipitation uses carbonate, followed by recovery of the titanium fraction as rutile.
    refining('separate_lithium_titanate_leachate')
        .itemInputs('2x gtceu:potassium_carbonate_dust')
        .inputFluids(fluid('gtcsolo:lithium_titanate_leachate', 8000), fluid('minecraft:water', 2000))
        .itemOutputs('2x gtcsolo:lithium_carbonate_dust', '5x gtceu:rutile_dust', '4x gtcsolo:potassium_chloride_dust')
        .outputFluids(fluid('gtceu:diluted_hydrochloric_acid', 2000))
        .duration(280).EUt(1920);

    refining('digest_hafnium_bearing_zirconium_phosphate')
        .itemInputs('4x gtcsolo:hafnium_bearing_zirconium_phosphate_dust', '4x gtceu:sodium_hydroxide_dust')
        .inputFluids(fluid('minecraft:water', 4000))
        .itemOutputs('4x gtcsolo:zirconium_hafnium_concentrate_dust')
        .outputFluids(fluid('gtcsolo:sodium_phosphate_liquor', 2000))
        .duration(320).EUt(7680);

    gt.chemical_reactor('gtcsolo:acidify_sodium_phosphate_liquor')
        .inputFluids(fluid('gtcsolo:sodium_phosphate_liquor', 2000), fluid('gtceu:sulfuric_acid', 3000))
        .itemOutputs('3x gtcsolo:sodium_sulfate_dust')
        .outputFluids(fluid('gtceu:phosphoric_acid', 2000))
        .duration(200).EUt(480);

    gt.chemical_reactor('gtcsolo:dissolve_potassium_scandium_sulfate')
        .itemInputs('4x gtcsolo:potassium_scandium_sulfate_dust')
        .inputFluids(fluid('minecraft:water', 4000))
        .outputFluids(fluid('gtcsolo:scandium_sulfate_liquor', 4000))
        .duration(200).EUt(480);

    // Hydroxide precipitation and calcination are represented as one refining operation.
    refining('precipitate_scandium_from_sulfate_liquor')
        .inputFluids(fluid('gtcsolo:scandium_sulfate_liquor', 4000), fluid('gtceu:ammonia', 6000))
        .itemOutputs('gtcsolo:scandium_oxide_dust', 'gtceu:potassium_sulfate_dust', '3x gtcsolo:ammonium_sulfate_dust')
        .duration(320).EUt(7680);

    refining('reduce_scandium_oxide')
        .itemInputs('gtcsolo:scandium_oxide_dust', '3x gtceu:calcium_dust')
        .inputFluids(fluid('gtceu:hydrofluoric_acid', 6000))
        .itemOutputs('2x gtceu:scandium_dust', '3x gtcsolo:calcium_fluoride_dust')
        .outputFluids(fluid('minecraft:water', 3000))
        .duration(240).EUt(7680);

    // Sulfation and water leaching are represented as one operation.
    refining('leach_rubidium_feldspar')
        .itemInputs('8x gtcsolo:rubidium_feldspar_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 4000), fluid('minecraft:water', 4000))
        .itemOutputs('4x gtceu:silicon_dioxide_dust', '2x gtcsolo:alumina_dust')
        .outputFluids(fluid('gtcsolo:rubidium_sulfate_liquor', 4000))
        .duration(360).EUt(1920);
    gt.distillation_tower('gtcsolo:concentrate_rubidium_sulfate')
        .inputFluids(fluid('gtcsolo:rubidium_sulfate_liquor', 4000))
        .itemOutputs('gtcsolo:rubidium_sulfate_dust')
        .outputFluids(fluid('gtceu:distilled_water', 2000), fluid('gtceu:diluted_sulfuric_acid', 1000))
        .duration(220).EUt(1920);

    refining('leach_cobalt_blue_spinel')
        .itemInputs('4x gtcsolo:cobalt_blue_spinel_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 4000))
        .itemOutputs('4x gtcsolo:alumina_dust')
        .outputFluids(fluid('gtcsolo:cobalt_sulfate_leachate', 4000))
        .duration(320).EUt(1920);
    event.shapeless('minecraft:blue_dye', ['gtcsolo:cobalt_blue_spinel_dust'])
        .id('gtcsolo:blue_dye_from_cobalt_spinel');

    refining('carbonate_strontium_powellite')
        .itemInputs('4x gtcsolo:strontium_powellite_dust', '4x gtceu:potassium_carbonate_dust')
        .inputFluids(fluid('minecraft:water', 4000))
        .itemOutputs('gtcsolo:strontium_carbonate_dust', '3x gtceu:calcite_dust')
        .outputFluids(fluid('gtcsolo:potassium_molybdate_liquor', 4000))
        .duration(300).EUt(1920);
    gt.chemical_reactor('gtcsolo:acidify_potassium_molybdate')
        .inputFluids(fluid('gtcsolo:potassium_molybdate_liquor', 4000), fluid('gtceu:sulfuric_acid', 4000))
        .itemOutputs('4x gtcsolo:molybdenum_trioxide_dust', '4x gtceu:potassium_sulfate_dust')
        .outputFluids(fluid('minecraft:water', 4000))
        .duration(240).EUt(480);

    refining('leach_zinc_germanate')
        .itemInputs('4x gtcsolo:zinc_germanate_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 5000))
        .outputFluids(fluid('gtcsolo:zinc_sulfate_solution', 4000), fluid('gtcsolo:germanium_bearing_leachate', 1000))
        .duration(280).EUt(1920);
    gt.assembler('gtcsolo:zinc_germanate_phosphor_lamp')
        .itemInputs('gtcsolo:zinc_germanate_dust', 'minecraft:glass', 'minecraft:redstone', 'gtceu:iron_plate')
        .itemOutputs('gtceu:white_lamp').duration(100).EUt(30);

    // CaFe2O4 + 3 H2 -> 2 Fe + CaO + 3 H2O.
    gt.electric_blast_furnace('gtcsolo:hydrogen_reduce_calcium_ferrite')
        .itemInputs('4x gtcsolo:calcium_ferrite_dust')
        .inputFluids(fluid('gtceu:hydrogen', 6000))
        .itemOutputs('4x gtceu:iron_dust', '2x gtceu:quicklime_dust')
        .outputFluids(fluid('minecraft:water', 6000))
        .blastFurnaceTemp(1100).duration(160).EUt(120);

    // Selective leaching transfers copper into the sulfate solution and leaves ferric oxide.
    gt.chemical_reactor('gtcsolo:leach_copper_ferrite')
        .itemInputs('4x gtcsolo:copper_ferrite_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 4000))
        .itemOutputs('4x gtceu:hematite_dust')
        .outputFluids(fluid('gtceu:sulfuric_copper_solution', 4000))
        .duration(160).EUt(120);
});
