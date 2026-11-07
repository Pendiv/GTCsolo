(function () {
  GTCEuStartupEvents.registry('gtceu:machine', event => {
    const PrototypeAssemblerMachine = Java.loadClass('DIV.gtcsolo.machine.PrototypeAssemblerMachine');
    event.create('prototype_assembler', 'multiblock')
      .rotationState(RotationState.NON_Y_AXIS)
      .machine(holder => new PrototypeAssemblerMachine(holder))
      .recipeType('assembler')
      // Step-up recipes can draw stored energy faster than low-tier hatches refill it.
      // Keep paid progress while recharging instead of damping it back to the first tick.
      .regressWhenWaiting(false)
      .recipeModifiers(true, [(machine, recipe) => PrototypeAssemblerMachine.prototypeOverclock(machine, recipe)])
      .appearanceBlock(() => Block.getBlock('gtceu:large_scale_assembler_casing'))
      // Saved world-axis slices are reversed so the core/glass wall is the front.
      .pattern(definition => FactoryBlockPattern.start()
        .aisle('AAAAA', 'AAAAA', 'AAAAA', 'AAAAA', 'AAAAA')
        .aisle('AAAAA', 'ADEDA', 'A###A', 'ADEDA', 'AAAAA')
        .aisle('AAAAA', 'AE#EA', 'A###A', 'AE#EA', 'AAAAA')
        .aisle('AAAAA', 'ADEDA', 'A###A', 'ADEDA', 'AAAAA')
        .aisle('AABAA', 'ACCCA', 'ACCCA', 'ACCCA', 'AAAAA')
        .where('A', Predicates.blocks('gtceu:large_scale_assembler_casing')
          .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
          .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)))
        .where('B', Predicates.controller(Predicates.blocks(definition.getBlock())))
        .where('C', Predicates.blocks('gtceu:laminated_glass'))
        .where('D', Predicates.blocks('gtceu:tungstensteel_pipe_casing'))
        .where('E', Predicates.blocks('gtceu:cupronickel_coil_block'))
        .where('#', Predicates.any())
        .build())
      .workableCasingRenderer(
        'gtceu:block/casings/gcym/large_scale_assembling_casing',
        'gtceu:block/machines/assembler', false)
      .tooltips([
        Component.translatable('block.gtceu.prototype_assembler.tooltip.voltage'),
        Component.translatable('block.gtceu.prototype_assembler.tooltip.retry'),
        Component.translatable('block.gtceu.prototype_assembler.tooltip.chance'),
        Component.translatable('block.gtceu.prototype_assembler.tooltip.oc')
      ]);
  });
})();
