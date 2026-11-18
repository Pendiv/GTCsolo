GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:naquadria_bearing_residue')
        .liquid(new GTFluidBuilder()).color(0x454B34);
    event.create('gtcsolo:enriched_naquadah_bearing_residue')
        .liquid(new GTFluidBuilder()).color(0x535835);
});
