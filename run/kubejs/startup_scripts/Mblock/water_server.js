GTCEuStartupEvents.registry('gtceu:machine', event => {
    const WaterServer = Java.loadClass('DIV.gtcsolo.machine.water.WaterServerMachine');
    const Structure = Java.loadClass('DIV.gtcsolo.machine.water.WaterServerStructure');
    event.create('water_server', 'multiblock')
        .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false).allowFlip(false)
        .machine(holder => new WaterServer(holder))
        .appearanceBlock(() => Block.getBlock('gtceu:wood_wall'))
        .pattern(definition => Structure.pattern(definition, 0))
        .shapeInfos(definition => Structure.previews(definition))
        .sidedWorkableCasingRenderer('block/casings/wood_wall', 'gtceu:block/multiblock/primitive_pump')
        .tooltips([Component.translatable('gtcsolo.water_server.flexible'),
            Component.translatable('gtcsolo.jei.introduction.details_hint')]);
});
