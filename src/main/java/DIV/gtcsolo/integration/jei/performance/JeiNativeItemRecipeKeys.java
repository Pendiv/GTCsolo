package DIV.gtcsolo.integration.jei.performance;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.ISubtypeManager;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.ingredients.IngredientManager;
import mezz.jei.library.ingredients.subtypes.SubtypeInterpreters;
import mezz.jei.library.ingredients.subtypes.SubtypeManager;
import mezz.jei.library.plugins.vanilla.ingredients.ItemStackHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.IdentityHashMap;

public final class JeiNativeItemRecipeKeys {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String STACK_PACKAGE = "mezz.jei.library.ingredients.itemStacks.";

    private final Class<?> plainStack;
    private final Class<?> taggedStack;
    private final MethodHandle plainItemHolder;
    private final MethodHandle taggedItemHolder;
    private final IdentityHashMap<?, ?> subtypeInterpreters;
    private final boolean verify;

    private JeiNativeItemRecipeKeys(MethodHandles.Lookup lookup, IdentityHashMap<?, ?> subtypeInterpreters)
            throws ReflectiveOperationException {
        ClassLoader loader = lookup.lookupClass().getClassLoader();
        this.plainStack = Class.forName(STACK_PACKAGE + "NormalizedTypedItem", false, loader);
        this.taggedStack = Class.forName(STACK_PACKAGE + "NormalizedTypedItemStack", false, loader);
        this.plainItemHolder = getter(lookup, plainStack, "itemHolder", Holder.class);
        this.taggedItemHolder = getter(lookup, taggedStack, "itemHolder", Holder.class);
        this.subtypeInterpreters = subtypeInterpreters;
        this.verify = JeiOptimizationOptions.recipeIndexVerificationEnabled();
    }

    public static JeiNativeItemRecipeKeys create(MethodHandles.Lookup lookup, IIngredientManager ingredientManager) {
        if (!Boolean.parseBoolean(System.getProperty("gtcsolo.optimization.jei.nativeItemKeys", "true"))) {
            return null;
        }
        var jei = ModList.get().getModContainerById("jei");
        if (jei.isEmpty()) {
            return null;
        }
        String version = jei.get().getModInfo().getVersion().toString();
        if (!version.equals("15.20.0.112") && !version.equals("15.20.0.115")) {
            return null;
        }
        try {
            if (ingredientManager.getClass() != IngredientManager.class) {
                return null;
            }
            Object helper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
            if (helper == null || helper.getClass() != ItemStackHelper.class) {
                return null;
            }
            var subtypeManagerGetter = getter(lookup, ItemStackHelper.class, "subtypeManager", ISubtypeManager.class);
            var subtypeManager = (ISubtypeManager) subtypeManagerGetter.invokeExact(helper);
            if (subtypeManager == null || subtypeManager.getClass() != SubtypeManager.class) {
                return null;
            }
            var interpretersGetter = getter(lookup, SubtypeManager.class, "interpreters", SubtypeInterpreters.class);
            var interpreters = (SubtypeInterpreters) interpretersGetter.invokeExact((Object) subtypeManager);
            if (interpreters == null || interpreters.getClass() != SubtypeInterpreters.class) {
                return null;
            }
            var mapGetter = getter(lookup, SubtypeInterpreters.class, "map", java.util.Map.class);
            var map = (java.util.Map<?, ?>) mapGetter.invokeExact((Object) interpreters);
            if (map == null || map.getClass() != IdentityHashMap.class) {
                return null;
            }
            return new JeiNativeItemRecipeKeys(lookup, (IdentityHashMap<?, ?>) map);
        } catch (Throwable failure) {
            if (failure instanceof Error error && !(error instanceof LinkageError)) {
                throw error;
            }
            LOGGER.warn("JEI native item recipe keys are unavailable", failure);
            return null;
        }
    }

    public Item getKey(ITypedIngredient<?> typedIngredient) throws Throwable {
        if (typedIngredient == null) {
            return null;
        }
        Class<?> stackClass = typedIngredient.getClass();
        Holder<?> holder;
        if (stackClass == plainStack) {
            holder = (Holder<?>) plainItemHolder.invokeExact((Object) typedIngredient);
        } else if (stackClass == taggedStack) {
            holder = (Holder<?>) taggedItemHolder.invokeExact((Object) typedIngredient);
        } else {
            return null;
        }

        // ItemStack(Holder) resolves through the current Forge delegate when it is reconstructed.
        Item item = ForgeRegistries.ITEMS.getDelegateOrThrow((Item) holder.value()).value();
        return subtypeInterpreters.containsKey(item) ? null : item;
    }

    public boolean verifiesNativeKey() {
        return verify;
    }

    private static MethodHandle getter(MethodHandles.Lookup lookup, Class<?> owner, String field, Class<?> type)
            throws ReflectiveOperationException {
        return MethodHandles.privateLookupIn(owner, lookup)
                .findGetter(owner, field, type)
                .asType(MethodType.methodType(type, Object.class));
    }
}
