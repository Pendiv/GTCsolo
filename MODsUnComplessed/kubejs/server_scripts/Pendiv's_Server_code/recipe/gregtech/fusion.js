ServerEvents.recipes(event => {
    event.recipes.gtceu.fusion_reactor('exporonium_fusion')
        .inputFluids(
            Fluid.of('gtceu:enriched_naquadah_trinium_europium_duranide', 144),
            Fluid.of('gtceu:poronium', 144)
        )
        .outputFluids(Fluid.of('gtceu:exporonium', 144))
        .duration(16)   // 加工時間（任意補完）
        .EUt(UV);       // UV電圧

})