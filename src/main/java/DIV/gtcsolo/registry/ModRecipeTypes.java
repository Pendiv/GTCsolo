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
    }
}