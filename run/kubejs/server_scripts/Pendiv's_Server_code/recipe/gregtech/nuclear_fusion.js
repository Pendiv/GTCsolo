// Experimentally demonstrated element combinations, represented with the native fusion recipe type.
// User-approved abstraction: use generic elements until their isotope-separation routes are authored.
// Zinc-70 already has a dedicated supply chain and remains mandatory for the copernicium route.
// Beam apparatus, neutron emission, isotope-specific decay, bulk yield and energy units are abstracted.
ServerEvents.recipes(event => {
    const LuV = 30720;
    const UV = 491520;
    const UHV = 2097152;
    // [id, input A, mB A, input B, mB B, output, seconds, EU/t, startup MEU]
    // Metal quantities and gas quantities follow native fusion conventions; every output is 16 mB.
    const reactions = [
        ['calcium_hydrogen_scandium', 'gtceu:calcium', 16, 'gtceu:hydrogen', 125,
            'gtceu:scandium', 20, LuV, 120],
        ['tungsten_deuterium_rhenium', 'gtceu:tungsten', 16, 'gtceu:deuterium', 125,
            'gtceu:rhenium', 40, UV, 480],
        ['bismuth_helium_astatine', 'gtceu:bismuth', 16, 'gtceu:helium', 125,
            'gtceu:astatine', 30, UV, 600],
        ['einsteinium_helium_mendelevium', 'gtcsolo:einsteinium', 16, 'gtceu:helium', 125,
            'gtceu:mendelevium', 60, UHV, 900],
        ['curium_carbon_nobelium', 'gtceu:curium', 16, 'gtceu:carbon', 16,
            'gtceu:nobelium', 60, UHV, 960],
        ['californium_boron_lawrencium', 'kubejs:liquid_californium', 16, 'gtceu:boron', 16,
            'gtceu:lawrencium', 72, UHV, 1000],
        ['californium_carbon_rutherfordium', 'kubejs:liquid_californium', 16, 'gtceu:carbon', 16,
            'gtceu:rutherfordium', 80, UHV, 1040],
        ['californium_nitrogen_dubnium', 'kubejs:liquid_californium', 16, 'gtceu:nitrogen', 125,
            'gtceu:dubnium', 80, UHV, 1080],
        ['californium_oxygen_seaborgium', 'kubejs:liquid_californium', 16, 'gtceu:oxygen', 125,
            'gtceu:seaborgium', 90, UHV, 1120],
        ['bismuth_chromium_bohrium', 'gtceu:bismuth', 16, 'gtceu:chromium', 16,
            'gtceu:bohrium', 90, UHV, 1160],
        ['lead_iron_hassium', 'gtceu:lead', 16, 'gtceu:iron', 16,
            'gtceu:hassium', 100, UHV, 1200],
        ['bismuth_iron_meitnerium', 'gtceu:bismuth', 16, 'gtceu:iron', 16,
            'gtceu:meitnerium', 110, UHV, 1240],
        ['bismuth_nickel_roentgenium', 'gtceu:bismuth', 16, 'gtceu:nickel', 16,
            'gtceu:roentgenium', 120, UHV, 1280],
        ['lead_zinc_70_copernicium', 'gtceu:lead', 16, 'gtcsolo:zinc_70', 16,
            'gtceu:copernicium', 120, UHV, 1280]
    ];

    const missing = [];
    reactions.forEach(row => {
        [row[1], row[3], row[5]].forEach(id => {
            if (!Fluid.exists(id) && missing.indexOf(id) < 0) missing.push(id);
        });
    });
    if (missing.length > 0) {
        console.warn('[Nuclear fusion] Restart Minecraft to register material fluids: ' + missing.join(', '));
        return;
    }
    reactions.forEach(row => {
        event.recipes.gtceu.fusion_reactor('gtcsolo:nuclear_fusion/' + row[0])
            .inputFluids(Fluid.of(row[1], row[2]), Fluid.of(row[3], row[4]))
            .outputFluids(Fluid.of(row[5], 16))
            .duration(row[6] * 20).EUt(row[7]).fusionStartEU(row[8] * 1000000);
    });

    // Preserve the existing Mekanism: Elements source of curium instead of adding a second waste-processing chain.
    if (Platform.isLoaded('mekanismelements')) {
        event.custom({
            type: 'mekanism:rotary',
            gasInput: {gas: 'mekanismelements:curium', amount: 1},
            gasOutput: {gas: 'mekanismelements:curium', amount: 1},
            fluidInput: {fluid: 'gtceu:curium', amount: 1},
            fluidOutput: {fluid: 'gtceu:curium', amount: 1}
        }).id('gtcsolo:rotary/curium');
    }
    // No recipes are registered here for Fl/Mc/Lv/Ts/Og pending the MK V design.
    // Native darmstadtium production and the dedicated nihonium route are unchanged.
});
