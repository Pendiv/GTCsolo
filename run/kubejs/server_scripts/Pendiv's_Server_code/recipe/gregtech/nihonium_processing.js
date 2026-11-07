// Reference route: diethylzinc centrifuge enrichment -> recovered 70Zn -> 209Bi(70Zn,n)278Nh.
// Amounts, cascade stages, reactor choice, stored metal lifetime and process rates are game abstractions.
// https://doi.org/10.1016/j.nima.2005.12.227
// https://www.riken.jp/press/2012/20120927/
ServerEvents.recipes(event => {
    if (!Fluid.exists('gtcsolo:diethylzinc') || !Fluid.exists('gtceu:nihonium')) {
        console.warn('[Nihonium processing] Restart Minecraft to register the new material forms.');
        return;
    }
    const IV = 7680;
    const ZPM = 122880;
    const UV = 491520;
    const UHV = 2097152;
    const fluid = (name, amount) => Fluid.of('gtcsolo:' + name, amount);

    // Acid-catalysed ethanol dehydration. Native acid distillation recovers the diluted acid separately.
    event.recipes.gtceu.chemical_reactor('gtcsolo:nihonium/diethyl_ether')
        .inputFluids(Fluid.of('gtceu:ethanol', 2000), Fluid.of('gtceu:sulfuric_acid', 2000))
        .outputFluids(fluid('diethyl_ether', 1000), Fluid.of('gtceu:diluted_sulfuric_acid', 3000))
        .duration(1200).EUt(IV);

    // Hydrogen iodide already has a native brine-production route.
    event.recipes.gtceu.chemical_reactor('gtcsolo:nihonium/ethyl_iodide')
        .inputFluids(Fluid.of('gtceu:ethanol', 1000), Fluid.of('gtceu:hydrogen_iodide', 1000))
        .outputFluids(fluid('ethyl_iodide', 1000), Fluid.of('minecraft:water', 1000))
        .duration(800).EUt(IV);

    // 2 Zn + 2 C2H5I -> Zn(C2H5)2 + ZnI2, in a recoverable ether carrier and an inert purge.
    event.recipes.gtceu.chemical_reactor('gtcsolo:nihonium/crude_diethylzinc')
        .itemInputs('32x gtceu:zinc_dust')
        .inputFluids(fluid('ethyl_iodide', 32000), fluid('diethyl_ether', 16000), Fluid.of('gtceu:argon', 8000))
        .itemOutputs('48x gtcsolo:zinc_iodide_dust')
        .outputFluids(fluid('crude_diethylzinc', 32000))
        .duration(2400).EUt(ZPM);

    event.recipes.gtceu.distillation_tower('gtcsolo:nihonium/diethylzinc_purification')
        .inputFluids(fluid('crude_diethylzinc', 32000))
        .outputFluids(fluid('diethylzinc', 16000), fluid('diethyl_ether', 16000))
        .disableDistilleryRecipes(true).duration(1200).EUt(ZPM);

    event.recipes.gtceu.electrolyzer('gtcsolo:nihonium/zinc_iodide_recovery')
        .itemInputs('3x gtcsolo:zinc_iodide_dust')
        // Match native brine processing: 2000 mB hydrogen iodide corresponds to one iodine-dust output.
        .itemOutputs('gtceu:zinc_dust', 'gtceu:iodine_dust')
        .duration(400).EUt(IV);

    event.recipes.gtceu.chemical_reactor('gtcsolo:nihonium/hydrogen_iodide_recovery')
        .itemInputs('gtceu:iodine_dust')
        .notConsumable('gtceu:platinum_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 2000))
        .outputFluids(Fluid.of('gtceu:hydrogen_iodide', 2000))
        .duration(400).EUt(IV);

    // Fractions are 0.61%, 5%, 25%, 99%, and an idealised 0% tail.
    // Both total feed and zinc-70 inventory balance in each stage; the two return streams must go upstream.
    event.recipes.gtceu.centrifuge('gtcsolo:nihonium/zinc_70_enrichment_1')
        .inputFluids(fluid('diethylzinc', 10000))
        .outputFluids(fluid('zinc_70_low_enriched_diethylzinc', 1220), fluid('zinc_70_depleted_diethylzinc', 8780))
        .duration(1200).EUt(ZPM);
    event.recipes.gtceu.centrifuge('gtcsolo:nihonium/zinc_70_enrichment_2')
        .inputFluids(fluid('zinc_70_low_enriched_diethylzinc', 2439))
        .outputFluids(fluid('zinc_70_enriched_diethylzinc', 439), fluid('diethylzinc', 2000))
        .duration(1800).EUt(UV);
    event.recipes.gtceu.centrifuge('gtcsolo:nihonium/zinc_70_enrichment_3')
        .inputFluids(fluid('zinc_70_enriched_diethylzinc', 4700))
        .outputFluids(fluid('zinc_70_high_enriched_diethylzinc', 1000), fluid('zinc_70_low_enriched_diethylzinc', 3700))
        .duration(2400).EUt(UV);

    // CVD-style metal recovery: organic coproducts are an atom-balanced gameplay representation.
    // 100 feed units at 99% -> 99 enriched metal units + 1 depleted unit; no isotope is created by purification.
    // The bulk input/output amounts rely on actual machine storage capacity, not an explicit multiblock condition.
    event.recipes.gtceu.chemical_reactor('gtcsolo:nihonium/zinc_70_metal_recovery')
        .inputFluids(fluid('zinc_70_high_enriched_diethylzinc', 100000))
        .itemOutputs('99x gtcsolo:zinc_70_dust', 'gtcsolo:zinc_70_depleted_dust')
        .outputFluids(Fluid.of('gtceu:ethylene', 100000), Fluid.of('gtceu:ethane', 100000))
        .duration(1800).EUt(UV);
    event.recipes.gtceu.chemical_reactor('gtcsolo:nihonium/depleted_zinc_metal_recovery')
        .inputFluids(fluid('zinc_70_depleted_diethylzinc', 1000))
        .itemOutputs('gtcsolo:zinc_70_depleted_dust')
        .outputFluids(Fluid.of('gtceu:ethylene', 1000), Fluid.of('gtceu:ethane', 1000))
        .duration(400).EUt(IV);

    // RIKEN reaction: 70Zn + 209Bi -> 278Nh + n. Bi isotope and the emitted neutron are abstracted.
    // The native material stores generic Nh, not a literal millisecond-lived 278Nh sample.
    // Heavy-ion acceleration is represented by the existing UHV fusion reactor pending any dedicated future structure.
    event.recipes.gtceu.fusion_reactor('gtcsolo:nihonium/zinc_70_bismuth_fusion')
        .inputFluids(fluid('zinc_70', 16), Fluid.of('gtceu:bismuth', 16))
        .outputFluids(Fluid.of('gtceu:nihonium', 16))
        .duration(2400).EUt(UHV).fusionStartEU(1280000000);
});
