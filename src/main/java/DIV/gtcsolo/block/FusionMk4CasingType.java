package DIV.gtcsolo.block;

import com.gregtechceu.gtceu.api.block.IFusionCasingType;
import net.minecraft.resources.ResourceLocation;

/** MK4 uses the same active casing and connected-texture contract as GT's MK1-3. */
public enum FusionMk4CasingType implements IFusionCasingType {
    INSTANCE;

    public static final ResourceLocation TEXTURE =
            new ResourceLocation("gtcsolo", "block/casings/fusion/fusion_casing_mk4");

    @Override public ResourceLocation getTexture() { return TEXTURE; }
    @Override public int getHarvestLevel() { return 3; }
    @Override public String getSerializedName() { return "fusion_casing_mk4"; }
}
