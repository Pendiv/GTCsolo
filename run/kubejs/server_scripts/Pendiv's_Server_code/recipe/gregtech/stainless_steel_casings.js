ServerEvents.recipes(event => {
    var casingsPerCraft = Java.loadClass('com.gregtechceu.gtceu.config.ConfigHolder').INSTANCE.recipes.casingsPerCraft;
    var plate = '#forge:plates/stainless_steel';
    var rod = '#forge:rods/stainless_steel';
    var rotor = '#forge:rotors/stainless_steel';
    var frame = 'gtceu:stainless_steel_frame';
    var pipe = 'gtceu:stainless_steel_normal_fluid_pipe';

    event.shaped('2x gtcsolo:stainless_steel_firebox_casing', ['PSP', 'SFS', 'PSP'], {
        P: plate, S: rod, F: frame
    }).id('gtcsolo:stainless_steel_firebox_casing');

    event.shaped(casingsPerCraft + 'x gtcsolo:stainless_steel_pipe_casing', ['PIP', 'IFI', 'PIP'], {
        P: plate, I: pipe, F: frame
    }).id('gtcsolo:stainless_steel_pipe_casing');

    event.shaped(casingsPerCraft + 'x gtcsolo:stainless_steel_engine_intake_casing', ['PhP', 'RFR', 'PwP'], {
        P: pipe, R: rotor, F: 'gtceu:clean_machine_casing',
        h: '#forge:tools/hammers', w: '#forge:tools/wrenches'
    }).damageIngredient('#forge:tools/hammers').damageIngredient('#forge:tools/wrenches')
        .id('gtcsolo:stainless_steel_engine_intake_casing');

    event.recipes.gtceu.assembler('gtcsolo:stainless_steel_engine_intake_casing')
        .itemInputs('2x ' + rotor, '4x ' + pipe, 'gtceu:clean_machine_casing')
        .itemOutputs(casingsPerCraft + 'x gtcsolo:stainless_steel_engine_intake_casing')
        .duration(50).EUt(16);
});
