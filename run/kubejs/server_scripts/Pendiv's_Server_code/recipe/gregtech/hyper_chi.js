ServerEvents.recipes(event => {
    // The original furnace route still returns one scrap per debris. This powered route
    // recovers its Sculk fraction as well, without multiplying the vanilla scrap yield.
    event.recipes.gtceu.macerator('gtcsolo:ancient_debris_dust')
        .itemInputs('minecraft:ancient_debris')
        .itemOutputs('2x gtcsolo:ancient_debris_dust')
        .duration(100).EUt(480);

    event.recipes.gtceu.centrifuge('gtcsolo:ancient_debris_sculk_recovery')
        .itemInputs('2x gtcsolo:ancient_debris_dust')
        .itemOutputs('minecraft:netherite_scrap', 'gtceu:sculk_dust')
        .duration(200).EUt(480);

    // Fictional fusion; initial tuning fits MK I's LuV / 160 MEU limits.
    event.recipes.gtceu.fusion_reactor('gtcsolo:hyper_chi')
        .inputFluids(Fluid.of('gtceu:sculk', 144), Fluid.of('gtcsolo:hyperium', 144))
        .outputFluids(Fluid.of('gtcsolo:hyper_chi', 288))
        .duration(200).EUt(32768).fusionStartEU(140000000);

    event.recipes.gtceu.vacuum_freezer('gtcsolo:hyper_chi_dust')
        .inputFluids(Fluid.of('gtcsolo:hyper_chi', 144))
        .itemOutputs('gtcsolo:hyper_chi_dust')
        .duration(100).EUt(32768);

    // This is the synthesis from its two feedstocks. Existing finished-alloy recycling stays intact.
    event.recipes.gtcsolo.extreme_temperature_blast_furnace('gtcsolo:hyperx_neutronium')
        .itemInputs('gtcsolo:hyper_chi_dust', 'gtceu:neutronium_dust')
        .outputFluids(Fluid.of('gtcsolo:hyperx_neutronium', 288))
        .duration(2400).EUt(2097152)
        .blastFurnaceTemp(22000)
        .addData('gtcsolo_ismbf_start_temperature', 12000);
});
