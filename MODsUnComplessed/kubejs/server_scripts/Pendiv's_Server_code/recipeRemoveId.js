ServerEvents.recipes(event => {
    // 削除したいレシピIDをこの配列に追加していく
    const recipesToRemove = [
    'mekanism:metallurgic_infusing/alloy/infused',
    'mekanism:metallurgic_infusing/alloy/reinforced',
    'mekanism:metallurgic_infusing/alloy/atomic',
    'evolvedmekanism:metallurgic_infusing/alloy/hypercharged',
    'evolvedmekanism:nucleosynthesizing/alloy_subatomic',
     'evolvedmekanism:metallurgic_infusing/alloy/singular',
      'evolvedmekanism:metallurgic_infusing/alloy/exoversal',
    ]

    // 配列の中身をループして一括削除
    recipesToRemove.forEach(recipeID => {
        event.remove({ id: recipeID })
    })
})