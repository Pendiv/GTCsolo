ServerEvents.recipes(event => {
  if (!Platform.isLoaded('powah')) return

  // Powah標準の完成インゴット直出しを、充電済み粉を作る工程へ置き換える。
  event.remove({ id: 'powah:energizing/energized_steel' })

  event.custom({
    type: 'powah:energizing',
    ingredients: [
      { tag: 'forge:dusts/iron' },
      { tag: 'forge:dusts/gold' }
    ],
    energy: 10000,
    result: {
      item: 'gtcsolo:energized_steel_dust',
      count: 2
    }
  }).id('gtcsolo:energizing/energized_steel_dust')
})
