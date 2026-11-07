ServerEvents.recipes(event => {
    // Six inputs, following the native data-orb/data-module layout with a specialized storage wafer.
    event.recipes.gtceu.circuit_assembler('gtcsolo:circuit_research_storage')
        .itemInputs(
            'gtcsolo:draconium_special_wafer',
            'gtceu:wetware_circuit_board',
            '16x gtcsolo:draconium_ram_chip',
            '32x gtcsolo:draconium_nor_memory_chip',
            '64x gtcsolo:draconium_nand_memory_chip',
            '32x gtceu:fine_yttrium_barium_cuprate_wire'
        )
        .itemOutputs('gtcsolo:circuit_research_storage')
        // Native alternatives: soldering alloy 288 mB or tin 576 mB.
        .solderMultiplier(4)
        .duration(40 * 20).EUt(491520)
        .cleanroom(CleanroomType.STERILE_CLEANROOM);

    // Like the native data orb, crafting the medium alone clears its recorded data.
    event.shapeless('gtcsolo:circuit_research_storage', ['gtcsolo:circuit_research_storage'])
        .id('gtcsolo:circuit_research_storage_clear');
});
