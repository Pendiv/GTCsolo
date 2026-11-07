ServerEvents.recipes(event => {

    event.recipes.gtceu.electric_blast_furnace('gtcsolo:pack/electric_blast_furnace/structural_glass')
        .itemInputs('#forge:glass/silica', '#forge:ingots/steel')
        .itemOutputs('4x mekanism:structural_glass')
        .duration(170)
        .EUt(496)
        .blastFurnaceTemp(1400);
});
