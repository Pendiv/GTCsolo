// All Mek -> GT processing recipes belong in this file. No controller crafting recipes.
// Values: Mekanism 10.4.16 DEFAULT theoretical steady-state, 1 EU = 4 FE = 10 J.
ServerEvents.recipes(function (event) {
    var fuel = 'kubejs:mek_gt_fusion_fuel';
    var deuterium = 'kubejs:mek_gt_deuterium';
    var tritium = 'kubejs:mek_gt_tritium';
    var steam = 'kubejs:mek_gt_steam';
    // EvaporatingRecipeProvider: water 10 -> brine 1, brine 10 -> lithium 1.
    // Circuit 1: one base conversion/tick, up to HEIGHT parallels on the machine, no EU.
    // Circuits 2..6: 1/2/4/8/16 seconds with 20/40/80/160/320 times the base amounts.
    [
        {id: 'brine', input: 'minecraft:water', output: 'kubejs:mek_gt_brine', eut: 32},
        {id: 'lithium', input: 'kubejs:mek_gt_brine', output: 'kubejs:mek_gt_lithium', eut: 128}
    ].forEach(function (conversion) {
        event.recipes.gtceu.mek_gt_evaporation('kubejs:mek_gt/evaporation/' + conversion.id + '_passive')
            .circuit(1).inputFluids(Fluid.of(conversion.input, 10)).outputFluids(Fluid.of(conversion.output, 1))
            .duration(1).addData('mek_gt_passive', 1);
        [20, 40, 80, 160, 320].forEach(function (ticks, index) {
            event.recipes.gtceu.mek_gt_evaporation('kubejs:mek_gt/evaporation/' + conversion.id + '_circuit_' + (index + 2))
                .circuit(index + 2).inputFluids(Fluid.of(conversion.input, 10 * ticks))
                .outputFluids(Fluid.of(conversion.output, ticks)).duration(ticks).EUt(conversion.eut);
        });
    });
    // Full 28-blade turbine: 1000 mB steam -> 1000 mB water + 1000 EU.
    event.recipes.gtceu.mek_gt_turbine('kubejs:mek_gt/turbine/steam')
        .inputFluids(Fluid.of(steam, 1000)).outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(1).EUt(-1000);
    var modes = [
        {circuit: 1, id: 'dt_air', mixed: false, water: false, parallels: 500},
        {circuit: 2, id: 'dt_water', mixed: false, water: true, parallels: 500},
        {circuit: 3, id: 'mixed_air', mixed: true, water: false, parallels: 49},
        {circuit: 4, id: 'mixed_water', mixed: true, water: true, parallels: 49}
    ];
    modes.forEach(function (mode) {
        var recipe = event.recipes.gtceu.mek_gt_fusion('kubejs:mek_gt/fusion/' + mode.id)
            .circuit(mode.circuit).duration(1)
            .EUt(mode.water ? -25000 : -100000)
            .addData('mek_gt_parallel_limit', mode.parallels);
        if (mode.mixed) {
            recipe.inputFluids(Fluid.of(deuterium, 1), Fluid.of(tritium, 1));
        } else {
            recipe.inputFluids(Fluid.of(fuel, 2));
        }
        if (mode.water) {
            recipe.inputFluids(Fluid.of('minecraft:water', 300000))
                .outputFluids(Fluid.of(steam, 300000));
        }
    });
    console.info('[mek_gt] Registered 17 processing recipes: fusion 4, turbine 1, evaporation 12.');
});
