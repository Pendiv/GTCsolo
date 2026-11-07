ServerEvents.recipes(event => {
    if (!Fluid.exists('gtcsolo:chi_molten_magma')) {
        console.warn('[Ancient metal] Restart Minecraft to register the new fluids before loading these recipes.');
        return;
    }
    event.recipes.gtceu.extractor('gtcsolo:ancient_debris_liquefaction')
        .itemInputs('minecraft:ancient_debris')
        .outputFluids(Fluid.of('gtcsolo:liquid_ancient_debris', 144))
        .duration(320)
        .EUt(32);

    // 96 B = 96,000 mB. This batch needs a multiblock mixer with sufficient fluid storage.
    event.recipes.gtceu.mixer('gtcsolo:molten_ancient_metal_slurry')
        .itemInputs('64x minecraft:netherrack')
        .inputFluids(
            Fluid.of('gtcsolo:liquid_ancient_debris', 144),
            Fluid.of('minecraft:lava', 96000))
        .outputFluids(Fluid.of('gtcsolo:molten_ancient_metal_slurry', 576))
        .duration(200)
        .EUt(32);

    // Output order is the native tower's bottom-to-top hatch order.
    event.recipes.gtceu.distillation_tower('gtcsolo:ancient_metal_fractionation')
        .inputFluids(Fluid.of('gtcsolo:molten_ancient_metal_slurry', 144))
        .itemOutputs('minecraft:netherite_scrap')
        .outputFluids(
            Fluid.of('minecraft:lava', 24000),
            Fluid.of('gtcsolo:stone_slurry', 144),
            Fluid.of('gtceu:gold', 36),
            Fluid.of('gtceu:sculk', 144),
            Fluid.of('gtcsolo:chi_molten_magma', 36))
        .duration(200)
        .EUt(524288)
        .disableDistilleryRecipes(true);
});
