ServerEvents.recipes(event => {
    // Optional circuit-24 route. Preserve the native PGS/copper-solution yields per ore;
    // additionally collect the trace-element fraction. Do not replace vanilla GT recipes.
    // Native copper electrorefining is abstracted into the existing ore-leaching chain.
    ['chalcopyrite', 'chalcocite', 'bornite', 'tetrahedrite'].forEach(ore => {
        event.recipes.gtceu.chemical_reactor('gtcsolo:recover_selenium_tellurium_from_' + ore)
            .circuit(24)
            .itemInputs('16x gtceu:purified_' + ore + '_ore')
            .inputFluids(Fluid.of('gtceu:nitric_acid', 1600))
            .itemOutputs('32x gtceu:platinum_group_sludge_dust', 'gtcsolo:selenium_tellurium_residue_dust')
            .outputFluids(Fluid.of('gtceu:sulfuric_copper_solution', 16000))
            .duration(800).EUt(480);
    });

    // 4Se + Te + 5O2 -> 4SeO2 (volatile) + TeO2 (solid).
    // Five mixture-dust units contain four Se units and one Te unit.
    event.recipes.gtceu.electric_blast_furnace('gtcsolo:oxidize_selenium_tellurium_residue')
        .itemInputs('5x gtcsolo:selenium_tellurium_residue_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 10000))
        .itemOutputs('3x gtcsolo:tellurium_dioxide_dust')
        .outputFluids(Fluid.of('gtcsolo:selenium_dioxide', 4000))
        .blastFurnaceTemp(900).duration(200).EUt(480);

    // SeO2 + 2SO2 + 2H2O -> Se + 2H2SO4: aqueous dissolution/reduction combined.
    event.recipes.gtceu.chemical_reactor('gtcsolo:selenium_dioxide_reduction')
        .inputFluids(Fluid.of('gtcsolo:selenium_dioxide', 1000),
            Fluid.of('gtceu:sulfur_dioxide', 2000), Fluid.of('minecraft:water', 2000))
        .itemOutputs('gtceu:selenium_dust')
        .outputFluids(Fluid.of('gtceu:sulfuric_acid', 2000))
        .duration(160).EUt(120);

    // GT generates ore crushing/washing/centrifuging and the final electrolysis recipes:
    // 2 PbSe dust -> Pb + Se; 3 AuTe2 dust -> Au + 2Te;
    // 3 TeO2 dust -> Te + 2000 mB O. No duplicate manual decomposition recipes.
});
