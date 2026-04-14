ServerEvents.recipes(event => {
    event.recipes.gtceu.distillation_tower('ameijia_air_distillation')
   
        .inputFluids(Fluid.of('gtceu:ameijia_air', 2000000))
       
        .outputFluids(
            Fluid.of('gtceu:carbon_dioxide', 1100000),   
            Fluid.of('gtceu:nitrogen', 500000),           
            Fluid.of('gtceu:argon', 160000),             
            Fluid.of('gtceu:ameizia_stagnation', 110000), 
            Fluid.of('gtceu:sulfur_dioxide', 100000),      
            Fluid.of('gtceu:enigma', 432)               
        )
        .duration(12000)
        .EUt(UHV);

})
ServerEvents.recipes(event => {
    let baseChance = 990; 
    event.recipes.gtceu.precision_centrifuge('enigma_centrifugation')

        .itemInputs('gtceu:singularity_dust')

        .inputFluids(Fluid.of('gtceu:ameizia_stagnation', 100000))
    
        .itemOutputs(
            'gtceu:tiny_enigma_dust',
            'gtceu:tiny_singularity_dust'
        )
        .chancedOutput('gtceu:stellarium_enigma_dust', baseChance, baseChance) 
    
        .outputFluids(
            Fluid.of('gtceu:low_constains_enigma', 6000),
            Fluid.of('gtceu:high_constains_enigma', 3000)
        )
        
        .duration(120000)
        .EUt(UV); 
    event.recipes.gtceu.precision_centrifuge('enigma_centrifugation_more')

        .itemInputs('gtceu:singularity_block')

        .inputFluids(Fluid.of('gtceu:ameizia_stagnation', 100000))
    
        .itemOutputs(
            'gtceu:tiny_enigma_dust',
            'gtceu:tiny_singularity_dust'
        )
        .chancedOutput('gtceu:stellarium_enigma_dust', baseChance, 0) 
    
        .outputFluids(
            Fluid.of('gtceu:low_constains_enigma', 6000),
            Fluid.of('gtceu:high_constains_enigma', 3000)
        )
        
        .duration(40000)
        .EUt(UV); 
})
ServerEvents.recipes(event => {

    event.recipes.gtceu.precision_centrifuge('low_enigma_processing')
        .inputFluids(Fluid.of('gtceu:low_constains_enigma', 3000))
        .itemInputs('gtceu:singularity_dust')
        .itemOutputs('6x gtceu:netherite_dust')
        .outputFluids(
            Fluid.of('gtceu:iron', 2304),
            Fluid.of('gtceu:enigma', 432),
            Fluid.of('gtceu:stellarium', 288),
            Fluid.of('gtceu:argon', 900)
        )
        .duration(600)
        .EUt(UEV);
        
    event.recipes.gtceu.precision_centrifuge('high_enigma_processing')
        .inputFluids(Fluid.of('gtceu:high_constains_enigma', 3000))
        .itemInputs('gtceu:singularity_dust')
        .itemOutputs('11x gtceu:netherite_dust')
        .outputFluids(
            Fluid.of('gtceu:iron', 432),
            Fluid.of('gtceu:enigma', 1296),
            Fluid.of('gtceu:stellarium', 576),
            Fluid.of('gtceu:argon', 1200)
        )
        .duration(600)
        .EUt(UEV);
   event.recipes.gtceu.precision_centrifuge('low_enigma_processing_more')
        .inputFluids(Fluid.of('gtceu:low_constains_enigma', 3000))
        .itemInputs('gtceu:singularity_block')
        .itemOutputs('6x gtceu:netherite_dust')
        .outputFluids(
            Fluid.of('gtceu:iron', 2304),
            Fluid.of('gtceu:enigma', 432),
            Fluid.of('gtceu:stellarium', 288),
            Fluid.of('gtceu:argon', 900)
        )
        .duration(400)
        .EUt(UEV);
        
    event.recipes.gtceu.precision_centrifuge('high_enigma_processing_more')
        .inputFluids(Fluid.of('gtceu:high_constains_enigma', 3000))
        .itemInputs('gtceu:singularity_block')
        .itemOutputs('11x gtceu:netherite_dust')
        .outputFluids(
            Fluid.of('gtceu:iron', 432),
            Fluid.of('gtceu:enigma', 1296),
            Fluid.of('gtceu:stellarium', 576),
            Fluid.of('gtceu:argon', 1200)
        )
        .duration(400)
        .EUt(UEV);
})