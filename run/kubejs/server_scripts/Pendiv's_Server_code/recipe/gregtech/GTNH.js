ServerEvents.recipes(event => {
    const UHV = GTValues.V[GTValues.UHV]   
 
    event.recipes.gtceu.gtneh('singularity_gen')
        .inputFluids('gtcsolo:singularity_gold 288') 
         .itemOutputs('gtcsolo:singularity_dust')
        .duration(2400)
        .EUt(UHV)
})
