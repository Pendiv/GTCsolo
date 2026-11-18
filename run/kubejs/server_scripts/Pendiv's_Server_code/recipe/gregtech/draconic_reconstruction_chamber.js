ServerEvents.recipes(event => {
    if (!Item.exists('gtceu:draconic_reconstruction_chamber')) return;

    const gt = event.recipes.gtceu;
    const uhv = GTValues.VA[GTValues.UHV];
    const casingCount = Java.loadClass('com.gregtechceu.gtceu.config.ConfigHolder').INSTANCE.recipes.casingsPerCraft;

    gt.assembler('gtcsolo:draconic_steel_casing')
        .itemInputs('gtcsolo:draconium_steel_frame', '6x gtcsolo:draconium_steel_plate')
        .circuit(6)
        .itemOutputs(Item.of('gtcsolo:draconic_steel_casing', casingCount))
        .duration(100).EUt(GTValues.VA[GTValues.UV]);

    gt.assembly_line('gtcsolo:draconic_reconstruction_chamber')
        .itemInputs('gtceu:uhv_machine_hull', '8x gtcsolo:draconic_steel_casing',
            '4x gtceu:high_power_casing', '4x gtcsolo:draconium_coil_block',
            '2x draconicevolution:awakened_draconium_ingot', '4x #gtceu:circuits/uhv',
            '2x gtceu:uv_field_generator', '2x gtceu:uv_electric_pump')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2304), Fluid.of('gtceu:polybenzimidazole', 1152))
        .itemOutputs('gtceu:draconic_reconstruction_chamber')
        .stationResearch(research => research
            .researchId('gtcsolo_draconic_reconstruction_chamber_unlock')
            .researchStack(Item.of('gtceu:high_power_casing'))
            .CWUt(128).EUt(GTValues.VA[GTValues.UV]))
        .duration(1200).EUt(uhv);
});
