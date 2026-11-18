package DIV.gtcsolo.block;

import com.gregtechceu.gtceu.api.block.IFusionCasingType;
import net.minecraft.resources.ResourceLocation;

public enum FusionMk5CasingType implements IFusionCasingType {
    CASING("fusion_casing_mk5"),
    COIL("advanced_fusion_coil");

    private final String name;
    private final ResourceLocation texture;

    FusionMk5CasingType(String name) {
        this.name = name;
        this.texture = new ResourceLocation("gtcsolo", "block/casings/fusion/" + name);
    }

    @Override public ResourceLocation getTexture() { return texture; }
    @Override public int getHarvestLevel() { return 3; }
    @Override public String getSerializedName() { return name; }
}
