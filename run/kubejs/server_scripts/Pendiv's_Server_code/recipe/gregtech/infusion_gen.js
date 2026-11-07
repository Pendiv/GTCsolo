// M5 phase 2 deferred: infusion_gen は legacy ChemicalBridge 経由の fluid 出力のまま維持.
// Java/KubeJS の登録順 + multiblock pattern 問題があり、phase 2 migration は後続課題.

ServerEvents.recipes(event => {

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_redstone_dust')
        .itemInputs('#forge:dusts/redstone')
        .outputFluids('gtcsolo:infusion_mekanism_redstone 15')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_redstone_enriched')
        .itemInputs('mekanism:enriched_redstone')
        .outputFluids('gtcsolo:infusion_mekanism_redstone 120')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_carbon_coal')
        .itemInputs('minecraft:coal')
        .outputFluids('gtcsolo:infusion_mekanism_carbon 15')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_carbon_charcoal')
        .itemInputs('minecraft:charcoal')
        .outputFluids('gtcsolo:infusion_mekanism_carbon 15')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_carbon_enriched')
        .itemInputs('mekanism:enriched_carbon')
        .outputFluids('gtcsolo:infusion_mekanism_carbon 120')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_tin_dust')
        .itemInputs('#forge:dusts/tin')
        .outputFluids('gtcsolo:infusion_mekanism_tin 15')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_tin_enriched')
        .itemInputs('mekanism:enriched_tin')
        .outputFluids('gtcsolo:infusion_mekanism_tin 120')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_bio_fuel')
        .itemInputs('#forge:fuels/bio')
        .outputFluids('gtcsolo:infusion_mekanism_bio 8')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_fungi_mushrooms')
        .itemInputs('#forge:mushrooms')
        .outputFluids('gtcsolo:infusion_mekanism_fungi 15')
        .duration(40).EUt(-16)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_diamond_dust')
        .itemInputs('#forge:dusts/diamond')
        .outputFluids('gtcsolo:infusion_mekanism_diamond 15')
        .duration(40).EUt(-64)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_diamond_enriched')
        .itemInputs('mekanism:enriched_diamond')
        .outputFluids('gtcsolo:infusion_mekanism_diamond 120')
        .duration(40).EUt(-64)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_gold_dust')
        .itemInputs('#forge:dusts/gold')
        .outputFluids('gtcsolo:infusion_mekanism_gold 15')
        .duration(40).EUt(-64)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_gold_enriched')
        .itemInputs('mekanism:enriched_gold')
        .outputFluids('gtcsolo:infusion_mekanism_gold 120')
        .duration(40).EUt(-64)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_refined_obsidian_dust')
        .itemInputs('#forge:dusts/refined_obsidian')
        .outputFluids('gtcsolo:infusion_mekanism_refined_obsidian 15')
        .duration(40).EUt(-256)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_refined_obsidian_enriched')
        .itemInputs('mekanism:enriched_refined_obsidian')
        .outputFluids('gtcsolo:infusion_mekanism_refined_obsidian 120')
        .duration(40).EUt(-256)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_uranium_dust')
        .itemInputs('#forge:dusts/uranium')
        .outputFluids('gtcsolo:infusion_evolvedmekanism_uranium 15')
        .duration(40).EUt(-4096)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_uranium_enriched')
        .itemInputs('#evolvedmekanism:enriched/uranium')
        .outputFluids('gtcsolo:infusion_evolvedmekanism_uranium 120')
        .duration(40).EUt(-4096)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_better_gold_dust')
        .itemInputs('#forge:dusts/better_gold')
        .outputFluids('gtcsolo:infusion_evolvedmekanism_better_gold 15')
        .duration(40).EUt(-16384)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_better_gold_enriched')
        .itemInputs('#evolvedmekanism:enriched/better_gold')
        .outputFluids('gtcsolo:infusion_evolvedmekanism_better_gold 120')
        .duration(40).EUt(-16384)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_plaslitherite_dust')
        .itemInputs('#forge:dusts/plaslitherite')
        .outputFluids('gtcsolo:infusion_evolvedmekanism_plaslitherite 15')
        .duration(40).EUt(-65536)

    event.recipes.gtceu.infusion_gen('gtcsolo:infusion_gen_plaslitherite_enriched')
        .itemInputs('#evolvedmekanism:enriched/plaslitherite')
        .outputFluids('gtcsolo:infusion_evolvedmekanism_plaslitherite 120')
        .duration(40).EUt(-65536)

})
