ServerEvents.recipes(event => {
  // Energetic AlloyはGTCsoloのMV級EBFレシピへ一本化する。
  event.remove({ id: 'enderio:alloy_smelting/energetic_alloy_ingot' })
})
