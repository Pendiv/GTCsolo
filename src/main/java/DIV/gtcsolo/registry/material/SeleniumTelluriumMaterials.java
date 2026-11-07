package DIV.gtcsolo.registry.material;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.resources.ResourceLocation;

/** Real minerals plus the simplified oxidative copper-residue recovery chain. */
public final class SeleniumTelluriumMaterials {
    public static Material CLAUSTHALITE, CALAVERITE, SELENIUM_TELLURIUM_RESIDUE,
            SELENIUM_DIOXIDE, TELLURIUM_DIOXIDE;

    private SeleniumTelluriumMaterials() {}

    public static void init() {
        // Standard GT forms, textures and component-averaged colors; no custom ore artwork.
        CLAUSTHALITE = material("clausthalite").dust().ore()
                .components(GTMaterials.Lead, 1, GTMaterials.Selenium, 1)
                .flags(MaterialFlags.DECOMPOSITION_BY_ELECTROLYZING)
                .colorAverage().buildAndRegister();
        CLAUSTHALITE.getProperty(PropertyKey.ORE)
                .setOreByProducts(GTMaterials.Lead, GTMaterials.Selenium, GTMaterials.Gold);

        CALAVERITE = material("calaverite").dust().ore()
                .components(GTMaterials.Gold, 1, GTMaterials.Tellurium, 2)
                .flags(MaterialFlags.DECOMPOSITION_BY_ELECTROLYZING)
                .colorAverage().buildAndRegister();
        CALAVERITE.getProperty(PropertyKey.ORE)
                .setOreByProducts(GTMaterials.Gold, GTMaterials.Tellurium, GTMaterials.Silver);

        // This is the Se/Te-bearing fraction after copper/precious-metal recovery, not whole anode mud.
        // 4:1 is a game recovery ratio, not an asserted natural concentration.
        SELENIUM_TELLURIUM_RESIDUE = material("selenium_tellurium_residue").dust()
                .components(GTMaterials.Selenium, 4, GTMaterials.Tellurium, 1)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .colorAverage().buildAndRegister();

        // Hot SeO2 is captured from the oxidative roast; direct electrolysis would skip recovery.
        SELENIUM_DIOXIDE = material("selenium_dioxide")
                .gas(new FluidBuilder().temperature(900))
                .components(GTMaterials.Selenium, 1, GTMaterials.Oxygen, 2)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .colorAverage().buildAndRegister();
        TELLURIUM_DIOXIDE = material("tellurium_dioxide").dust()
                .components(GTMaterials.Tellurium, 1, GTMaterials.Oxygen, 2)
                .flags(MaterialFlags.DECOMPOSITION_BY_ELECTROLYZING)
                .colorAverage().buildAndRegister();
    }

    private static Material.Builder material(String name) {
        return new Material.Builder(new ResourceLocation("gtcsolo", name));
    }
}
