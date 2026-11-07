package DIV.gtcsolo.render.material;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.common.pipelike.cable.Insulation;
import com.gregtechceu.gtceu.common.pipelike.fluidpipe.FluidPipeType;
import com.gregtechceu.gtceu.common.pipelike.item.ItemPipeType;
import net.minecraft.resources.ResourceLocation;

/** Bind supplied sprites while retaining native geometry, covers, insulation and restriction marks. */
public final class EntroniumPipeModels {
    private EntroniumPipeModels() {}

    public static PipeModel create(FluidPipeType type) {
        return pipe(type.thickness, type.name);
    }

    public static PipeModel create(ItemPipeType type) {
        PipeModel model = pipe(type.getThickness(), type.getSizeForTexture());
        if (type.isRestrictive()) model.setSideOverlayTexture(GTCEu.id("block/pipe/pipe_restrictive"));
        return model;
    }

    public static PipeModel create(Insulation type) {
        PipeModel model = new PipeModel(type.thickness,
                () -> GTCEu.id(type.isCable ? "block/cable/insulation_5" :
                        "block/material_sets/entronium/wire_side"),
                () -> GTCEu.id("block/material_sets/entronium/wire_end"), null, null);
        if (type.isCable) model.setEndOverlayTexture(GTCEu.id("block/cable/insulation_" + type.insulationLevel));
        return model;
    }

    private static PipeModel pipe(float thickness, String size) {
        return new PipeModel(thickness,
                () -> new ResourceLocation("gtcsolo", "block/pipe/entronium/pipe_side"),
                () -> new ResourceLocation("gtcsolo", "block/pipe/entronium/pipe_" + size + "_in"), null, null);
    }
}
