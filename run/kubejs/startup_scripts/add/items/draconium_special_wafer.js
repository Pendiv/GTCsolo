// Artwork: Draconium-Special-Wafer-Whole-Glitch-v2-Assets.zip (unchanged).
StartupEvents.registry('item', event => {
    event.create('gtcsolo:draconium_special_wafer')
        .displayName('Draconium-Doped Special Wafer')
        .texture('gtcsolo:item/draconium_special_wafer')
        .maxStackSize(64);
});

StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', event => {
    event.add(['gtcsolo:draconium_special_wafer']);
});
