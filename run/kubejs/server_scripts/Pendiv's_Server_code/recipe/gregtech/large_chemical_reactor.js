(() => {
const ChemicalCapabilities = Java.loadClass('DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities')
const ChemicalIngredient = Java.loadClass('DIV.gtcsolo.integration.mekanism.capability.ChemicalIngredient')

ServerEvents.recipes(event => {

    event.remove({ id: 'mekanism:chemical_infusing/hydrogen_chloride' })
    event.remove({ id: 'mekanism:chemical_infusing/sulfur_trioxide' })
    event.remove({ id: 'mekanism:chemical_infusing/sulfuric_acid' })

    event.recipes.gtceu.large_chemical_reactor('hydrogen_chloride_from_h2_cl2')
        .input(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:hydrogen', 1000))
        .input(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:chlorine', 1000))
        .output(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:hydrogen_chloride', 1000))
        .duration(120)
        .EUt(MV)

    event.recipes.gtceu.large_chemical_reactor('sulfur_trioxide_from_o2_so2')
        .input(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:oxygen', 1000))
        .input(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:sulfur_dioxide', 2000))
        .output(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:sulfur_trioxide', 2000))
        .duration(160)
        .EUt(MV)

    event.recipes.gtceu.large_chemical_reactor('sulfuric_acid_from_so3_h2o')
        .input(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:sulfur_trioxide', 1000))
        .input(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:water_vapor', 1000))
        .output(ChemicalCapabilities.GAS, ChemicalIngredient.gas('mekanism:sulfuric_acid', 1000))
        .duration(180)
        .EUt(HV)

})
})()