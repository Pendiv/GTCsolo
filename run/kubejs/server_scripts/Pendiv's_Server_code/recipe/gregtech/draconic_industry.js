// Draconium's EBF/hot-ingot/Argon/vacuum recipes are generated from its Java Material.
ServerEvents.recipes(event => {
    // Remove direct furnace routes only. Casting, cooling and reversible storage crafting remain available.
    ['minecraft:smelting', 'minecraft:blasting', 'gtceu:electric_furnace'].forEach(type => {
        event.remove({ type: type, output: 'draconicevolution:draconium_ingot' });
    });

    // Four dust units become four ingot-equivalents of molten alloy (4 * 144 mB).
    event.recipes.gtcsolo.extreme_temperature_blast_furnace('draconium_steel')
        .itemInputs('gtceu:trinium_dust', 'gtceu:steel_dust',
            'draconicevolution:draconium_dust', 'gtceu:lutetium_dust')
        .outputFluids(Fluid.of('gtcsolo:draconium_steel', 576))
        .duration(2400)
        .EUt(2097152)
        .blastFurnaceTemp(16000)
        .addData('gtcsolo_ismbf_start_temperature', 12000);

    event.recipes.gtceu.assembler('draconium_coil')
        .itemInputs('8x gtcsolo:draconium_double_wire', '8x gtcsolo:mithrilite_foil')
        .inputFluids(Fluid.of('gtceu:neutronium', 576))
        .itemOutputs('gtcsolo:draconium_coil_block')
        .duration(600)
        .EUt(2097152);
});
