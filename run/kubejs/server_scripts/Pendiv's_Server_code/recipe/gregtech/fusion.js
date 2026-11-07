ServerEvents.recipes(event => {
    // Fusion-evaporation: 78Se + 4He -> 80Kr + 2n;
    // 126Te + 4He -> 128Xe + 2n. Isotopes/neutrons are abstracted as in GT fusion.
    // Match GT's 16 mB solid / 125 mB gas convention, rather than equal fluid volumes.
    // MK I: LuV ceiling 32768 EU/t, 10 MEU capacity per energy hatch (max 160 MEU).
    event.recipes.gtceu.fusion_reactor('gtcsolo:selenium_and_helium_to_krypton_plasma')
        .inputFluids(Fluid.of('gtceu:selenium', 16), Fluid.of('gtceu:helium', 125))
        .outputFluids(Fluid.of('gtceu:krypton_plasma', 125))
        .duration(32)
        .EUt(24576)
        .fusionStartEU(120000000);

    event.recipes.gtceu.fusion_reactor('gtcsolo:tellurium_and_helium_to_xenon_plasma')
        .inputFluids(Fluid.of('gtceu:tellurium', 16), Fluid.of('gtceu:helium', 125))
        .outputFluids(Fluid.of('gtceu:xenon_plasma', 125))
        .duration(64)
        .EUt(24576)
        .fusionStartEU(150000000);

    // Amounts are mB. UV here is an explicit local value, independent of other scripts.
    const fusionUV = 524288;
    const fusionStartEU = 500000000;
    event.recipes.gtceu.fusion_reactor('mithlite_fusion')
        .inputFluids(Fluid.of('gtcsolo:mithril', 32), Fluid.of('gtcsolo:pluslitherite', 128))
        .outputFluids(Fluid.of('gtcsolo:mithlite', 32))
        .duration(100)
        .EUt(fusionUV)
        .fusionStartEU(fusionStartEU);

    event.recipes.gtceu.fusion_reactor('mithrilite_fusion')
        .inputFluids(Fluid.of('gtcsolo:mithlite', 128), Fluid.of('gtceu:trinium', 128))
        .outputFluids(Fluid.of('gtcsolo:mithrilite', 128))
        .duration(400)
        .EUt(fusionUV)
        .fusionStartEU(fusionStartEU);

    event.recipes.gtceu.fusion_reactor('exporonium_fusion')
        .inputFluids(
            Fluid.of('gtceu:enriched_naquadah_trinium_europium_duranide', 144),
            Fluid.of('gtceu:poronium', 144)
        )
        .outputFluids(Fluid.of('gtcsolo:exporonium', 144))
        .duration(16)
        .EUt(UV);

})
