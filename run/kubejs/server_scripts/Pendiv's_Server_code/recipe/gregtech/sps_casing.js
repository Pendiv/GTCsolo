ServerEvents.recipes(event => {

    event.recipes.gtceu.assembler('sps_casing')
        .itemInputs(
            '4x #forge:frames/refined_obsidian',
            '4x #forge:storage_blocks/osmium',
            'mekanism:hdpe_sheet',
            'mekanism:pellet_polonium',
            'mekanism:pellet_plutonium',
            '#forge:dense_plates/steel'
        )
        .inputFluids('gtceu:soldering_alloy 588')
        .circuit(6)
        .itemOutputs('2x mekanism:sps_casing')
        .duration(600)
        .EUt(IV);

})
