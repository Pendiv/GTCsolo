// priority: -100
(() => {
const ChemicalCapabilities = Java.loadClass('DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities')
const ChemicalIngredient = Java.loadClass('DIV.gtcsolo.integration.mekanism.capability.ChemicalIngredient')
const LV = 32

ServerEvents.recipes(event => {
    let count = 0
    event.forEachRecipe({ type: 'mekanism:infusion_conversion' }, recipe => {
        const ridStr = String(recipe.id)
        let data
        try {
            data = JSON.parse(String(recipe.json))
        } catch (e) {
            console.warn('[conversion mirror] parse failed ' + ridStr + ': ' + e)
            return
        }
        const out = data.output
        const ing = data.input && data.input.ingredient
        if (!out || !out.amount || !out.infuse_type || !ing) return

        // Keep the original input count and the whole ingredient, including alternatives/NBT.
        const itemInput = Ingredient.of(ing, data.input.amount || 1)

        const newAmount = Math.floor(out.amount * 1.5)
        const duration = newAmount * 8
        const idSuffix = String(ridStr).replace(/[:/]/g, '_')
        const newId = 'mirror_' + idSuffix

        event.recipes.gtcsolo.industrial_infusion_conversion(newId)
            .itemInputs(itemInput)
            .output(ChemicalCapabilities.INFUSION,
                ChemicalIngredient.infusion(out.infuse_type, newAmount))
            .duration(duration)
            .EUt(LV)
        count++
    })
    console.info('[conversion mirror] generated ' + count + ' industrial_infusion_conversion recipes')
})
})()
