ServerEvents.recipes(event => {
  event.recipes.gtceu.assembly_line('gtcsolo:advanced_assembly_line')
    .itemInputs(
      '1x gtceu:assembly_line',
      '8x gtceu:atomic_casing',
      '8x gtceu:assembly_line_unit',
      Ingredient.of('#gtceu:circuits/luv', 4),
      '4x gtceu:luv_robot_arm',
      '2x gtceu:luv_sensor',
      '2x gtceu:luv_emitter',
      '16x gtcsolo:hssx_single_wire'
    )
    .inputFluids(
      Fluid.of('gtceu:soldering_alloy', 2304),
      Fluid.of('gtceu:niobium_titanium', 1152)
    )
    .itemOutputs('1x gtceu:advanced_assembly_line')
    .duration(1200)
    .EUt(GTValues.VA[GTValues.LuV])
    .scannerResearch('gtceu:assembly_line');
});
