// Bedrock dust and Bedrockium dust are distinct inputs.
ServerEvents.recipes(event => {
    const uhv = 2097152;

    event.recipes.gtceu.implosion_compressor('gtcsolo:bedrock')
        .itemInputs('1000x minecraft:iron_ingot', '10000x minecraft:cobblestone', '5x minecraft:tnt')
        .itemOutputs('minecraft:bedrock')
        .duration(40)
        .EUt(30);

    event.recipes.gtceu.macerator('gtcsolo:bedrock_dust')
        .itemInputs('minecraft:bedrock')
        .itemOutputs('gtcsolo:bedrock_dust')
        .duration(100)
        .EUt(uhv);

    // NO_SMELTING disables automatic dust smelting. Also close the dust -> fluid -> ingot bypass,
    // while leaving casting and recycling of already-made Bedrockium parts available.
    event.remove({ id: 'gtceu:extractor/extract_bedrockium_dust' });

    event.recipes.gtcsolo.extreme_temperature_blast_furnace('gtcsolo:bedrockium_from_bedrock')
        .itemInputs('gtcsolo:bedrock_dust')
        .itemOutputs('gtcsolo:bedrockium_ingot')
        .duration(2800)
        .EUt(uhv)
        .blastFurnaceTemp(20000)
        .addData('gtcsolo_ismbf_start_temperature', 12000);

    event.recipes.gtcsolo.extreme_temperature_blast_furnace('gtcsolo:bedrockium_from_bedrockium')
        .itemInputs('gtcsolo:bedrockium_dust')
        .itemOutputs('gtcsolo:bedrockium_ingot')
        .duration(2400)
        .EUt(uhv)
        .blastFurnaceTemp(20000)
        .addData('gtcsolo_ismbf_start_temperature', 12000);
});
