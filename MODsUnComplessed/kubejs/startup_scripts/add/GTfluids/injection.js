GTCEuStartupEvents.registry("gtceu:material", event => {

    const injectionFluids = [
        ["refined_obsidian",0x7A1FCF ],
        ["diamond", 0x63CFCF],
        ["redstone",0xC81E1E ],
        ["better_gold", 0xF2C230],
        ["uran", 0xA8D870],
        ["plaslitherite",0x6E6E6E ]
    ];

    injectionFluids.forEach(([name, color]) => {

        event.create("injection_" + name)
            .fluid()
            .color(color)
            .iconSet(GTMaterialIconSet.FLUID)
            .flags(GTMaterialFlags.DISABLE_DECOMPOSITION);

    });

});