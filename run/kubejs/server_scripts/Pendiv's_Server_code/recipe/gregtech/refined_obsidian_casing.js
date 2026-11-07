ServerEvents.recipes(event => {

    event.recipes.gtceu.assembler('refined_obsidian_casing')
        .itemInputs(
            '8x #forge:frames/refined_obsidian',
            '8x #forge:frames/refined_glowstone',
            '8x #forge:frames/steel',
            '6x #forge:dense_plates/refined_obsidian',
            '6x #forge:dense_plates/steel',
            '24x #forge:plates/refined_obsidian',
            '6x #forge:gears/refined_obsidian',
            '6x #forge:gears/refined_glowstone'
        )
        .inputFluids('gtceu:soldering_alloy 576')
        .circuit(6)
        .itemOutputs('2x gtcsolo:refined_obsidian_casing')
        .duration(600)
        .EUt(IV);

})
