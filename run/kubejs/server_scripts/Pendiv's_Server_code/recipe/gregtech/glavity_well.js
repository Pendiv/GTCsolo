ServerEvents.recipes(event => {
    event.recipes.gtceu.gravity_well('singularitys_of_gtnh')
        .inputFluids('gtceu:gold 2880000')
        .itemOutputs('gtcsolo:singularity_gold_dust')
        .duration(3600)
        .EUt(GTValues.V[GTValues.UHV]);
});
