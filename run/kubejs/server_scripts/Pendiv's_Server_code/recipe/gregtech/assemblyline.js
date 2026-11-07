ServerEvents.recipes(event => {

  if (!Item.exists('gtceu:chaos_void_absorber')) return;
  event.recipes.gtceu.assembly_line('gtceu:chaos_void_absorver')

    .itemInputs(
    '6x gtceu:superconducting_coil',
      Ingredient.of('#gtceu:circuits/zpm', 6),
      '7x gtceu:dense_naquadah_alloy_plate',
      '32x gtceu:double_europium_plate',
      '4x gtceu:zpm_field_generator',
'32x gtceu:zpm_electric_pump',
'64x gtceu:uhpic_chip',
'64x gtceu:uhpic_chip',
'32x gtceu:trinium_single_wire',
'12x #gtceu:circuits/uv',
'16x gtceu:fusion_casing',
'32x gtceu:naquadah_alloy_frame','16x gtceu:naquadah_alloy_rotor','32x gtceu:long_naquadah_alloy_rod'
,'16x gtceu:naquadah_alloy_gear'
    )
    .inputFluids(
  'gtceu:soldering_alloy 2880',
  'gtceu:naquadria 576',
  'gtceu:europium 1024'
)

    .itemOutputs('1x gtceu:chaos_void_absorber') 
    .EUt(524288)
    .duration(1200)

})

ServerEvents.recipes(event => {

    event.recipes.gtceu.assembly_line('gravity_tune_casing')
        .itemInputs(
            '8x gtceu:atomic_casing',
            '16x gtceu:europium_frame',
            '32x gtceu:double_europium_plate',
            '32x gtcsolo:double_exporonium_plate',
            '64x gtceu:fine_europium_wire',
            '64x gtceu:fine_europium_wire',
            '6x #gtceu:circuits/uhv',
            '3x gtceu:uv_field_generator',
            '3x gtceu:uv_emitter'
        )
        .inputFluids(
            Fluid.of('gtceu:soldering_alloy', 1024),
            Fluid.of('gtceu:naquadria', 576)
        )
        .itemOutputs('2x kubejs:gravity_tune_casing')
        .duration(900)
        .EUt(UHV);

})
