package DIV.gtcsolo.render.halo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import committee.nova.mods.avaritia.client.model.loader.base.BaseGeometry;
import committee.nova.mods.avaritia.client.model.loader.base.BaseModelLoader;
import committee.nova.mods.avaritia.client.model.loader.base.HaloSetting;
import committee.nova.mods.avaritia.client.model.loader.base.HaloUtils;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;

import java.util.function.Function;

/**
 * Re-Avaritia-compatible halo loader whose halo color is resolved at render time.
 */
public final class RainbowHaloModelLoader
        extends BaseModelLoader<RainbowHaloModelLoader.RainbowHaloGeometry> {

    public static final RainbowHaloModelLoader INSTANCE = new RainbowHaloModelLoader();

    private RainbowHaloModelLoader() {}

    @Override
    public RainbowHaloGeometry read(
            JsonObject modelContents,
            JsonDeserializationContext deserializationContext) throws JsonParseException {
        BlockModel baseModel =
                deserializationContext.deserialize(clear(modelContents, "halo"), BlockModel.class);
        HaloSetting halo = getHalo(modelContents);
        long cycleMillis = GsonHelper.getAsLong(
                modelContents.getAsJsonObject("halo"),
                "cycleMillis",
                5_000L);
        if (cycleMillis < 250L) {
            throw new JsonParseException("Rainbow halo cycleMillis must be at least 250");
        }
        return new RainbowHaloGeometry(baseModel, new RainbowHaloSetting(halo, cycleMillis));
    }

    public static final class RainbowHaloGeometry extends BaseGeometry<RainbowHaloGeometry> {

        private final RainbowHaloSetting setting;

        private RainbowHaloGeometry(BlockModel baseModel, RainbowHaloSetting setting) {
            super(baseModel);
            this.setting = setting;
        }

        @Override
        public BakedModel bake(
                IGeometryBakingContext owner,
                ModelBaker bakery,
                Function<Material, TextureAtlasSprite> spriteGetter,
                ModelState modelTransform,
                ItemOverrides overrides,
                ResourceLocation modelLocation) {
            BakedModel bakedBaseModel =
                    this.baseModel.bake(
                            bakery,
                            this.baseModel,
                            spriteGetter,
                            modelTransform,
                            modelLocation,
                            false);
            Material haloMaterial = this.baseModel.getMaterial(this.setting.halo().texture());
            TextureAtlasSprite haloSprite = spriteGetter.apply(haloMaterial);
            return new RainbowHaloBakedModel(
                    HaloUtils.tintLayers(bakedBaseModel, this.setting.halo().layerColors()),
                    haloSprite,
                    this.setting);
        }
    }
}
