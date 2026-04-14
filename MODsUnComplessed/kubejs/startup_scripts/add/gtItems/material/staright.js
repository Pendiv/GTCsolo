// priority: 999

GTCEuStartupEvents.registry('gtceu:element', event => {
  event.create('starlight')
    .protons(79)
    .neutrons(150)
    .symbol('☆彡')
})
GTCEuStartupEvents.registry('gtceu:element', event => {
  event.create('netherite')
    .protons(79)
    .neutrons(150)
    .symbol('Nr')
})
GTCEuStartupEvents.registry('gtceu:material',event =>{
 event.create('starlight')
 .polymer()
 .color(0xF2E3A3)
 .iconSet(GTMaterialIconSet.SHINY)
 .flags(
GTMaterialFlags.GENERATE_PLATE,
GTMaterialFlags.GENERATE_ROD,
GTMaterialFlags.GENERATE_FRAME

 )
})
