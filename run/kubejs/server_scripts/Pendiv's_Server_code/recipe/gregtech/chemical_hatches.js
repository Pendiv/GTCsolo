// Match GTCEu 1.6.4 fluid-hatch assembly: hull, reservoir, binder, 300 ticks, tier VA.
// Chemical tanks accept Mekanism's gas, infusion, pigment and slurry contents.
ServerEvents.recipes(event => {
    var Components = Java.loadClass('com.gregtechceu.gtceu.data.recipe.CraftingComponent');
    var Values = Java.loadClass('com.gregtechceu.gtceu.api.GTValues');
    var tiers = ['lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv'];
    var tanks = ['basic', 'advanced', 'elite', 'ultimate'];
    var binders = ['glue', 'polyethylene', 'polytetrafluoroethylene', 'polybenzimidazole'];
    // Circuits distinguish the chemical handler and direction without arbitrary extra materials.
    var families = [
        {key: 'gas', tanks: 1, circuit: 1},
        {key: 'infusion', tanks: 1, circuit: 3},
        {key: 'other', tanks: 2, circuit: 5},
        {key: 'universal', tanks: 1, circuit: 7}
    ];

    for (var index = 0; index < tiers.length; index++) {
        var tier = index + 1;
        var tierName = tiers[index];
        var tank = 'mekanism:' + tanks[Math.min(index, tanks.length - 1)] + '_chemical_tank';
        if (!Item.exists(tank)) throw new Error('[Chemical hatches] Missing tank: ' + tank);
        for (var familyIndex = 0; familyIndex < families.length; familyIndex++) {
            var family = families[familyIndex];
            var input = 'gtcsolo:' + family.key + '_input_hatch_' + tierName;
            var output = 'gtcsolo:' + family.key + '_output_hatch_' + tierName;
            if (!Item.exists(input) || !Item.exists(output))
                throw new Error('[Chemical hatches] Missing registered hatches: ' + input + ', ' + output);

            for (var direction = 0; direction < 2; direction++) {
                var result = direction === 0 ? input : output;
                var directionName = direction === 0 ? 'input' : 'output';
                for (var binderIndex = Math.min(3, Math.floor(tier / 2)); binderIndex < binders.length; binderIndex++) {
                    var amount = (288 * (tier + 1)) >> binderIndex;
                    var recipe = event.recipes.gtceu.assembler('gtcsolo:chemical_hatches/' + family.key + '_' +
                            directionName + '_' + tierName + '_' + binders[binderIndex])
                        .itemInputs(Components.HULL.getIngredient(tier), family.tanks + 'x ' + tank)
                        .inputFluids(Fluid.of('gtceu:' + binders[binderIndex], amount))
                        .circuit(family.circuit + direction)
                        .itemOutputs(result).duration(300).EUt(Values.VA[tier]);
                    if (family.key === 'universal') recipe.itemInputs(Components.DRUM.getIngredient(tier));
                }
            }

            // Same workbench screwdriver conversion as the native fluid hatches.
            // The screwdriver's native crafting remainder applies wear and returns the tool.
            event.shaped(output, ['d', 'H'], {d: '#forge:tools/screwdrivers', H: input})
                .id('gtcsolo:chemical_hatches/' + family.key + '_' + tierName + '_to_output');
            event.shaped(input, ['d', 'H'], {d: '#forge:tools/screwdrivers', H: output})
                .id('gtcsolo:chemical_hatches/' + family.key + '_' + tierName + '_to_input');
        }
    }
});
