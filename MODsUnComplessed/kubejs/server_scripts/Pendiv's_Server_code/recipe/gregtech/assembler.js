ServerEvents.recipes(event => {

    // 組み立て機 (Assembler)
    // アウトプット: UHVローターホルダー (UHV Rotor Holder)
    event.recipes.gtceu.assembler('uhv_rotor_holder')
        .itemInputs(
            '4x gtceu:small_hsss_gear',         
            '4x gtceu:exporonium_gear',           
            'gtceu:uhv_machine_casing'           
        )
        .itemOutputs('gtceu:uhv_rotor_holder')
        .duration(400)
        .EUt(UV);     
    event.recipes.gtceu.assembler('uev_machine_casing')
        .itemInputs(
            '8x gtceu:jupitate_plate'
        )
        .circuit(8)
        .itemOutputs('gtceu:uev_machine_casing')
        .duration(16)
        .EUt(LV);
event.recipes.gtceu.assembler('high_strength_steel_machine_casing')
.itemInputs(
            '16x gtceu:steel_block','12x gtceu:steel_machine_casing','4x gtceu:tungsten_steel_frame'
            
            ,'32x gtceu:steel_frame','7x gtceu:dense_steel_plate'
            ,'32x gtceu:steel_plate','64x gtceu:fine_steel_wire'

        )
       .inputFluids('gtceu:soldering_alloy 1296',)
        .circuit(6)
         .itemOutputs('gtcsolo:high_strength_steel_casing')
         .duration(50)
         .EUt(EV);
})