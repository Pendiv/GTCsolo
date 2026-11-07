ServerEvents.recipes(event => {
    // Replace primitive furnace recipes with pack-specific processing times.

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coal_block' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coal_block')
        .itemInputs('#forge:storage_blocks/iron', '2x #forge:storage_blocks/coal')
        .itemOutputs('gtceu:steel_block', '2x gtceu:dark_ash_dust')
        .duration(12150);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coal_dust_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coal_dust_wrought')
        .itemInputs('#forge:ingots/wrought_iron', '2x #forge:dusts/coal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(600);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coal_gem_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coal_gem_wrought')
        .itemInputs('#forge:ingots/wrought_iron', '2x #forge:gems/coal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(600);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coal_dust' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coal_dust')
        .itemInputs('#forge:ingots/iron', '2x #forge:dusts/coal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(1350);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coke_dust' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coke_dust')
        .itemInputs('#forge:ingots/iron', '#forge:dusts/coke')
        .itemOutputs('gtceu:steel_ingot', 'gtceu:ash_dust')
        .duration(1125);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coke_block_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coke_block_wrought')
        .itemInputs('#forge:storage_blocks/wrought_iron', '#forge:storage_blocks/coke')
        .itemOutputs('gtceu:steel_block', 'gtceu:ash_dust')
        .duration(4050);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coke_block' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coke_block')
        .itemInputs('#forge:storage_blocks/iron', '#forge:storage_blocks/coke')
        .itemOutputs('gtceu:steel_block', 'gtceu:ash_dust')
        .duration(10125);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coke_dust_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coke_dust_wrought')
        .itemInputs('#forge:ingots/wrought_iron', '#forge:dusts/coke')
        .itemOutputs('gtceu:steel_ingot', 'gtceu:ash_dust')
        .duration(450);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coke_gem' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coke_gem')
        .itemInputs('#forge:ingots/iron', '#forge:gems/coke')
        .itemOutputs('gtceu:steel_ingot', 'gtceu:ash_dust')
        .duration(1125);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coal_gem' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coal_gem')
        .itemInputs('#forge:ingots/iron', '2x #forge:gems/coal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(1350);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coal_block_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coal_block_wrought')
        .itemInputs('#forge:storage_blocks/wrought_iron', '2x #forge:storage_blocks/coal')
        .itemOutputs('gtceu:steel_block', '2x gtceu:dark_ash_dust')
        .duration(5400);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_charcoal_gem_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_charcoal_gem_wrought')
        .itemInputs('#forge:ingots/wrought_iron', '2x #forge:gems/charcoal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(600);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_charcoal_gem' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_charcoal_gem')
        .itemInputs('#forge:ingots/iron', '2x #forge:gems/charcoal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(1350);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_charcoal_block' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_charcoal_block')
        .itemInputs('#forge:storage_blocks/iron', '2x #forge:storage_blocks/charcoal')
        .itemOutputs('gtceu:steel_block', '2x gtceu:dark_ash_dust')
        .duration(12150);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_charcoal_dust_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_charcoal_dust_wrought')
        .itemInputs('#forge:ingots/wrought_iron', '2x #forge:dusts/charcoal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(600);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_charcoal_dust' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_charcoal_dust')
        .itemInputs('#forge:ingots/iron', '2x #forge:dusts/charcoal')
        .itemOutputs('gtceu:steel_ingot', '2x gtceu:tiny_dark_ash_dust')
        .duration(1350);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_charcoal_block_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_charcoal_block_wrought')
        .itemInputs('#forge:storage_blocks/wrought_iron', '2x #forge:storage_blocks/charcoal')
        .itemOutputs('gtceu:steel_block', '2x gtceu:dark_ash_dust')
        .duration(5400);

    event.remove({ id: 'gtceu:primitive_blast_furnace/steel_from_coke_gem_wrought' })
    event.recipes.gtceu.primitive_blast_furnace('gtcsolo:pack/primitive_blast_furnace/steel_from_coke_gem_wrought')
        .itemInputs('#forge:ingots/wrought_iron', '#forge:gems/coke')
        .itemOutputs('gtceu:steel_ingot', 'gtceu:ash_dust')
        .duration(450);
});
