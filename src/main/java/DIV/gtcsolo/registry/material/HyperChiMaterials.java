package DIV.gtcsolo.registry.material;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.resources.ResourceLocation;

/** Feedstocks for the Hyper-chi fusion and extreme-temperature alloying chain. */
public final class HyperChiMaterials {
    public static Material ANCIENT_DEBRIS;
    public static Material HYPERIUM;
    public static Material HYPER_CHI;

    private HyperChiMaterials() {}

    public static void init() {
        // Chi is Sculk itself; retain the native material, dust and vanilla block identity.
        GTMaterials.Sculk.setFormula("χ");
        var sculkFluids = GTMaterials.Sculk.getProperty(PropertyKey.FLUID);
        var liquid = new FluidBuilder().state(FluidState.LIQUID);
        if (sculkFluids == null) {
            GTMaterials.Sculk.setProperty(PropertyKey.FLUID,
                    new FluidProperty(FluidStorageKeys.LIQUID, liquid));
        } else if (sculkFluids.getQueuedBuilder(FluidStorageKeys.LIQUID) == null
                && sculkFluids.getEntry(FluidStorageKeys.LIQUID) == null) {
            sculkFluids.enqueueRegistration(FluidStorageKeys.LIQUID, liquid);
        }

        ANCIENT_DEBRIS = new Material.Builder(id("ancient_debris"))
                .dust().color(0x665047).iconSet(MaterialIconSet.ROUGH)
                .formula("Nrχ")
                .flags(MaterialFlags.NO_SMELTING, MaterialFlags.DISABLE_DECOMPOSITION,
                        MaterialFlags.EXCLUDE_BLOCK_CRAFTING_RECIPES)
                .buildAndRegister();
        ExternalMaterialBridge.bind(ANCIENT_DEBRIS, "minecraft")
                .block(TagPrefix.block, "ancient_debris");
        // One debris yields two dust units, then one vanilla scrap and one Sculk dust.
        // In particular, never inherit the storage-block default of nine material units.
        TagPrefix.block.modifyMaterialAmount(ANCIENT_DEBRIS, 2);

        HYPERIUM = new Material.Builder(id("hyperium"))
                .ingot().dust().ore().fluid()
                .color(0x65507D).iconSet(MaterialIconSet.METALLIC).formula("Hy")
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        HYPER_CHI = new Material.Builder(id("hyper_chi"))
                .dust().fluid().color(0x503E7B).iconSet(MaterialIconSet.SHINY)
                .formula("Hy≡χ").flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Gtcsolo.MODID, name);
    }
}
