// Native research medium; the circuit name does not restrict the data stored on it.
StartupEvents.registry('item', event => {
    const ComponentItem = Java.loadClass('com.gregtechceu.gtceu.api.item.ComponentItem');
    const ItemProperties = Java.loadClass('net.minecraft.world.item.Item$Properties');
    const DataItemBehavior = Java.loadClass('com.gregtechceu.gtceu.common.item.DataItemBehavior');

    event.createCustom('gtcsolo:circuit_research_storage', () => {
        // Manufacturing experience belongs to one physical medium.
        const item = ComponentItem.create(new ItemProperties().stacksTo(1));
        item['attachComponents(com.gregtechceu.gtceu.api.item.component.IItemComponent)'](new DataItemBehavior());
        return item;
    }).displayName('Circuit Data Orb');
});

StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', event => {
    event.add(['gtcsolo:circuit_research_storage']);
});
