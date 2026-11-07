// Small provisional supply routes for the trace additives used by the authored alloys.
// Natural minerals are treated as impure feedstocks; recovery quantities are game balance values.
ServerEvents.recipes(event => {
    if (!Item.exists('gtcsolo:zircon_dust') || !Item.exists('gtceu:germanium_dust')) {
        console.warn('[Industrial feedstocks] Restart Minecraft to register the new material forms.');
        return;
    }
    event.recipes.gtceu.electrolyzer('gtcsolo:industrial_feedstocks/germanium_recovery')
        .itemInputs('64x gtceu:sphalerite_dust').circuit(24)
        .itemOutputs('32x gtceu:zinc_dust', '32x gtceu:sulfur_dust', 'gtceu:tiny_germanium_dust')
        .duration(1600).EUt(480);
    event.recipes.gtceu.centrifuge('gtcsolo:industrial_feedstocks/zircon_separation')
        .itemInputs('64x #minecraft:sand').circuit(24)
        .itemOutputs('8x gtceu:silicon_dioxide_dust', 'gtcsolo:tiny_zircon_dust')
        .duration(1200).EUt(120);
    event.recipes.gtceu.electrolyzer('gtcsolo:industrial_feedstocks/zircon_decomposition')
        .itemInputs('6x gtcsolo:zircon_dust')
        .itemOutputs('gtceu:zirconium_dust', 'gtceu:silicon_dust')
        .outputFluids(Fluid.of('gtceu:oxygen', 4000))
        .duration(4000).EUt(480);
});
