ServerEvents.recipes(event => {
    const gt = event.recipes.gtceu;
    const fluid = (id, amount) => Fluid.of(id, amount);

    if (Item.exists('gtceu:tiny_ytterbium_dust')) {
        // Separate another heavy rare-earth fraction from the existing xenotime concentrate.
        gt.large_chemical_reactor('gtcsolo:structural_rare_alloys/recover_ytterbium')
            .itemInputs('4x gtcsolo:heavy_rare_earth_concentrate_dust', 'gtceu:calcium_dust')
            .inputFluids(fluid('gtceu:hydrofluoric_acid', 2000))
            .itemOutputs('2x gtceu:tiny_ytterbium_dust', 'gtcsolo:calcium_fluoride_dust')
            .outputFluids(fluid('minecraft:water', 1000)).duration(320).EUt(7680);
    }

    if (Item.exists('gtceu:precision_blast_furnace') &&
        Fluid.exists('gtcsolo:yttrium_refractory_high_entropy_alloy')) {
        // AlMo0.5NbTa0.5TiZr with approximately 0.55 at.% Y in GT material units.
        gt.precision_blast_furnace('gtcsolo:structural_rare_alloys/refractory_high_entropy_alloy')
            .itemInputs('4x gtceu:aluminium_dust', '2x gtceu:molybdenum_dust', '4x gtceu:niobium_dust',
                '2x gtceu:tantalum_dust', '4x gtceu:titanium_dust', '4x gtceu:zirconium_dust',
                'gtceu:tiny_yttrium_dust')
            .inputFluids(fluid('gtceu:argon', 250))
            .outputFluids(fluid('gtcsolo:yttrium_refractory_high_entropy_alloy', 2896))
            .blastFurnaceTemp(2700).duration(800).EUt(7680);
    }

    // Silicon diffusion coating is a separate option; the original alloy forms stay available.
    if (Item.exists('gtcsolo:yttrium_refractory_high_entropy_alloy_plate') &&
        Item.exists('kubejs:yttrium_refractory_silicide_plate')) {
        gt.electric_blast_furnace('gtcsolo:structural_rare_alloys/refractory_siliconizing')
            .itemInputs('gtcsolo:yttrium_refractory_high_entropy_alloy_plate', 'gtceu:tiny_silicon_dust')
            .inputFluids(fluid('gtceu:argon', 250))
            .itemOutputs('kubejs:yttrium_refractory_silicide_plate')
            .blastFurnaceTemp(1573).duration(600).EUt(1920);
    }

    if (Item.exists('gtceu:precision_blast_furnace') && Fluid.exists('gtceu:ytterbium') &&
        Fluid.exists('gtceu:zirconium') && Fluid.exists('gtcsolo:as_cast_aluminium_yb_zr')) {
        // 23 Al units plus 1 mB Yb and 2 mB Zr approximate 0.03 at.% / 0.06 at.%.
        gt.precision_blast_furnace('gtcsolo:structural_rare_alloys/aluminium_yb_zr_melting')
            .itemInputs('23x gtceu:aluminium_dust')
            .inputFluids(fluid('gtceu:ytterbium', 1), fluid('gtceu:zirconium', 2), fluid('gtceu:argon', 250))
            .outputFluids(fluid('gtcsolo:as_cast_aluminium_yb_zr', 3315))
            .blastFurnaceTemp(1200).duration(400).EUt(1920);
    }

    if (Item.exists('gtcsolo:as_cast_aluminium_yb_zr_ingot') &&
        Item.exists('gtcsolo:aged_aluminium_yb_zr_ingot')) {
        // The native solidifier casts the unaged alloy; age the ingot after solidification.
        gt.electric_blast_furnace('gtcsolo:structural_rare_alloys/aluminium_yb_zr_aging')
            .itemInputs('gtcsolo:as_cast_aluminium_yb_zr_ingot')
            .itemOutputs('gtcsolo:aged_aluminium_yb_zr_ingot')
            .blastFurnaceTemp(673).duration(200).EUt(480);

        gt.electric_blast_furnace('gtcsolo:structural_rare_alloys/aluminium_yb_zr_dust_recovery')
            .itemInputs('gtcsolo:aged_aluminium_yb_zr_dust')
            .itemOutputs('gtcsolo:aged_aluminium_yb_zr_ingot')
            .blastFurnaceTemp(673).duration(200).EUt(480);
    }

    if (Item.exists('gtcsolo:heat_exchange_casing') && Item.exists('gtcsolo:aged_aluminium_yb_zr_frame')) {
        var Config = Java.loadClass('com.gregtechceu.gtceu.config.ConfigHolder');
        gt.assembler('gtcsolo:structural_rare_alloys/heat_exchange_casing')
            .itemInputs('4x gtcsolo:cucr1zr_plate', '2x gtcsolo:cucr1zr_rod',
                'gtcsolo:aged_aluminium_yb_zr_frame', '2x gtcsolo:rilun_foil')
            .circuit(6).inputFluids(fluid('gtcsolo:galinstan', 144))
            .itemOutputs(Item.of('gtcsolo:heat_exchange_casing', Config.INSTANCE.recipes.casingsPerCraft))
            .duration(50).EUt(16);
    }

    if (Item.exists('gtcsolo:nimonic_80a_heatproof_casing') &&
        Item.exists('kubejs:yttrium_refractory_silicide_plate')) {
        var NimonicConfig = Java.loadClass('com.gregtechceu.gtceu.config.ConfigHolder');
        gt.assembler('gtcsolo:structural_rare_alloys/nimonic_heatproof_casing')
            .itemInputs('4x gtcsolo:nimonic_80a_plate', 'gtcsolo:nimonic_80a_frame',
                'kubejs:yttrium_refractory_silicide_plate')
            .circuit(6)
            .itemOutputs(Item.of('gtcsolo:nimonic_80a_heatproof_casing', NimonicConfig.INSTANCE.recipes.casingsPerCraft))
            .duration(50).EUt(16);
    }
});
