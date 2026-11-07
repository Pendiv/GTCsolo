// Combustion support definitions. Larger EU/mB values require less material for the same output.
ServerEvents.recipes(function (event) {
    var ForgeRegistries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries');
    var ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');
    var MekanismAPI = Java.loadClass('mekanism.api.MekanismAPI');
    if (!ForgeRegistries.RECIPE_SERIALIZERS.containsKey(ResourceLocation.tryParse('gtcsolo:combustion_support'))) return;
    var supports = [
        {id: 'oxygen_gas', gas: 'mekanism:oxygen', eu_per_mb: 256},
        {id: 'oxygen_fluid', fluid: 'gtceu:oxygen', eu_per_mb: 256},
        {id: 'oxygen_steam_gas', gas: 'gtcsolo:oxygen_steam_mixture', eu_per_mb: 8192},
        {id: 'oxygen_steam_fluid', fluid: 'gtcsolo:oxygen_steam_mixture', eu_per_mb: 8192}
    ];
    supports.forEach(function (support) {
        var gas = support.gas !== undefined;
        var material = ResourceLocation.tryParse(gas ? support.gas : support.fluid);
        if (gas ? !MekanismAPI.gasRegistry().containsKey(material) : !Fluid.exists(support.fluid)) return;
        var definition = {type: 'gtcsolo:combustion_support', eu_per_mb: support.eu_per_mb};
        if (gas) definition.gas = support.gas;
        else definition.fluid = support.fluid;
        event.custom(definition).id('gtcsolo:combustion_support/' + support.id);
    });
});
