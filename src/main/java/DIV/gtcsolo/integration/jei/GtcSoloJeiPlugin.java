package DIV.gtcsolo.integration.jei;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.common.TooltipDisplayHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class GtcSoloJeiPlugin implements IModPlugin {

    private static volatile IJeiRuntime runtime;

    private static final IIngredientSubtypeInterpreter<ItemStack> TOOLTIP_INTERPRETER =
            (stack, context) -> TooltipDisplayHelper.getSubtypeKey(stack);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Gtcsolo.MODID, "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            if (Gtcsolo.MODID.equals(id.getNamespace())) {
                registration.registerSubtypeInterpreter(item, TOOLTIP_INTERPRETER);
            }
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        Gtcsolo.LOGGER.info("Captured JEI runtime in GtcSoloJeiPlugin");
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static IJeiRuntime getRuntimeOrNull() {
        return runtime;
    }

    public static IJeiRuntime getRuntimeOrThrow() {
        if (runtime == null) {
            throw new IllegalStateException(
                    "JEI runtime is not available yet. Join a world, wait for JEI to finish starting, then run the command again."
            );
        }
        return runtime;
    }
}