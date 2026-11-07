ServerEvents.recipes(event => {

    event.recipes.gtceu.assembly_line('wen_basic_energy_cell')
        .itemInputs(
            '16x gtcsolo:wen_mainstorage_casing',
            '6x #gtceu:circuits/luv',
            '7x #forge:dense_plates/rhodium_plated_palladium',
            '7x #forge:dense_plates/rhodium_plated_palladium',
            '7x #forge:dense_plates/rhodium_plated_palladium',
            '7x #forge:dense_plates/rhodium_plated_palladium',
            '6x gtceu:iv_field_generator',
            '6x gtceu:iv_emitter',
            '4x gtceu:lapotronic_energy_orb_cluster'
        )
        .inputFluids(
            'gtceu:tungsten 2880',
            'gtceu:soldering_alloy 2880'
        )
        .itemOutputs('gtcsolo:wen_basic_energy_cell')
        .duration(1200)
        .EUt(LuV);

})
