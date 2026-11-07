ServerEvents.recipes(event => {
    if (!Platform.isLoaded('fantasy_armor')) return;
    event.remove({ mod: 'fantasy_armor' })
    // 出力が fantasy_armor 装備のレシピを除去するが、自前の Fantasy Builder レシピ
    // (gtcsolo:fantasy_armor_build) は装備を出力するため巻き添えにしない (not で除外)
    // KJS checks "not" before "output". Cucumber tag recipes resolve their output
    // through a shared config file, so exclude them before the parallel output scan.
    event.remove({ output: '@fantasy_armor', not: { or: [
        { type: 'gtcsolo:fantasy_armor_build' },
        { type: 'cucumber:shaped_tag' },
        { type: 'cucumber:shapeless_tag' }
    ] } })
})
