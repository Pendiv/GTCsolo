GTCEuStartupEvents.registry('gtceu:element', event => {
    event.create('gtcsolo:calcium_48').protons(20).neutrons(28).symbol('Ca-48').isIsotope(true);
    event.create('gtcsolo:californium_249').protons(98).neutrons(151).symbol('Cf-249').isIsotope(true);
});

GTCEuStartupEvents.registry('gtceu:material', event => {
    const fluid = (name, color) => event.create('gtcsolo:' + name).color(color)
        .iconSet(GTMaterialIconSet.FLUID).flags(GTMaterialFlags.DISABLE_DECOMPOSITION);

    fluid('calcium_vapor', 0xEEE8CB).gas(new GTFluidBuilder().temperature(1800)).formula('Ca');
    fluid('calcium_48_enriched_vapor', 0xF4C990).gas(new GTFluidBuilder().temperature(1800)).formula('Ca');
    fluid('calcium_48_depleted', 0xB4A88D).liquid(new GTFluidBuilder().temperature(1150)).formula('Ca');
    fluid('calcium_48', 0xFFF0CE).liquid(new GTFluidBuilder().temperature(1150)).element('calcium_48');

    fluid('californium_249_concentrate', 0xDDA8EC).liquid().formula('Cf');
    fluid('californium_249_depleted', 0xA08BC2).liquid().formula('Cf');
    fluid('californium_249_plating_solution', 0xEDBCD8).liquid().formula('Cf(NO3)3');
    fluid('californium_249', 0xF0CEFF).liquid().element('californium_249');
});

StartupEvents.registry('item', event => {
    event.create('gtcsolo:californium_249_target')
        .texture('gtceu:item/material_sets/shiny/foil').color(0, 0xCFB4DB);
});

StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', event => {
    event.add(['gtcsolo:californium_249_target']);
});
