package DIV.gtcsolo.render.material;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.common.pipelike.cable.Insulation;
import net.minecraft.resources.ResourceLocation;

/** Complete-color textures for the gtcsolo Infinity conductor, on the client only. */
public final class InfinityCableTextures {
    private static final ResourceLocation MATERIAL = new ResourceLocation("gtcsolo", "infinity");
    private static final ResourceLocation SIDE = GTCEu.id("block/material_sets/infinity/wire_side");
    private static final ResourceLocation END = GTCEu.id("block/material_sets/infinity/wire_end");

    private InfinityCableTextures() {}

    public static boolean matches(Material material) {
        return material != null && MATERIAL.equals(material.getResourceLocation());
    }

    public static PipeModel create(Insulation insulation) {
        // 1.6.4 looks for an unsuffixed wire.png before appending _side/_end.
        // Bind these paths explicitly, and avoid inheriting unrelated secondary layers.
        PipeModel model = new PipeModel(insulation.thickness, () -> SIDE, () -> END, null, null);
        if (insulation.isCable) {
            // These masks include the stock dark tint in their pixels. The side of the
            // cable can therefore show the full-color material while its end keeps a rim.
            model.setEndOverlayTexture(GTCEu.id(
                    "block/material_sets/infinity/cable_end_" + insulation.insulationLevel));
        }
        return model;
    }
}
