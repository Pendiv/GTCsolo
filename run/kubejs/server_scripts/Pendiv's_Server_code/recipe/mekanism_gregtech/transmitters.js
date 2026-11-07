// Basic transmitters use LV assembly. Advanced elastomers are derived once by rubber_alternatives.js.
(function () {
    var seals = [
        {name: 'rubber', fluid: 'gtceu:rubber', amount: 576},
        {name: 'silicone_rubber', fluid: 'gtceu:silicone_rubber', amount: 72},
        {name: 'styrene_butadiene_rubber', fluid: 'gtceu:styrene_butadiene_rubber', amount: 18},
        {name: 'polyethylene', fluid: 'gtceu:polyethylene', amount: 288},
        {name: 'polytetrafluoroethylene', fluid: 'gtceu:polytetrafluoroethylene', amount: 144},
        {name: 'polybenzimidazole', fluid: 'gtceu:polybenzimidazole', amount: 36}
    ];
    var transmitters = [
        {
            name: 'basic_universal_cable',
            inputs: ['4x gtceu:manganese_phosphide_single_wire', '4x gtceu:steel_plate',
                '4x gtceu:steel_foil', '4x gtceu:red_alloy_ingot', '4x mekanism:alloy_infused']
        },
        {
            name: 'basic_pressurized_tube',
            inputs: ['4x gtceu:tempered_glass', '4x gtceu:steel_plate',
                '4x gtceu:steel_foil', '4x gtceu:red_alloy_ingot', '4x mekanism:alloy_infused']
        },
        {
            name: 'basic_logistical_transporter',
            inputs: ['4x gtceu:brass_normal_item_pipe', '4x gtceu:steel_plate',
                '4x gtceu:steel_foil', '4x gtceu:red_alloy_ingot', '4x mekanism:alloy_infused']
        },
        {
            name: 'basic_mechanical_pipe',
            inputs: ['4x gtceu:steel_normal_fluid_pipe', '4x gtceu:steel_plate',
                '4x gtceu:steel_foil', '4x gtceu:red_alloy_ingot', '4x mekanism:alloy_infused']
        }
    ];

    ServerEvents.recipes(function (event) {
        transmitters.forEach(function (entry) {
            var output = 'mekanism:' + entry.name;
            if (!Item.exists(output)) return;
            if (!Fluid.exists('gtceu:rubber') || entry.inputs.some(function (input) {
                return !Item.exists(input.split('x ')[1]);
            })) {
                console.warn('[Mekanism transmitters] Missing assembly material for ' + output + '; retaining its recipes.');
                return;
            }
            // Skip unrelated tag outputs before KubeJS's parallel search resolves them through Cucumber's shared file.
            event.remove({output: output, not: {or: [
                {type: 'cucumber:shaped_tag'},
                {type: 'cucumber:shapeless_tag'}
            ]}});
            seals.forEach(function (seal) {
                if (!Fluid.exists(seal.fluid)) return;
                event.recipes.gtceu.assembler('gtcsolo:mekanism_transmitters/' + entry.name + '/' + seal.name)
                    .itemInputs(entry.inputs[0], entry.inputs[1], entry.inputs[2], entry.inputs[3], entry.inputs[4])
                    .inputFluids(Fluid.of(seal.fluid, seal.amount))
                    .itemOutputs('64x ' + output)
                    .duration(15 * 20).EUt(GTValues.VA[GTValues.LV]);
            });
        });
    });
})();
