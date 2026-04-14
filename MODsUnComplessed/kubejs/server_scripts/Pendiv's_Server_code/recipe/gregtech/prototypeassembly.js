ServerEvents.recipes(event => {
event.recipes.gtceu.prototype_assembler('atomic_casing_from_exporonium')
    .itemInputs(
        '32x mekanism:alloy_atomic',
        '16x gtceu:exporonium_frame',
        '16x gtceu:double_exporonium_plate',
        '4x gtceu:dense_exporonium_plate',
        '16x gtceu:double_neutronium_plate',
        '3x #gtceu:circuits/uv',
        '2x gtceu:uv_field_generator',
        '64x gtceu:fine_europium_wire'
    )
    .inputFluids(
        Fluid.of('gtceu:soldering_alloy', 576),
        Fluid.of('gtceu:naquadria', 144)
    )
    .chancedOutput('2x gtceu:atomic_casing', 1000, 3000)
       .duration(400)
        .EUt(UV);
})