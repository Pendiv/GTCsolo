ServerEvents.recipes(event => {
    // Follow Mekanism's standard 1:1 bidirectional Rotary Condensentrator format.
    event.custom({
        type: 'mekanism:rotary',
        gasInput: { gas: 'mekanismelements:californium', amount: 1 },
        gasOutput: { gas: 'mekanismelements:californium', amount: 1 },
        fluidInput: { fluid: 'kubejs:liquid_californium', amount: 1 },
        fluidOutput: { fluid: 'kubejs:liquid_californium', amount: 1 }
    }).id('gtcsolo:rotary/californium');

    // Z: 98 + 28 = 126. Anteria is fictional; isotope selection/neutrons are abstracted.
    // Initial tuning: standard 16 mB metal quantities, UV, 64 ticks; startup EU is user-specified.
    event.recipes.gtceu.fusion_reactor('gtcsolo:californium_and_nickel_to_anteria_plasma')
        .inputFluids(Fluid.of('kubejs:liquid_californium', 16), Fluid.of('gtceu:nickel', 16))
        .outputFluids(Fluid.of('gtcsolo:anteria_plasma', 16))
        .duration(64)
        .EUt(524288)
        .fusionStartEU(500000000);

    // Same recipe power as native plasma fuels; 3072 ticks is 16x nickel's 192 ticks.
    // The condensate remains available for casting the metal into parts.
    event.recipes.gtceu.plasma_generator('gtcsolo:anteria')
        .inputFluids(Fluid.of('gtcsolo:anteria_plasma', 1))
        .outputFluids(Fluid.of('gtcsolo:anteria', 1))
        .duration(3072)
        .EUt(-2048);
});
