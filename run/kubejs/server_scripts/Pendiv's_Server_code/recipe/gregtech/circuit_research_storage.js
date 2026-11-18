ServerEvents.recipes(event => {
    // GT's Java recipe builder adds these two solder options. KubeJS needs explicit fluid inputs.
    const storageRecipe = (id, fluid, amount) => {
        event.recipes.gtceu.circuit_assembler(id)
            .itemInputs(
                'gtcsolo:draconium_special_wafer',
                'gtceu:wetware_circuit_board',
                '16x gtcsolo:draconium_ram_chip',
                '32x gtcsolo:draconium_nor_memory_chip',
                '64x gtcsolo:draconium_nand_memory_chip',
                '32x gtceu:fine_yttrium_barium_cuprate_wire'
            )
            .inputFluids(Fluid.of(fluid, amount))
            .itemOutputs('gtcsolo:circuit_research_storage')
            .duration(40 * 20).EUt(491520)
            .cleanroom(CleanroomType.STERILE_CLEANROOM);
    };
    storageRecipe('gtcsolo:circuit_research_storage', 'gtceu:soldering_alloy', 288);
    storageRecipe('gtcsolo:circuit_research_storage_tin', 'gtceu:tin', 576);

    // Like the native data orb, crafting the medium alone clears its recorded data.
    event.shapeless('gtcsolo:circuit_research_storage', ['gtcsolo:circuit_research_storage'])
        .id('gtcsolo:circuit_research_storage_clear');
});
