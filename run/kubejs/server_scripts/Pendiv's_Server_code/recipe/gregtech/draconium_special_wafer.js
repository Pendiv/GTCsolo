ServerEvents.recipes(event => {
    event.recipes.gtceu.laser_engraver('gtcsolo:draconium_silicon/special_wafer')
        .itemInputs('gtcsolo:draconium_silicon_wafer')
        .notConsumable('gtcsolo:purified_chi_lens')
        .itemOutputs('gtcsolo:draconium_special_wafer')
        .duration(40 * 20)
        .EUt(491520)
        .cleanroom(CleanroomType.STERILE_CLEANROOM);
});
