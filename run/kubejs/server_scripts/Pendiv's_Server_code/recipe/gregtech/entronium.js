ServerEvents.recipes(event => {
    // New startup materials require a complete restart, not only /reload.
    if (!Fluid.exists('gtcsolo:liquid_entro') || !Fluid.exists('gtcsolo:ma_fluid') ||
        !Fluid.exists('gtcsolo:entronium') || !Item.exists('gtcsolo:entro_crystal_gem')) {
        console.warn('[Entronium] Restart Minecraft to register the energy materials.');
        return;
    }

    // The pack's fusion recipes use the nominal UV voltage. Amounts are mB.
    event.recipes.gtceu.fusion_reactor('gtcsolo:entronium')
        .inputFluids(Fluid.of('gtcsolo:liquid_entro', 144), Fluid.of('gtcsolo:ma_fluid', 4))
        .outputFluids(Fluid.of('gtcsolo:entronium', 144))
        .duration(32)
        .EUt(524288)
        .fusionStartEU(1000000000);

    // No extra heat tier was specified: use the ordinary furnace's initial 1,800 K threshold.
    event.recipes.gtceu.electric_blast_furnace('gtcsolo:entro_crystal_liquefaction')
        .itemInputs('gtcsolo:entro_crystal_gem')
        .outputFluids(Fluid.of('gtcsolo:liquid_entro', 144))
        .blastFurnaceTemp(1800)
        .duration(600)
        .EUt(524288);
});
