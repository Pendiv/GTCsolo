package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.recipe.ManualCraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * バニラ系クラフトレシピのシリアライザ登録 (GT レシピタイプの {@link ModRecipeTypes} とは別系統)。
 */
public class ModRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Gtcsolo.MODID);

    /** マニュアル (本 + レンチ耐久-1)。 special recipe なので JSON は {@code {"type":"gtcsolo:manual_crafting"}} のみ。 */
    public static final RegistryObject<SimpleCraftingRecipeSerializer<ManualCraftingRecipe>> MANUAL_CRAFTING =
            SERIALIZERS.register("manual_crafting",
                    () -> new SimpleCraftingRecipeSerializer<>(ManualCraftingRecipe::new));

    private ModRecipeSerializers() {}
}
