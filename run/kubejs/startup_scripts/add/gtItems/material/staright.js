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
// material 'starlight' registration moved to Java: see DIV.gtcsolo.registry.ModMaterials#STARLIGHT
// (gtcsolo namespace に統一移植済 — element 部のみここに残置)
