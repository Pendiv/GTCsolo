ServerEvents.recipes(event => {

    event.shaped('gtceu:uev_machine_casing', [
        "PPP",
        "PWP",
        "PPP"
    ], {
        P: 'gtcsolo:jupitate_plate',
        W: '#forge:tools/wrenches'
    }).damageIngredient('#forge:tools/wrenches')

})
