ServerEvents.recipes(event => {
  // Mek標準の高速な粉生成とOsmium Compressor直通を、GT側の工程へ一本化する。
  event.remove({ id: 'mekanism:processing/refined_obsidian/dust/from_obsidian_dust' })
  event.remove({ id: 'mekanism:processing/refined_obsidian/ingot/from_dust' })
})
