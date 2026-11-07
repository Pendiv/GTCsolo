// Pendiv's_StartUp_Code/add/GTfluids/void.js

const GTMaterialIconSet = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet')

GTCEuStartupEvents.registry('gtceu:material', event => {
  event.create('chaosvoid')
    .fluid()
    .color(0x220033)
    .iconSet(GTMaterialIconSet.DULL)
    .formula('∑C')
event.create('poronium')
    .fluid()
    .color(0x220033)
    .iconSet(GTMaterialIconSet.DULL)
    .formula('po')
  event.create('voidsingularity')
    .fluid()
    .color(0x000000)
    .iconSet(GTMaterialIconSet.SHINY)
    .formula('∑?')
    
})

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
  ['ocean_fluid_extraction', 'bedrock_fluid_extraction', 'void_absorber'].forEach(id => {
    event.create(id)
      .category('multiblock')
      .setEUIO('in')
      .setMaxIOSize(1, 0, 0, 1)
      .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT);
  });
})
