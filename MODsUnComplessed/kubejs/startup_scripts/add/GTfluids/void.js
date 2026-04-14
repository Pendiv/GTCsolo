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
  event.create('void_absorber')
    .category('void_absorber')
    .setEUIO('in')
    .setMaxIOSize(0, 0, 3, 1)
})
