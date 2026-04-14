ServerEvents.recipes(event => {
    // 定数: 電圧
    const ZPM = GTValues.V[GTValues.ZPM] // 131,072
    const UV = GTValues.V[GTValues.UV]   // 524,288

    // レシピ1: 液体空気 1,000,000mB -> chaosvoid 10,000mB
    event.recipes.gtceu.void_absorber('chaosvoid_gen')
        .inputFluids('gtceu:liquid_air 1000000') // 液体空気
        .outputFluids('gtceu:chaosvoid 10000')
        .duration(2400) // 120秒 = 2400 ticks
        .EUt(ZPM)

    // レシピ2: 液体ネザー空気 2,000,000mB -> voidsingularity 10,000mB
    event.recipes.gtceu.void_absorber('singularity_gen')
        .inputFluids('gtceu:liquid_nether_air 2000000') // 液体のネザーの空気
        .outputFluids('gtceu:voidsingularity 10000')
        .duration(2400) // 120秒
        .EUt(UV)
})