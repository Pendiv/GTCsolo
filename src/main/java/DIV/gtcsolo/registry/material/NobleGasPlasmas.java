package DIV.gtcsolo.registry.material;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;

/** Enable fusion feedstocks and noble-gas plasmas before GT registers material forms. */
public final class NobleGasPlasmas {
    private NobleGasPlasmas() {}

    public static void register() {
        // GT 1.6.4 defines these elements without any obtainable item/fluid forms.
        // Dust also enables GT's standard dust -> molten fluid extractor recipes.
        registerFeedstock(GTMaterials.Selenium, 494);
        registerFeedstock(GTMaterials.Tellurium, 723);
        for (Material material : new Material[]{GTMaterials.Krypton, GTMaterials.Xenon}) {
            var fluids = material.getProperty(PropertyKey.FLUID);
            // Another addon may already provide a plasma for this same material.
            if (fluids.getQueuedBuilder(FluidStorageKeys.PLASMA) == null
                    && fluids.getEntry(FluidStorageKeys.PLASMA) == null) {
                fluids.enqueueRegistration(FluidStorageKeys.PLASMA, new FluidBuilder().state(FluidState.PLASMA));
            }
        }
    }

    private static void registerFeedstock(Material material, int meltingTemperature) {
        if (!material.hasProperty(PropertyKey.DUST)) {
            material.setProperty(PropertyKey.DUST, new DustProperty());
        }
        var fluids = material.getProperty(PropertyKey.FLUID);
        var builder = new FluidBuilder().state(FluidState.LIQUID).temperature(meltingTemperature);
        if (fluids == null) {
            material.setProperty(PropertyKey.FLUID, new FluidProperty(FluidStorageKeys.LIQUID, builder));
        } else if (fluids.getQueuedBuilder(FluidStorageKeys.LIQUID) == null
                && fluids.getEntry(FluidStorageKeys.LIQUID) == null) {
            fluids.enqueueRegistration(FluidStorageKeys.LIQUID, builder);
        }
    }
}
