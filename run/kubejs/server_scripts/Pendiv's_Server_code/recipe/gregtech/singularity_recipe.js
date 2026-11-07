ServerEvents.recipes(event => {
    const UHV = GTValues.V[GTValues.UHV];

    const SINGULARITY_RECIPES = [
        ['iron', 1],
        ['tritanium', 4],
        ['bismuth', 2],
        ['naquadah', 16],
        ['silver', 2],
        ['tungsten', 8],
        ['osmium', 12],
        ['samarium', 10]
    ];

    SINGULARITY_RECIPES.forEach(([mat, multiplier]) => {
        event.recipes.gtceu.gravity_well(`${mat}_to_singularity`)
            .inputFluids(`gtceu:${mat} 2880000`)
            .itemOutputs(`gtcsolo:singularity_${mat}_dust`)
            .duration(3600)
            .EUt(UHV);

        event.recipes.gtceu.gtneh(`gen_${mat}_singularity`)
            .inputFluids(`gtcsolo:singularity_${mat} 288`) 
            .itemOutputs(`${multiplier}x gtcsolo:singularity_dust`)
            .duration(2400)
            .EUt(UHV);
    });
});
