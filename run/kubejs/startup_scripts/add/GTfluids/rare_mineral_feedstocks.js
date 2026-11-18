GTCEuStartupEvents.registry('gtceu:material', event => {
    const dust = (id, color) => event.create('gtcsolo:' + id).dust().color(color)
        .iconSet(GTMaterialIconSet.DULL)
        .flags(GTMaterialFlags.DISABLE_DECOMPOSITION, GTMaterialFlags.NO_SMELTING);
    dust('xenotime', 0xA08A64).ore(1, 1, false).formula('YPO4');
    dust('scandium_bearing_baddeleyite', 0x51474D).ore(1, 1, false).formula('ZrO2');
    dust('thallium_bearing_orpiment', 0xC89A31).ore(1, 1, false).formula('As2S3');
    dust('yttrium_oxide', 0xECE9DC).formula('Y2O3');
    dust('heavy_rare_earth_concentrate', 0x826984);
    dust('zirconium_tetrachloride', 0xDEDECF).formula('ZrCl4');
    dust('scandium_refining_residue', 0x877D65);
    dust('arsenic_trioxide', 0xDFDAD5).formula('As2O3');
    dust('thallium_roasting_residue', 0x836F70);
    event.create('gtcsolo:xenotime_sulfate_leachate').liquid(new GTFluidBuilder().temperature(320)).color(0xA89A53);
});

GTCEuStartupEvents.materialModification(() => {
    const PropertyKey = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey');
    const DustProperty = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty');
    [GTMaterials.Terbium, GTMaterials.Thallium].forEach(material => {
        if (!material.hasProperty(PropertyKey.DUST)) material.setProperty(PropertyKey.DUST, new DustProperty());
    });
});
