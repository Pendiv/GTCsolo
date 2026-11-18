// Rare-earth extraction and Pr-Nd-Fe-B magnet processing.
ServerEvents.recipes(event => {
    const fluids = ['gtcsolo:praseodymium_rare_earth_leachate', 'gtcsolo:praseodymium_neodymium_iron_boron'];
    const items = ['gtceu:praseodymium_dust', 'gtcsolo:praseodymium_oxide_dust',
        'gtcsolo:long_praseodymium_neodymium_iron_boron_rod', 'gtcsolo:long_magnetic_praseodymium_rod'];
    if (fluids.some(id => !Fluid.exists(id)) || items.some(id => !Item.exists(id))) {
        console.warn('[Praseodymium magnet] Restart Minecraft to register material forms.');
        return;
    }

    const gt = event.recipes.gtceu;
    // The multiblock combines leaching, separation and precipitation at recipe scale.
    gt.rare_mineral_refining('gtcsolo:praseodymium_magnet/rare_earth_leaching')
        .itemInputs('8x gtceu:rare_earth_dust')
        .inputFluids(Fluid.of('gtceu:hydrochloric_acid', 4000))
        .outputFluids(Fluid.of('gtcsolo:praseodymium_rare_earth_leachate', 4000))
        .duration(320).EUt(1920);
    gt.rare_mineral_refining('gtcsolo:praseodymium_magnet/oxide_separation')
        .itemInputs('18x gtceu:sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtcsolo:praseodymium_rare_earth_leachate', 4000))
        .itemOutputs('5x gtcsolo:praseodymium_oxide_dust', '12x gtceu:salt_dust')
        .outputFluids(Fluid.of('minecraft:water', 3000))
        .duration(400).EUt(7680);
    gt.rare_mineral_refining('gtcsolo:praseodymium_magnet/oxide_reduction')
        .itemInputs('5x gtcsolo:praseodymium_oxide_dust', '3x gtceu:calcium_dust')
        .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 6000))
        .itemOutputs('2x gtceu:praseodymium_dust', '9x gtcsolo:calcium_fluoride_dust')
        .outputFluids(Fluid.of('minecraft:water', 3000))
        .duration(240).EUt(7680);

    // 1:1 substitution of Pr for Nd in the rare-earth sites of (Pr,Nd)2Fe14B.
    gt.precision_blast_furnace('gtcsolo:praseodymium_magnet/alloy')
        .itemInputs('gtceu:praseodymium_dust', 'gtceu:neodymium_dust',
            '14x gtceu:iron_dust', 'gtceu:boron_dust')
        .inputFluids(Fluid.of('gtceu:argon', 1000))
        .outputFluids(Fluid.of('gtcsolo:praseodymium_neodymium_iron_boron', 2448))
        .blastFurnaceTemp(2700).duration(800).EUt(7680);
    gt.polarizer('gtcsolo:praseodymium_magnet/polarize_long_rod')
        .itemInputs('gtcsolo:long_praseodymium_neodymium_iron_boron_rod')
        .itemOutputs('gtcsolo:long_magnetic_praseodymium_rod')
        .duration(400).EUt(491520);
});
