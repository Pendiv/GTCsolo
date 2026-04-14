ServerEvents.recipes(event => {

    // ミキサー (Mixer) を使用したレシピ
    // 銅の粉(1) + ニッケルの粉(2) -> モネルの粉(3)
    event.recipes.gtceu.mixer('monel_dust_mixing')
        .itemInputs('gtceu:copper_dust', '2x gtceu:nickel_dust')
        .itemOutputs('3x gtceu:monel_dust')
        .duration(100)
        .EUt(16);

})