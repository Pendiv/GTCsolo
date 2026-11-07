(() => {
const ChemicalCapabilities = Java.loadClass('DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities')
const ChemicalIngredient = Java.loadClass('DIV.gtcsolo.integration.mekanism.capability.ChemicalIngredient')

ServerEvents.recipes(event => {

    event.recipes.gtcsolo.mekanism_infuser('refined_glowstone_dust_from_diamond_infusion')
        .itemInputs('minecraft:glowstone_dust')
        .input(ChemicalCapabilities.INFUSION, ChemicalIngredient.infusion('mekanism:diamond', 20))
        .itemOutputs('gtcsolo:refined_glowstone_dust')
        .duration(140)
        .EUt(MV)

})
})()