// Gas-burning generator: composite insulation, material-specific rotor and EV drive equipment.
ServerEvents.recipes(function (event) {
    var output = 'mekanismgenerators:gas_burning_generator';
    var ingredients = [
        '16x gtcsolo:boron_nitride_filled_polyimide_foil',
        '2x gtceu:ev_field_generator',
        '8x gtceu:ev_electric_motor',
        '4x gtceu:ev_electric_pump',
        '2x gtceu:big_titanium_plate',
        '2x gtceu:big_stainless_steel_plate'
    ];
    if (!Item.exists(output)) return;
    if (!Item.exists('gtceu:turbine_rotor') || !Fluid.exists('gtceu:styrene_butadiene_rubber') ||
            ingredients.some(function (input) { return !Item.exists(input.split('x ')[1]); })) {
        console.info('[Gas-burning generator] Missing assembly materials; retaining existing recipes until the next restart.');
        return;
    }

    // All rotor materials share the same item ID. Match the material while allowing ordinary extra tags.
    var rotor = Item.of('gtceu:turbine_rotor', {'GT.PartStats': {Material: 'gtceu:ultimet'}}).weakNBT();
    // Skip unrelated tag outputs before KubeJS's parallel search resolves them through Cucumber's shared file.
    event.remove({output: output, not: {or: [
        {type: 'cucumber:shaped_tag'},
        {type: 'cucumber:shapeless_tag'}
    ]}});
    event.recipes.gtceu.assembler('gtcsolo:mekanism/gas_burning_generator')
        .itemInputs(ingredients[0], rotor, ingredients[1], ingredients[2], ingredients[3], ingredients[4], ingredients[5])
        .inputFluids(Fluid.of('gtceu:styrene_butadiene_rubber', 2304))
        .itemOutputs(output)
        .duration(45 * 20).EUt(GTValues.VA[GTValues.EV]);
});
