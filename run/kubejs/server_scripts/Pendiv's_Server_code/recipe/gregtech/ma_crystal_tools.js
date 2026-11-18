ServerEvents.recipes(event => {
    if (!GTRecipeTypes.get('gtceu:draconic_reconstruction') || !Item.exists('gtcsolo:ma_matter_crystal')) return;

    [
        [1, 'gtcsolo:ma_crystal_pickaxe'],
        [2, 'gtcsolo:ma_crystal_axe'],
        [3, 'gtcsolo:ma_crystal_shovel']
    ].forEach(([configuration, tool]) => {
        if (!Item.exists(tool)) return;

        event.recipes.gtceu.draconic_reconstruction(tool)
            .itemInputs('4x gtcsolo:ma_matter_crystal', '2x gtcsolo:mithrilite_rod')
            .circuit(configuration)
            .inputFluids(Fluid.of('gtceu:polybenzimidazole', 144))
            .itemOutputs(tool)
            .duration(400)
            .EUt(GTValues.VA[GTValues.UHV]);
    });
});
