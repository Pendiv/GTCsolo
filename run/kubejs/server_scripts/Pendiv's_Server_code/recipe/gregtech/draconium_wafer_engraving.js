// Keep the native lens colors for corresponding wafers. The five additional routes use unused colors.
// Yields continue the native silicon/phosphorus/naquadah/neutronium progression.
ServerEvents.recipes(event => {
    const LuV = 30720;
    const ZPM = 122880;
    const UV = 491520;
    // [wafer, lens color, output count, seconds, EU/t]
    const engravings = [
        ['ilc',                 'red',        32, 10, LuV],
        ['ram',                 'green',      32, 10, LuV],
        ['cpu',                 'light_blue', 32, 10, LuV],
        ['ulpic',               'blue',       32, 10, LuV],
        ['lpic',                'orange',     32, 10, LuV],
        ['mpic',                'brown',      16, 20, LuV],
        ['hpic',                'magenta',     8, 30, ZPM],
        ['uhpic',               'black',       4, 40, UV],
        ['simple_soc',          'cyan',       32, 10, LuV],
        ['soc',                 'yellow',     16, 20, LuV],
        ['advanced_soc',        'purple',      8, 30, LuV],
        ['highly_advanced_soc',  'white',       4, 40, ZPM],
        ['nand_memory',         'gray',       16, 20, LuV],
        ['nor_memory',          'pink',       16, 20, LuV],
        ['nano_cpu',            'lime',       16, 30, LuV],
        ['qbit_cpu',            'light_gray',  8, 40, ZPM]
    ];

    engravings.forEach(entry => {
        event.recipes.gtceu.laser_engraver('gtcsolo:draconium_silicon/engrave_' + entry[0])
            .itemInputs('gtcsolo:draconium_silicon_wafer')
            // Native color tags also accept the corresponding gemstone lenses.
            .notConsumable('#forge:lenses/' + entry[1])
            .itemOutputs(entry[2] + 'x gtcsolo:draconium_' + entry[0] + '_wafer')
            .duration(entry[3] * 20).EUt(entry[4])
            .cleanroom(CleanroomType.CLEANROOM);
    });
});
