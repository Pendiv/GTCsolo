ServerEvents.recipes(event => {

    event.shaped('gtceu:uev_machine_casing', [
        "PPP",
        "PWP",
        "PPP"
    ], {
        P: 'gtceu:jupitate_plate',
        // タグを文字列で指定し、その後に .damageIngredient() を繋げる
        // もしこれでもエラーが出る場合は、下記【回避策】を試してください
        W: '#forge:tools/wrenches'
    }).damageIngredient('#forge:tools/wrenches')

})