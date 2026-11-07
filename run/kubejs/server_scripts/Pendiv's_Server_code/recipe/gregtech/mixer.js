ServerEvents.recipes(event => {

    event.recipes.gtceu.mixer('monel_dust_mixing')
        .itemInputs('gtceu:copper_dust', '2x gtceu:nickel_dust')
        .itemOutputs('3x gtcsolo:monel_dust')
        .duration(100)
        .EUt(16);

})
