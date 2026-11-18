package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.JeiNativeItemRecipeKeys;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.recipes.collect.RecipeMap;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;

@Pseudo
@Mixin(value = RecipeMap.class, remap = false)
public abstract class JeiRecipeMapItemKeyMixin {
    @Unique private static final String GTC_SOLO_INGREDIENT_KEY =
            "getIngredientUid(Lmezz/jei/api/ingredients/ITypedIngredient;)Ljava/lang/Object;";
    @Unique private static final Logger GTC_SOLO_ITEM_KEYS_LOGGER = LogManager.getLogger();
    @Unique private JeiNativeItemRecipeKeys gtcsolo$nativeItemRecipeKeys;
    @Unique private boolean gtcsolo$loggedVerifiedItemRecipeKey;

    @Inject(method = "<init>(Ljava/util/Comparator;Lmezz/jei/api/runtime/IIngredientManager;"
            + "Lmezz/jei/api/recipe/RecipeIngredientRole;)V", at = @At("RETURN"), require = 0)
    private void gtcsolo$initializeItemRecipeKeys(Comparator<RecipeType<?>> recipeTypeComparator,
                                                 IIngredientManager ingredientManager, RecipeIngredientRole role,
                                                 CallbackInfo ci) {
        gtcsolo$nativeItemRecipeKeys = JeiNativeItemRecipeKeys.create(MethodHandles.lookup(), ingredientManager);
    }

    @Inject(method = GTC_SOLO_INGREDIENT_KEY, at = @At("HEAD"), cancellable = true, require = 0)
    private void gtcsolo$getIndexedItemRecipeKey(ITypedIngredient<?> ingredient, CallbackInfoReturnable<Object> cir) {
        JeiNativeItemRecipeKeys keys = gtcsolo$nativeItemRecipeKeys;
        if (keys != null && !keys.verifiesNativeKey()) {
            Item key = gtcsolo$tryItemRecipeKey(keys, ingredient);
            if (key != null) {
                cir.setReturnValue(key);
            }
        }
    }

    @Inject(method = GTC_SOLO_INGREDIENT_KEY, at = @At("RETURN"), require = 0)
    private void gtcsolo$verifyIndexedItemRecipeKey(ITypedIngredient<?> ingredient, CallbackInfoReturnable<Object> cir) {
        JeiNativeItemRecipeKeys keys = gtcsolo$nativeItemRecipeKeys;
        if (keys == null || !keys.verifiesNativeKey()) {
            return;
        }
        Item key = gtcsolo$tryItemRecipeKey(keys, ingredient);
        if (key == null) {
            return;
        }
        if (key != cir.getReturnValue()) {
            gtcsolo$nativeItemRecipeKeys = null;
            GTC_SOLO_ITEM_KEYS_LOGGER.warn("JEI native item recipe key disagreed with standard indexing");
        } else if (!gtcsolo$loggedVerifiedItemRecipeKey) {
            gtcsolo$loggedVerifiedItemRecipeKey = true;
            GTC_SOLO_ITEM_KEYS_LOGGER.info("[JEI-startup] Verified native item recipe key against standard indexing");
        }
    }

    @Unique
    private Item gtcsolo$tryItemRecipeKey(JeiNativeItemRecipeKeys keys, ITypedIngredient<?> ingredient) {
        try {
            return keys.getKey(ingredient);
        } catch (Throwable failure) {
            if (failure instanceof Error error && !(error instanceof LinkageError)) {
                throw error;
            }
            gtcsolo$nativeItemRecipeKeys = null;
            GTC_SOLO_ITEM_KEYS_LOGGER.warn("JEI native item recipe keys fell back to standard indexing", failure);
            return null;
        }
    }
}
