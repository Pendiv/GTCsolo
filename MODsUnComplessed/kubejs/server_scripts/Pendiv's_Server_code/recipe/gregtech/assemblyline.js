ServerEvents.recipes(event => {


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
'12x mekanism_extras:supreme_control_circuit',
'16x gtceu:fusion_casing',
'32x gtceu:naquadah_alloy_frame','16x gtceu:naquadah_alloy_rotor','32x gtceu:long_naquadah_alloy_rod'
,'16x gtceu:naquadah_alloy_gear'
    )
    .inputFluids(
  'gtceu:soldering_alloy 2880',
  'gtceu:naquadria 576',
  'gtceu:europium 1024'
)

    // 出力（アイテム）
    .itemOutputs('1x gtceu:chaos_void_absorber') 
    .EUt(524288)
    .duration(1200)

  // ここに次のレシピを続けて書ける
})

ServerEvents.recipes(event => {

    // アセンブリライン (Assembly Line)
    // アウトプット: 2x gravity_tune_casing
    event.recipes.gtceu.assembly_line('gravity_tune_casing')
        .itemInputs(
            '8x gtceu:atomic_casing',
            '16x gtceu:europium_frame',                // ユーロピウムの足場
            '32x gtceu:double_europium_plate',         // 二重ユーロピウムプレート
            '32x gtceu:double_exporonium_plate',       // 二重エクスポロニウムプレート
            '64x gtceu:fine_europium_wire',            // 極細のユーロピウムワイヤー (1枠目)
            '64x gtceu:fine_europium_wire',            // 極細のユーロピウムワイヤー (2枠目)
            '6x #gtceu:circuits/uhv',                  // タグ:UHV回路
            '3x gtceu:uv_field_generator',             // UV空間発生器
            '3x gtceu:uv_emitter'                      // UVエミッタ
        )
        .inputFluids(
            Fluid.of('gtceu:soldering_alloy', 1024),   // はんだ
            Fluid.of('gtceu:naquadria', 576)           // ナクアドリア
        )
        .itemOutputs('2x kubejs:gravity_tune_casing')
        .duration(900)                                 // 加工時間 (適宜調整)
        .EUt(UHV);                                     // UHV電圧定数を使用

})