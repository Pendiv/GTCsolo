// 水/マグマ バケツの遠心分離 → 空バケツ + gtcsolo:water/magma アイテム (LV 32EU/t, 20tick)
ServerEvents.recipes(event => {
    event.recipes.gtceu.centrifuge('gtcsolo:water_bucket_extract')
        .itemInputs('minecraft:water_bucket')
        .itemOutputs('minecraft:bucket', 'gtcsolo:water')
        .duration(20)
        .EUt(32);

    event.recipes.gtceu.centrifuge('gtcsolo:lava_bucket_extract')
        .itemInputs('minecraft:lava_bucket')
        .itemOutputs('minecraft:bucket', 'gtcsolo:magma')
        .duration(20)
        .EUt(32);
});
