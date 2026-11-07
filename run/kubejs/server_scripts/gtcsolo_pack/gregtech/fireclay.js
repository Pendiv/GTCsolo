ServerEvents.recipes(event => {
    // Use the pack-specific fireclay and firebrick recipes.

    event.remove({ id: 'gtceu:centrifuge/decomposition_centrifuging__fireclay' })

    event.remove({ id: 'gtceu:shapeless/fireclay_dust' })
    event.shapeless(
        Item.of('gtceu:fireclay_dust', 4),
        ['#forge:dusts/brick', '#forge:dusts/clay']
    ).id('gtcsolo:pack/fireclay_dust');

    event.remove({ id: 'gtceu:shaped/casing_primitive_bricks' })
    event.shaped(
        'gtceu:firebricks',
        ['AAA', 'AAA', 'AAA'],
        {A: 'gtceu:firebrick'}
    ).id('gtcsolo:pack/firebricks');
});
