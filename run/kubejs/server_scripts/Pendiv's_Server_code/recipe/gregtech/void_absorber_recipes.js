ServerEvents.recipes(event => {
    if (!Item.exists('gtceu:chaos_void_absorber') || !GTRecipeTypes.get('ocean_fluid_extraction')) {
        console.info('[Void absorber] Restart with the updated machine before loading its extraction recipes.');
        return;
    }
    // Surface-ocean eligibility is checked by the controller at the drilling column.
    event.recipes.gtceu.ocean_fluid_extraction('gtcsolo:sea_water_extraction')
        .outputFluids(Fluid.of('gtceu:salt_water', 4000))
        .duration(20).EUt(GTValues.VA[GTValues.MV]);

    // Vein recipes are obtained from native BedrockFluidVeinSavedData at the installation site.
    // No fixed oil recipe is registered here: doing so would bypass vein yield and depletion.
    if (Fluid.exists('kubejs:unrefined_ma_fluid')) {
        event.recipes.gtceu.void_absorber('gtcsolo:unrefined_ma_extraction')
            .outputFluids(Fluid.of('kubejs:unrefined_ma_fluid', 100))
            .duration(200).EUt(GTValues.VA[GTValues.UV]);
    } else {
        console.warn('[Void absorber] MA source fluid is missing; restart after its startup registration.');
    }
});
