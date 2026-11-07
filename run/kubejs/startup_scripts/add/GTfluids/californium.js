StartupEvents.registry('fluid', event => {
    const Component = Java.loadClass('net.minecraft.network.chat.Component');
    event.create('kubejs:liquid_californium')
        .thinTexture(0xC299FF)
        .bucketColor(0xC299FF)
        .noBlock()
        .displayName(Component.translatable('fluid.kubejs.liquid_californium'));
});
