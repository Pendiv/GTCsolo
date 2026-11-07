// All amounts are mB. Numbers not explicitly specified by the user are provisional.
ServerEvents.tags('item', event => {
    // Resolve only during the tag event, after GT's material blocks have been registered.
    Java.loadClass('DIV.gtcsolo.integration.kubejs.StoneSlurryTags').addRawOreBlocks(event);
});

ServerEvents.recipes(event => {
    const slurryLV = 32;
    const slurryMV = 128;
    const slurryHV = 512;
    const fluid = (name, amount) => Fluid.of('gtcsolo:' + name, amount);

    event.recipes.gtceu.mixer('gtcsolo:stone_slurry')
        .itemInputs('256x gtceu:stone_dust')
        .inputFluids(Fluid.of('minecraft:water', 1000))
        .outputFluids(fluid('stone_slurry', 2000))
        .duration(100).EUt(slurryLV);

    // Optional basic route: a few useful dusts without the raw-ore-block input.
    event.recipes.gtceu.electrolyzer('gtcsolo:basic_stone_slurry_recovery')
        .inputFluids(fluid('stone_slurry', 1000))
        .itemOutputs('gtceu:iron_dust', 'gtceu:copper_dust', 'gtceu:gold_dust', 'gtceu:coal_dust')
        .duration(100).EUt(slurryLV);

    event.recipes.gtceu.mixer('gtcsolo:molten_ore_slurry')
        .itemInputs('4x #gtcsolo:raw_ore_blocks')
        .inputFluids(Fluid.of('minecraft:lava', 1000), fluid('stone_slurry', 2000))
        .outputFluids(fluid('molten_ore_slurry', 4000))
        .duration(120).EUt(slurryMV);

    event.recipes.gtceu.electric_blast_furnace('gtcsolo:slurry_calcination')
        .inputFluids(fluid('molten_ore_slurry', 1000))
        .itemOutputs('2x gtceu:nether_quartz_dust', '2x gtceu:clay_dust', '2x gtceu:sulfur_dust')
        .outputFluids(fluid('settled_ore_slurry', 1000))
        .blastFurnaceTemp(1800).duration(300).EUt(slurryHV);

    event.recipes.gtceu.electrolyzer('gtcsolo:settled_ore_slurry_recovery')
        .inputFluids(fluid('settled_ore_slurry', 1000))
        .itemOutputs('gtceu:lead_dust', 'gtceu:silver_dust', 'gtceu:gold_dust', 'gtceu:opal_dust')
        .outputFluids(fluid('refractory_mineral_slurry', 750))
        .duration(160).EUt(slurryMV);

    // A supply recipe is needed: this pre-existing material previously had no producer.
    event.recipes.gtceu.chemical_reactor('gtcsolo:hydrogen_peroxide_supply')
        .circuit(24)
        .inputFluids(Fluid.of('gtceu:hydrogen', 2000), Fluid.of('gtceu:oxygen', 2000))
        .outputFluids(fluid('hydrogen_peroxide', 1000))
        .duration(100).EUt(slurryHV);

    event.recipes.gtceu.mixer('gtcsolo:oxidized_mineral_slurry')
        .inputFluids(fluid('refractory_mineral_slurry', 1000), fluid('hydrogen_peroxide', 250))
        .outputFluids(fluid('oxidized_mineral_slurry', 1250))
        .duration(80).EUt(slurryHV);

    event.recipes.gtceu.centrifuge('gtcsolo:oxidized_slurry_separation')
        .inputFluids(fluid('oxidized_mineral_slurry', 1250))
        .itemOutputs('2x minecraft:redstone')
        .outputFluids(fluid('residual_mineral_slurry', 1000),
            Fluid.of('gtceu:oxygen', 250), Fluid.of('gtceu:fluorine', 50))
        .duration(120).EUt(slurryHV);

    // Either polymer works. PTFE doubles slurry yield and keeps its shorter processing time.
    [
        {name: 'polyethylene', time: 100, output: 1250},
        {name: 'polytetrafluoroethylene', time: 60, output: 2500}
    ].forEach(polymer => {
        event.recipes.gtceu.mixer('gtcsolo:acidic_mineral_slurry_' + polymer.name)
            .itemInputs('gtceu:' + polymer.name + '_dust')
            .inputFluids(fluid('residual_mineral_slurry', 1000), Fluid.of('gtceu:nitric_acid', 250))
            .outputFluids(fluid('acidic_mineral_slurry', polymer.output))
            .duration(polymer.time).EUt(slurryHV);
    });

    // Direct registration to the large-reactor type makes the final stage LCR-exclusive.
    event.recipes.gtceu.large_chemical_reactor('gtcsolo:acidic_slurry_manganese')
        .inputFluids(fluid('acidic_mineral_slurry', 1000), Fluid.of('gtceu:mercury', 50))
        .itemOutputs('2x gtceu:manganese_dust')
        .outputFluids(fluid('acidic_mineral_residue', 1000))
        .duration(1200).EUt(slurryHV);
});
