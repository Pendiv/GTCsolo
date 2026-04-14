GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
  event.create('gravity_well').category('gravity_well').setMaxIOSize(3, 1, 3, 1)
  event.create('gtneh').category('gtneh').setMaxIOSize(2, 1, 1, 1)
  event.create('injection').category('injection').setMaxIOSize(1,0,0,1)   
  event.create('precision_centrifuge').category('precision_centrifuge').setMaxIOSize(3,3,6,6)
  event.create('prototype_assembler').category('prototype_assembler').setMaxIOSize(9,3,3,3)
  event.create('local_transfer').category('transfer').setMaxIOSize(1,5,1,3).setProgressBar(GuiTextures.PROGRESS_BAR_FUSION, FillDirection.LEFT_TO_RIGHT)
  event.create('dimension_transfer').category('transfer').setMaxIOSize(1,5,1,3).setProgressBar(GuiTextures.PROGRESS_BAR_FUSION, FillDirection.LEFT_TO_RIGHT)
})

GTCEuStartupEvents.registry('gtceu:world_gen_layer', event => {
  event.create('ameijia')
    .targets(
      'minecraft:deepslate',
      'minecraft:cobbled_deepslate',
      '#minecraft:deepslate_ore_replaceables'
    )
  
    .dimensions('gtcsolo:ameijia')
})