(function () {
  GTCEuStartupEvents.registry("gtceu:material", event => {
    event.create("gtceu:new_gt_rothoy")
      .gem(2, 0)
      .liquid(new GTFluidBuilder().temperature(1300))
      .ore(1, 1, false)
      .color(0x8AE16B)
      .secondaryColor(0x738226)
      .iconSet(GTMaterialIconSet.getByName("ruby"))
      .formula("Ro")
      .flags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.NO_SMELTING, GTMaterialFlags.DISABLE_ALLOY_BLAST, GTMaterialFlags.GENERATE_LENS, GTMaterialFlags.CRYSTALLIZABLE, GTMaterialFlags.HIGH_SIFTER_OUTPUT, GTMaterialFlags.MORTAR_GRINDABLE, GTMaterialFlags.GENERATE_DENSE, GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_LONG_ROD, GTMaterialFlags.GENERATE_BOLT_SCREW)
      .ignoredTagPrefixes(TagPrefix.ingot, TagPrefix.nugget, TagPrefix.plateDouble, TagPrefix.get("bigPlate"), TagPrefix.toolHeadBuzzSaw)
      .toolStats(ToolProperty.Builder.of(6, 2, 500, 2).types(GTToolType.SWORD, GTToolType.PICKAXE, GTToolType.SHOVEL, GTToolType.AXE, GTToolType.HOE).enchantability(14).attackSpeed(0).durabilityMultiplier(1).build());
  });
})();
