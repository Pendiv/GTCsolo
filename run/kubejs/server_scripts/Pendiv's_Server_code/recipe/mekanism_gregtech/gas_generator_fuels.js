// Liquid fuel density is Mekanism J/mB. Current conversion: 2.5 J = 1 FE.
ServerEvents.recipes(function (event) {
    var fuels = [
        ['benzene', 48, '42300'],
        ['diesel', 64, '56400'],
        ['cetane_boosted_diesel', 72, '84600'],
        ['nitrobenzene', 64, '112800'],
        ['high_octane_gasoline', 96, '169200']
    ];

    fuels.forEach(function (fuel) {
        event.custom({
            type: 'gtcsolo:gas_generator_liquid_fuel',
            fluid: 'gtceu:' + fuel[0],
            burn_time: fuel[1],
            energy_density: fuel[2]
        }).id('kubejs:gas_generator/' + fuel[0]);
    });
});
