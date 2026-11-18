ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu;
    const fluid = (id, amount) => Fluid.of(id, amount);

    gt.electrolyzer('gtcsolo:electrowin_zinc_from_cementation_liquor')
        .inputFluids(fluid('gtcsolo:zinc_sulfate_solution', 2000))
        .itemOutputs('gtceu:zinc_dust')
        .outputFluids(fluid('gtceu:sulfuric_acid', 1000), fluid('gtceu:oxygen', 500))
        .duration(160).EUt(480);

    gt.chemical_reactor('gtcsolo:regenerate_hydrofluoric_acid_from_fluoride')
        .itemInputs('gtcsolo:calcium_fluoride_dust')
        .inputFluids(fluid('gtceu:sulfuric_acid', 1000), fluid('minecraft:water', 2000))
        .itemOutputs('gtceu:gypsum_dust')
        .outputFluids(fluid('gtceu:hydrofluoric_acid', 2000))
        .duration(180).EUt(480);

    gt.distillation_tower('gtcsolo:recover_ammonia_from_condensate')
        .inputFluids(fluid('gtcsolo:ammoniacal_condensate', 4000))
        .outputFluids(fluid('gtceu:ammonia', 1000), fluid('gtceu:distilled_water', 3000))
        .duration(160).EUt(480);
});
