ServerEvents.recipes(event => {
    // Same material cost and assembly settings as GT's tiered machine casings.
    event.shaped('gtcsolo:mithrilite_machine_casing', ['PPP', 'PWP', 'PPP'], {
        P: '#forge:plates/mithrilite',
        W: '#forge:tools/wrenches'
    }).damageIngredient('#forge:tools/wrenches')
        .id('gtcsolo:mithrilite_machine_casing_crafting');

    event.recipes.gtceu.assembler('gtcsolo:mithrilite_machine_casing')
        .itemInputs('8x #forge:plates/mithrilite')
        .itemOutputs('gtcsolo:mithrilite_machine_casing')
        .circuit(8)
        .duration(50)
        .EUt(16);

    // Inputs copied from cmdex/export/misirite_robst_machine_casing.txt.
    event.recipes.gtceu.assembler('gtcsolo:mithrilite_robust_machine_casing')
        .itemInputs(
            '1x gtcsolo:mithrilite_machine_casing',
            '1x gtcsolo:big_mithrilite_plate',
            '4x gtcsolo:mithrilite_frame',
            '6x gtcsolo:dense_mithrilite_plate',
            '2x gtcsolo:mithrilite_screw',
            '2x gtcsolo:long_mithrilite_rod',
            '2x gtcsolo:mithrilite_rod'
        )
        .itemOutputs('gtcsolo:mithrilite_robust_machine_casing')
        .duration(400)
        .EUt(524288);

    // Inputs copied from cmdex/export/misirite_firebox.txt.
    event.recipes.gtceu.assembler('gtcsolo:mithrilite_firebox_casing')
        .itemInputs(
            '1x gtcsolo:mithrilite_machine_casing',
            '4x gtcsolo:mithrilite_frame',
            '4x gtcsolo:long_mithrilite_rod',
            '4x gtcsolo:mithrilite_rod',
            '4x gtcsolo:double_mithrilite_plate'
        )
        .itemOutputs('gtcsolo:mithrilite_firebox_casing')
        .duration(200)
        .EUt(524288);
});
