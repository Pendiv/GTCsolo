// GTCEu 1.6.4. Structure: run/cmdex/export/multi_ebf.txt.
(function () {
  const CoilMachine = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine');
  const ParallelLogic = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic');
  const ModifierFunction = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
  const ContentModifier = Java.loadClass('com.gregtechceu.gtceu.api.recipe.content.ContentModifier');
  const RecipeHelper = Java.loadClass('com.gregtechceu.gtceu.api.recipe.RecipeHelper');

  // Java.loadClass initializes static fields. Loading GTMultiMachines during
  // startup-script evaluation pulls GTBlocks/GTToolType/GTSoundEntries in before
  // GT's sound registration. Resolve it only when the runtime callbacks run.
  let standardEbf = null;
  function getStandardEbf() {
    if (standardEbf === null) {
      standardEbf = Java.loadClass('com.gregtechceu.gtceu.common.data.machines.GTMultiMachines').ELECTRIC_BLAST_FURNACE;
    }
    return standardEbf;
  }

  function parallelFour(machine, recipe) {
    // As with a standard parallel hatch, inputs, outputs and EU/t all scale.
    // Reduce the count when voltage, input stock or output space is insufficient.
    const eut = Number(RecipeHelper.getInputEUt(recipe));
    const powerLimit = eut > 0 ? Math.floor(Number(machine.getOverclockVoltage()) / eut) : 4;
    const limit = Math.min(4, powerLimit);
    if (limit < 1) return ModifierFunction.NULL;
    const count = Math.min(limit, ParallelLogic.getParallelAmount(machine, recipe, limit));
    if (count < 1) return ModifierFunction.NULL;
    if (count === 1) return ModifierFunction.IDENTITY;
    return ModifierFunction.builder()
      .modifyAllContents(ContentModifier.multiplier(count))
      .eutMultiplier(count)
      .parallels(count)
      .build();
  }

  GTCEuStartupEvents.registry('gtceu:machine', event => {
    event.create('multi_ebf', 'multiblock')
      .rotationState(RotationState.ALL)
      .machine(holder => new CoilMachine(holder))
      .recipeType('electric_blast_furnace')
      // Parallel first, then the original EBF temperature checks, discount and OC.
      // Re-evaluate the parallel count for every new batch.
      .recipeModifiers(true, [parallelFour, (machine, recipe) => GTRecipeModifiers.ebfOverclock(machine, recipe)])
      .appearanceBlock(() => Block.getBlock('gtceu:heatproof_machine_casing'))
      // /aisle exports world axes, not controller facing. Rotate the exported
      // footprint 90 degrees so C is centered in the LAST (front) aisle.
      // FactoryBlockPattern.start() uses LEFT / UP / FRONT; earlier aisles are behind C.
      .pattern(definition => FactoryBlockPattern.start()
        .aisle('AAAAA', 'BBBBB', 'BBBBB', 'AAAAA')
        .aisle('AAAAA', 'B#B#B', 'B#B#B', 'AAAAA')
        .aisle('AAAAA', 'BBBBB', 'BBBBB', 'AADAA')
        .aisle('AAAAA', 'B#B#B', 'B#B#B', 'AAAAA')
        .aisle('AACAA', 'BBBBB', 'BBBBB', 'AAAAA')
        .where('A', Predicates.blocks('gtceu:heatproof_machine_casing')
          // Explicit I/O avoids Rhino selecting the GTRecipeType... overload
          // when booleans are passed to the overloaded autoAbilities methods.
          .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
          .or(Predicates.abilities(PartAbility.INPUT_ENERGY)
            .setMinGlobalLimited(1).setMaxGlobalLimited(4).setPreviewCount(1))
          .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)))
        .where('B', Predicates.heatingCoils())
        .where('C', Predicates.controller(Predicates.blocks(definition.getBlock())))
        .where('D', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
        .where('#', Predicates.any())
        .build())
      // Reuse the standard EBF's muffler recovery and temperature display.
      .recoveryStacks(() => getStandardEbf().getRecoveryItems().get())
      .additionalDisplay((machine, text) => getStandardEbf().getAdditionalDisplay().accept(machine, text))
      .workableCasingRenderer(
        'gtceu:block/casings/solid/machine_casing_heatproof',
        'gtceu:block/multiblock/electric_blast_furnace',
        false)
      .tooltips([
        Component.translatable('gtceu.machine.electric_blast_furnace.tooltip.0'),
        Component.translatable('gtceu.machine.electric_blast_furnace.tooltip.1'),
        Component.translatable('gtceu.machine.electric_blast_furnace.tooltip.2'),
        Component.translatable('block.gtceu.multi_ebf.tooltip.parallel'),
        Component.translatable('block.gtceu.multi_ebf.tooltip.energy'),
        Component.translatable('block.gtceu.multi_ebf.tooltip.hatches')
      ]);
  });
})();
