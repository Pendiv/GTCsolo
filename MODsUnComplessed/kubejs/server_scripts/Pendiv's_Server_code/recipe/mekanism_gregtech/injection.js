ServerEvents.recipes(event => {

    // 吹込み (injection)
    event.recipes.gtceu.injection('injection_redstone_minecraft')
        .itemInputs(
            '1x minecraft:redstone'
        )
        .outputFluids(Fluid.of('gtceu:injection_redstone', 10))
        .duration(16)
        .EUt(8);

})