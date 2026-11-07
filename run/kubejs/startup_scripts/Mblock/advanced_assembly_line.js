(function () {
  const AssemblyLineMachine = Java.loadClass(
    'com.gregtechceu.gtceu.common.machine.multiblock.electric.AssemblyLineMachine'
  );
  const RelativeDirection = Java.loadClass(
    'com.gregtechceu.gtceu.api.pattern.util.RelativeDirection'
  );
  const GtcsoloRecipeModifiers = Java.loadClass(
    'DIV.gtcsolo.util.GtcsoloRecipeModifiers'
  );

  GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('advanced_assembly_line', 'multiblock')
      .rotationState(RotationState.ALL)
      .machine(holder => new AssemblyLineMachine(holder))
      .recipeType('assembly_line')
      .recipeModifiers(true, [
        GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
        (machine, recipe) => GtcsoloRecipeModifiers.advancedAssemblyLine(machine, recipe),
        GTRecipeModifiers.OC_NON_PERFECT_SUBTICK
      ])
      .appearanceBlock(() => Block.getBlock('gtceu:atomic_casing'))
      // Rhino から Java の Component... へは配列として渡す。
      .tooltips([
        Component.translatable('block.gtceu.advanced_assembly_line.tooltip.1'),
        Component.translatable('block.gtceu.advanced_assembly_line.tooltip.2'),
        Component.translatable('block.gtceu.advanced_assembly_line.tooltip.3')
      ])
      .pattern(definition => FactoryBlockPattern
        .start(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
        .aisle('FIF', 'RTR', 'SAG', '#Y#')
        .aisle('FIF', 'RTR', 'DAG', '#Y#').setRepeatable(3, 15)
        .aisle('FOF', 'RTR', 'DAG', '#Y#')
        .where('S', Predicates.controller(Predicates.blocks(definition.get())))
        .where('F', Predicates.blocks('gtceu:atomic_casing')
          .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS)))
        .where('O', Predicates.abilities(PartAbility.EXPORT_ITEMS)
          .addTooltips(Component.translatable('gtceu.multiblock.pattern.location_end')))
        .where('Y', Predicates.blocks('gtceu:atomic_casing')
          .or(Predicates.abilities(PartAbility.INPUT_ENERGY)
            .setMinGlobalLimited(1)
            .setMaxGlobalLimited(2)))
        .where('I', Predicates.blocks('gtceu:ulv_input_bus'))
        .where('G', Predicates.blocks('gtceu:assembly_line_grating'))
        .where('A', Predicates.blocks('gtceu:assembly_line_unit'))
        .where('R', Predicates.blocks('gtceu:laminated_glass'))
        .where('T', Predicates.blocks('gtceu:assembly_line_casing'))
        .where('D', Predicates.dataHatchPredicate(
          Predicates.blocks('gtceu:assembly_line_grating')
        ))
        .where('#', Predicates.any())
        .build()
      )
      .workableCasingRenderer(
        'gtceu:block/casings/gcym/atomic_casing',
        'gtceu:block/multiblock/assembly_line',
        false
      );
  });
})();
