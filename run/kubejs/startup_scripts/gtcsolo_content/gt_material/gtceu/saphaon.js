(function () {
  GTCEuStartupEvents.registry("gtceu:material", event => {
    event.create("gtceu:saphaon")
      .dust(4, 0)
      .polymer(4)
      .ingot(4, 0)
      .liquid(new GTFluidBuilder().temperature(1300))
      .gas(new GTFluidBuilder().temperature(3000))
      .ore(4, 1, true)
      .color(0x72F3D3)
      .secondaryColor(0xA0CFFF)
      .iconSet(GTMaterialIconSet.getByName("metallic"))
      .formula("∽SF")
      .flags(GTMaterialFlags.GENERATE_PLATE, GTMaterialFlags.DISABLE_ALLOY_BLAST, GTMaterialFlags.GENERATE_DENSE, GTMaterialFlags.GENERATE_FOIL, GTMaterialFlags.GENERATE_RING, GTMaterialFlags.GENERATE_FRAME, GTMaterialFlags.GENERATE_ROTOR, GTMaterialFlags.GENERATE_ROUND, GTMaterialFlags.GENERATE_SMALL_GEAR, GTMaterialFlags.GENERATE_ROD, GTMaterialFlags.GENERATE_LONG_ROD, GTMaterialFlags.GENERATE_BOLT_SCREW, GTMaterialFlags.NO_SMASHING, GTMaterialFlags.FLAMMABLE, GTMaterialFlags.DISABLE_DECOMPOSITION)
      .ignoredTagPrefixes(TagPrefix.toolHeadWireCutter)
      .blastTemp(2700)
      .cableProperties(Java.loadClass("java.lang.Long").parseLong("8388608"), 8, 1, false, 0)
      .fluidPipeProperties(180002, 2000000, true, true, true, true)
      .rotorStats(7, 1200, 1, 10000000)
      .toolStats(ToolProperty.Builder.of(99, 2000, 2147483647, 11).types(GTToolType.SWORD, GTToolType.PICKAXE, GTToolType.SHOVEL, GTToolType.AXE, GTToolType.HOE, GTToolType.BUZZSAW, GTToolType.DRILL_LV, GTToolType.MINING_HAMMER, GTToolType.SCYTHE, GTToolType.SAW, GTToolType.SCREWDRIVER_LV, GTToolType.WRENCH_IV, GTToolType.CHAINSAW_LV, GTToolType.DRILL_HV, GTToolType.SHEARS, GTToolType.PLUNGER, GTToolType.DRILL_MV, GTToolType.BUTCHERY_KNIFE, GTToolType.MORTAR, GTToolType.FILE, GTToolType.WIRE_CUTTER, GTToolType.CROWBAR, GTToolType.SOFT_MALLET, GTToolType.WRENCH, GTToolType.KNIFE, GTToolType.SCREWDRIVER, GTToolType.HARD_HAMMER, GTToolType.SPADE, GTToolType.DRILL_EV, GTToolType.DRILL_IV, GTToolType.WRENCH_HV, GTToolType.WRENCH_LV).enchantability(14).attackSpeed(0).durabilityMultiplier(1).build())
      .addOreByproducts(GTMaterials.get("gtceu:trinium"));
  });
})();
