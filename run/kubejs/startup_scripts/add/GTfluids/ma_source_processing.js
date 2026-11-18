GTCEuStartupEvents.registry('gtceu:material', event => {
    event.create('gtcsolo:concentrated_ma_source')
        .liquid(new GTFluidBuilder().temperature(293))
        .color(0xAFE9DA);
    event.create('gtcsolo:mantle_magma')
        .liquid(new GTFluidBuilder().temperature(1500))
        .color(0x9E4127);
    event.create('gtcsolo:deep_mantle_gas')
        .gas(new GTFluidBuilder().temperature(293))
        .color(0xC3C7AD);
    event.create('gtcsolo:spent_pcb_coolant')
        .liquid(new GTFluidBuilder().temperature(310))
        .color(0x2E8276);
});
