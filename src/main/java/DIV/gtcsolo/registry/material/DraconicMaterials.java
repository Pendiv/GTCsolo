package DIV.gtcsolo.registry.material;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** GT processing around DE's existing metals and the ISMBF steel alloy. */
public final class DraconicMaterials {
    public static Material DRACONIUM;
    public static Material AWAKENED_DRACONIUM;
    public static Material DRACONIUM_STEEL;

    private DraconicMaterials() {}

    public static void init() {
        DRACONIUM = metalBuilder("draconium", 0x8A60C2)
                // D is Deuterium and Dr is Duranium in native GregTech.
                .formula("Dc")
                // GT1.6.4 Tritanium coil = 10,800; HIGH gas = 50 mB Argon, 67% time.
                .blastTemp(10800, BlastProperty.GasTier.HIGH, (int) GTValues.V[GTValues.UV], 1000)
                .cableProperties(GTValues.V[GTValues.UV], 1, 4)
                .buildAndRegister();
        bind(DRACONIUM, "draconium");
        // Match DE's forge:* /draconium_awakened tags; DE item IDs use the opposite word order.
        AWAKENED_DRACONIUM = metalBuilder("draconium_awakened", 0xF3A24A).buildAndRegister();
        bind(AWAKENED_DRACONIUM, "awakened_draconium");

        DRACONIUM_STEEL = metalBuilder("draconium_steel", 0x79638F)
                .components(GTMaterials.Trinium, 1, GTMaterials.Steel, 1, DRACONIUM, 1, GTMaterials.Lutetium, 1)
                // The alloy is first made as molten metal in the ISMBF, then cast into forms.
                .flags(MaterialFlags.NO_SMELTING)
                // Match Mithrilite: twice Neutronium's flow and heat limit, with every fluid protection.
                .fluidPipeProperties(200_000, 10_000, true, true, true, true)
                .buildAndRegister();
    }

    private static Material.Builder metalBuilder(String name, int color) {
        return new Material.Builder(new ResourceLocation(Gtcsolo.MODID, name))
                .ingot().dust().fluid().color(color).iconSet(MaterialIconSet.METALLIC)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_DENSE,
                        MaterialFlags.GENERATE_ROD, MaterialFlags.GENERATE_LONG_ROD,
                        MaterialFlags.GENERATE_BOLT_SCREW, MaterialFlags.GENERATE_RING,
                        MaterialFlags.GENERATE_ROUND, MaterialFlags.GENERATE_GEAR,
                        MaterialFlags.GENERATE_SMALL_GEAR, MaterialFlags.GENERATE_FOIL,
                        MaterialFlags.GENERATE_SPRING, MaterialFlags.GENERATE_SPRING_SMALL,
                        MaterialFlags.GENERATE_FINE_WIRE, MaterialFlags.GENERATE_ROTOR,
                        MaterialFlags.GENERATE_FRAME, MaterialFlags.DISABLE_DECOMPOSITION);
    }

    private static void bind(Material material, String base) {
        ExternalMaterialBridge.bind(material, "draconicevolution")
                .item(TagPrefix.dust, base + "_dust")
                .item(TagPrefix.ingot, base + "_ingot")
                .item(TagPrefix.nugget, base + "_nugget")
                .block(TagPrefix.block, base + "_block");
    }

    /** Supplement forms absent from the shared main-tab prefix list. */
    public static void addToCreativeTab(CreativeModeTab.Output output) {
        for (Material material : new Material[]{DRACONIUM, AWAKENED_DRACONIUM, DRACONIUM_STEEL}) {
            for (TagPrefix prefix : new TagPrefix[]{TagPrefix.dustSmall, TagPrefix.dustTiny,
                    TagPrefix.plateDouble, TagPrefix.wireFine, TagPrefix.rotor, TagPrefix.ingotHot,
                    TagPrefix.wireGtDouble, TagPrefix.wireGtQuadruple, TagPrefix.wireGtOctal, TagPrefix.wireGtHex,
                    TagPrefix.cableGtSingle, TagPrefix.cableGtDouble, TagPrefix.cableGtQuadruple,
                    TagPrefix.cableGtOctal, TagPrefix.cableGtHex}) {
                var stack = ChemicalHelper.get(prefix, material);
                if (!stack.isEmpty()) output.accept(stack);
            }
        }
    }
}
