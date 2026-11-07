// GTCEu 1.6.4. The three source blueprints are in run/cmdex/export/.
(function () {
  const RelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
  const ParallelLogic = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic');
  const ModifierFunction = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
  const ContentModifier = Java.loadClass('com.gregtechceu.gtceu.api.recipe.content.ContentModifier');
  const RecipeHelper = Java.loadClass('com.gregtechceu.gtceu.api.recipe.RecipeHelper');
  const EURecipeCapability = Java.loadClass('com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability');

  function throughputBonus(machine, recipe) {
    const hatch = machine.getParallelHatch();
    const requestedParallel = hatch.isPresent() ? Math.max(1, Number(hatch.get().getCurrentParallel())) : 1;
    const eut = Number(RecipeHelper.getInputEUt(recipe));
    const powerParallel = eut > 0 ? Math.floor(Number(machine.getOverclockVoltage()) / eut) : requestedParallel;
    const paidLimit = Math.min(requestedParallel, powerParallel);
    if (paidLimit < 1) return ModifierFunction.NULL;

    // DistillationTowerMachine's custom fluid parallel limiter checks each floor's
    // own output tank, rather than pooling capacity from unrelated output floors.
    // GT's generic parallel input check ALSO counts EU/t. Remove it only from a
    // disposable capacity probe, since here one paid parallel covers up to 64 copies.
    const capacityProbe = recipe.copy();
    capacityProbe.tickInputs.remove(EURecipeCapability.CAP);
    const limit = Math.min(2147483647, paidLimit * 64);
    const count = Math.min(limit, ParallelLogic.getParallelAmount(machine, capacityProbe, limit));
    if (count < 1) return ModifierFunction.NULL;
    // Example: 200 copies = 64 + 64 + 64 + 8, charged as 4 paid parallels.
    const paidParallel = Math.ceil(count / 64);
    return ModifierFunction.builder()
      .modifyAllContents(ContentModifier.multiplier(count))
      .eutMultiplier(paidParallel)
      .durationMultiplier(2)
      .parallels(count)
      .build();
  }

  GTCEuStartupEvents.registry('gtceu:machine', event => {
    const DistillationTowerMachine = Java.loadClass('com.gregtechceu.gtceu.common.machine.multiblock.electric.DistillationTowerMachine');
    event.create('mega_distillation_tower', 'multiblock')
      .rotationState(RotationState.NON_Y_AXIS)
      .allowExtendedFacing(false)
      .machine(holder => new DistillationTowerMachine(holder))
      .recipeType('distillation_tower')
      .recipeModifiers(true, [throughputBonus, GTRecipeModifiers.OC_NON_PERFECT_SUBTICK])
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
      .tooltips([
        Component.translatable('block.gtceu.mega_distillation_tower.tooltip.bonus'),
        Component.translatable('block.gtceu.mega_distillation_tower.tooltip.parallel'),
        Component.translatable('block.gtceu.mega_distillation_tower.tooltip.duration'),
        Component.translatable('block.gtceu.mega_distillation_tower.tooltip.height'),
        Component.translatable('block.gtceu.mega_distillation_tower.tooltip.outputs')
      ]);
  });
})();
