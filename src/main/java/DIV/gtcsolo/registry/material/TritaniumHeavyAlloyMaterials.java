package DIV.gtcsolo.registry.material;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** Extreme-temperature alloy; ordinary GregTech handlers supply its metal forms. */
public final class TritaniumHeavyAlloyMaterials {
    public static Material TRITANIUM_HEAVY_ALLOY;

    private TritaniumHeavyAlloyMaterials() {}

    public static void init() {
        TRITANIUM_HEAVY_ALLOY = new Material.Builder(new ResourceLocation(Gtcsolo.MODID, "tritanium_heavy_alloy"))
                .ingot().dust().fluid().colorAverage().iconSet(MaterialIconSet.METALLIC)
                .components(GTMaterials.Tritanium, 1, GTMaterials.Trinium, 6,
                        GTMaterials.NaquadahEnriched, 6, GTMaterials.Netherite, 5, GTMaterials.Bismuth, 3)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_DENSE,
                        MaterialFlags.GENERATE_ROD, MaterialFlags.GENERATE_LONG_ROD,
                        MaterialFlags.GENERATE_BOLT_SCREW, MaterialFlags.GENERATE_RING,
                        MaterialFlags.GENERATE_ROUND, MaterialFlags.GENERATE_GEAR,
                        MaterialFlags.GENERATE_SMALL_GEAR, MaterialFlags.GENERATE_FOIL,
                        MaterialFlags.GENERATE_SPRING, MaterialFlags.GENERATE_SPRING_SMALL,
                        MaterialFlags.GENERATE_FINE_WIRE, MaterialFlags.GENERATE_ROTOR,
                        MaterialFlags.GENERATE_FRAME, MaterialFlags.NO_SMELTING,
                        MaterialFlags.DISABLE_DECOMPOSITION)
                // Provisional values approved alongside the synthesis recipes.
                .cableProperties(GTValues.V[GTValues.UHV], 1, 2)
                .fluidPipeProperties(200_000, 10_000, true, true, true, true)
                .buildAndRegister();
    }

    /** Forms not covered by the common metal tab; cables also have their own shared tab. */
    public static void addToCreativeTab(CreativeModeTab.Output output) {
        for (TagPrefix prefix : new TagPrefix[]{TagPrefix.dustSmall, TagPrefix.dustTiny,
                TagPrefix.plateDouble, TagPrefix.wireFine, TagPrefix.rotor,
                TagPrefix.pipeTinyFluid, TagPrefix.pipeSmallFluid, TagPrefix.pipeNormalFluid,
                TagPrefix.pipeLargeFluid, TagPrefix.pipeHugeFluid,
                TagPrefix.pipeQuadrupleFluid, TagPrefix.pipeNonupleFluid}) {
            var stack = ChemicalHelper.get(prefix, TRITANIUM_HEAVY_ALLOY);
            if (!stack.isEmpty()) output.accept(stack);
        }
    }
}
