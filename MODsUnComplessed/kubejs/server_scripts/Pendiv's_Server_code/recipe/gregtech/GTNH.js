ServerEvents.recipes(event => {
    const UHV = GTValues.V[GTValues.UHV]   
 
    event.recipes.gtceu.gtneh('singularity_gen')
        .inputFluids('gtceu:singularity_gold 288') 
         .itemOutputs('gtceu:singularity_dust')
        .duration(2400) // 120秒
        .EUt(UHV)
})