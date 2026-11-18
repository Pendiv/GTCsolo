ServerEvents.recipes(event => {
    const filter = 'gtcsolo:damped_washing_filter_casing';
    if (!Item.exists(filter)) return;

    event.recipes.gtceu.assembler(filter)
        .itemInputs('2x gtceu:sterilizing_filter_casing', 'gtceu:vibration_safe_casing',
            '4x gtcsolo:small_elinvar_spring', '2x gtcsolo:electropolished_stainless_casing',
            '4x gtceu:activated_carbon_dust', '4x gtceu:zeolite_dust')
        .inputFluids(Fluid.of('gtcsolo:polyimide', 144))
        .itemOutputs('2x ' + filter)
        .cleanroom(CleanroomType.STERILE_CLEANROOM)
        .duration(400).EUt(GTValues.VA[GTValues.UHV]);

    const controller = 'gtcsolo:damped_washing_cleanroom';
    if (!Item.exists(controller)) return;

    event.recipes.gtceu.assembler(controller)
        .itemInputs('gtceu:cleanroom', '4x ' + filter, '2x gtceu:uhv_sensor',
            '2x gtceu:uhv_electric_pump', '4x #gtceu:circuits/uhv')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs(controller)
        .cleanroom(CleanroomType.STERILE_CLEANROOM)
        .duration(800).EUt(GTValues.VA[GTValues.UHV]);
});
