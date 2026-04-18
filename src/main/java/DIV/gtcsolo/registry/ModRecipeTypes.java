package DIV.gtcsolo.registry;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.*;

public class ModRecipeTypes {

    // fantasy_element_constructor の略
    public static GTRecipeType FEC;

    // WEN (Wireless Energy Network) — ダミーレシピタイプ
    // マルチブロック登録に必要だが、実際のレシピは持たない
    public static GTRecipeType WEN_STORAGE;

    // Space Forge — 仮値 IO (16, 3, 3, 3)
    public static GTRecipeType SPACEFORGE;

    // Chemical Combustion Generator — 液体入力3、発電
    public static GTRecipeType CHEMICAL_COMBUSTION_GENERATOR;

    // Fantasia Forge — 幻想元素鍛造マルチブロック、items 3/3, fluids 1/1
    public static GTRecipeType FANTASIA_FORGE;

    public static void init() {
        FEC = new GTRecipeType(
                new ResourceLocation("gtcsolo", "fec"), "multiblock")
                // アイテム入力6, アイテム出力1, 液体入力1, 液体出力0
                .setMaxIOSize(6, 1, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);

        // WEN Storage ダミー
        WEN_STORAGE = new GTRecipeType(
                new ResourceLocation("gtcsolo", "wen_storage"), "multiblock")
                .setMaxIOSize(0, 0, 0, 0)
                .setEUIO(IO.IN);
        ResourceLocation wenId = new ResourceLocation("gtcsolo", "wen_storage");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, wenId, WEN_STORAGE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, wenId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(wenId, WEN_STORAGE);

        ResourceLocation fecId = new ResourceLocation("gtcsolo", "fec");
        // GTCEu自身のGTRecipeTypes.register()と同様に、3つ全てに登録する
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, fecId, FEC);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, fecId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(fecId, FEC);

        // Space Forge
        SPACEFORGE = new GTRecipeType(
                new ResourceLocation("gtcsolo", "spaceforge"), "multiblock")
                .setMaxIOSize(16, 3, 3, 3)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation spaceforgeId = new ResourceLocation("gtcsolo", "spaceforge");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, spaceforgeId, SPACEFORGE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, spaceforgeId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(spaceforgeId, SPACEFORGE);

        // Chemical Combustion Generator
        CHEMICAL_COMBUSTION_GENERATOR = new GTRecipeType(
                new ResourceLocation("gtcsolo", "chemical_combustion_generator"), "multiblock")
                .setMaxIOSize(0, 0, 3, 3)
                .setEUIO(IO.OUT)
                .setProgressBar(GuiTextures.PROGRESS_BAR_GAS_COLLECTOR, LEFT_TO_RIGHT);
        ResourceLocation ccgId = new ResourceLocation("gtcsolo", "chemical_combustion_generator");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, ccgId, CHEMICAL_COMBUSTION_GENERATOR);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, ccgId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(ccgId, CHEMICAL_COMBUSTION_GENERATOR);

        // Fantasia Forge
        FANTASIA_FORGE = new GTRecipeType(
                new ResourceLocation("gtcsolo", "fantasia_forge"), "multiblock")
                .setMaxIOSize(3, 3, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation ffId = new ResourceLocation("gtcsolo", "fantasia_forge");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, ffId, FANTASIA_FORGE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, ffId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(ffId, FANTASIA_FORGE);
    }
}