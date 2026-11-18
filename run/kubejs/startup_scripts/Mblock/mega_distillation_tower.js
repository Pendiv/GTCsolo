// GTCEu 1.6.4. The three source blueprints are in run/cmdex/export/.
(function () {
  const RelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
  const Industrial = Java.loadClass('DIV.gtcsolo.util.GtcsoloRecipeModifiers');

  GTCEuStartupEvents.registry('gtceu:machine', event => {
    const DistillationTowerMachine = Java.loadClass('com.gregtechceu.gtceu.common.machine.multiblock.electric.DistillationTowerMachine');
    event.create('mega_distillation_tower', 'multiblock')
      .rotationState(RotationState.NON_Y_AXIS)
      .allowExtendedFacing(false)
      .machine(holder => new DistillationTowerMachine(holder))
      .recipeType('distillation_tower')
      .recipeModifiers(true, [(machine, recipe) => Industrial.industrialOverclock(machine, recipe)])
      .appearanceBlock(() => Block.getBlock('gtceu:clean_machine_casing'))
      .pattern(definition => {
        const maintenance = Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false);
        // /aisle exports X slices, Y rows and Z characters. Transpose each flat
        // blueprint so characters go RIGHT, rows go BACK, and repeated aisles go UP.
        // The core and every output hatch then face out from the same front wall.
        return FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.BACK, RelativeDirection.UP)
          .aisle('AAASAAA', 'AAAAAAA', 'AAAAAAA', 'AAAAAAA', 'AAAAAAA', 'AAAAAAA', 'AAAAAAA')
          .aisle('MMMOMMM', 'M#EEE#M', 'ME###EM', 'ME###EM', 'ME###EM', 'M#EEE#M', 'MMMMMMM')
          .setRepeatable(1, 11)
          .aisle('TTTOTTT', 'TTTTTTT', 'TTTTTTT', 'TTTTTTT', 'TTTTTTT', 'TTTTTTT', 'TTTTTTT')
          .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
          .where('A', Predicates.blocks('gtceu:clean_machine_casing')
            .or(Predicates.abilities(PartAbility.INPUT_ENERGY)
              .setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
            .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1))
            .or(maintenance))
          .where('M', Predicates.blocks('gtceu:clean_machine_casing').or(maintenance))
          .where('E', Predicates.blocks('gtceu:stainless_evaporation_casing'))
          // The ULV hatches in the export mark positions, not a tier restriction.
          .where('O', Predicates.abilities(PartAbility.EXPORT_FLUIDS))
          .where('T', Predicates.blocks('gtceu:clean_machine_casing'))
          .where('#', Predicates.any())
          .build();
      })
      .workableCasingRenderer(
        'gtceu:block/casings/solid/machine_casing_clean_stainless_steel',
        'gtceu:block/multiblock/distillation_tower',
        false)
      .tooltips([Component.translatable('block.gtceu.mega_distillation_tower.tooltip.structure')]);
  });
})();
