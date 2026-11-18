// Experimental isotope reactions represented by ordinary elements in the fusion reactor.
// Isotope enrichment, emitted particles, radioactive decay and real-world yield are abstracted.
ServerEvents.recipes(event => {
    // [id, input, output, duration (ticks), EU/t, start EU, second input (optional)]
    const reactions = [
        // 70Ge + 4He -> 73Se + n
        ['germanium_helium_selenium', 'gtceu:germanium', 'gtceu:selenium', 200, 24576, 120000000],
        // 115In + 4He -> 118Sb + n
        ['indium_helium_antimony', 'gtceu:indium', 'gtceu:antimony', 400, 30720, 160000000],
        // 89Y + 4He -> 92Nb + n
        ['yttrium_helium_niobium', 'gtceu:yttrium', 'gtceu:niobium', 800, 122880, 240000000],
        // 176Hf + 4He -> 179W + n
        ['hafnium_helium_tungsten', 'gtceu:hafnium', 'gtceu:tungsten', 800, 491520, 480000000],
        // 185Re + 4He -> 185Ir + 4n
        ['rhenium_helium_iridium', 'gtceu:rhenium', 'gtceu:iridium', 1600, 491520, 600000000],
        // Measured deuteron-induced reactions on natural dysprosium produce holmium isotopes.
        ['dysprosium_deuterium_holmium', 'gtcsolo:dysprosium', 'gtcsolo:holmium', 800, 122880, 240000000, 'gtceu:deuterium']
    ];

    reactions.forEach(row => {
        const secondInput = row[6] || 'gtceu:helium';
        const missing = [row[1], secondInput, row[2]].filter(id => !Fluid.exists(id));
        if (missing.length > 0) {
            console.warn('[Rare element fusion] ' + row[0] + ' requires missing fluids: ' + missing.join(', '));
            return;
        }
        event.recipes.gtceu.fusion_reactor('gtcsolo:rare_element_fusion/' + row[0])
            .inputFluids(Fluid.of(row[1], 16), Fluid.of(secondInput, 125))
            .outputFluids(Fluid.of(row[2], 16))
            .duration(row[3]).EUt(row[4]).fusionStartEU(row[5]);
    });
});
