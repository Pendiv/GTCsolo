package DIV.gtcsolo.integration.jei;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.client.BilingualSearchNames;
import DIV.gtcsolo.common.TooltipDisplayHelper;
import DIV.gtcsolo.integration.jei.multiblock.MultiblockIntroductionCategory;
import DIV.gtcsolo.integration.jei.multiblock.GlossaryCategory;
import DIV.gtcsolo.integration.jei.multiblock.FontComparisonCategory;
import DIV.gtcsolo.integration.jei.multiblock.MegaMultiblockInfoCategory;
import DIV.gtcsolo.integration.jei.starforge.StarForgeInfoCategory;
import DIV.gtcsolo.item.AbstractLocusItem;
import DIV.gtcsolo.machine.starforge.StarForgeTraceData;
import DIV.gtcsolo.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraftforge.fluids.FluidStack;
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
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        var itemStacks = ItemStackLinkedSet.createTypeAndTagSet();
        for (Item item : ForgeRegistries.ITEMS.getValues()) itemStacks.add(item.getDefaultInstance());
        for (var tab : CreativeModeTabs.allTabs()) {
            itemStacks.addAll(tab.getDisplayItems());
            itemStacks.addAll(tab.getSearchTabDisplayItems());
        }
        for (ItemStack stack : itemStacks) {
            if (stack.isEmpty()) continue;
            var aliases = BilingualSearchNames.aliases(stack.getHoverName(), ForgeRegistries.ITEMS.getKey(stack.getItem()));
            if (!aliases.isEmpty()) registration.addAliases(VanillaTypes.ITEM_STACK, stack, aliases);
        }
        for (var fluid : ForgeRegistries.FLUIDS.getValues()) {
            if (!fluid.defaultFluidState().isSource()) continue;
            FluidStack stack = new FluidStack(fluid, 1000);
            if (stack.isEmpty()) continue;
            var aliases = BilingualSearchNames.aliases(stack.getDisplayName(), ForgeRegistries.FLUIDS.getKey(fluid));
            if (!aliases.isEmpty()) registration.addAliases(ForgeTypes.FLUID_STACK, stack, aliases);
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new StarForgeInfoCategory(registration.getJeiHelpers()));
        registration.addRecipeCategories(new MultiblockIntroductionCategory(registration.getJeiHelpers()));
        registration.addRecipeCategories(new GlossaryCategory(registration.getJeiHelpers()));
        registration.addRecipeCategories(new FontComparisonCategory(registration.getJeiHelpers()));
        if (MegaMultiblockInfoCategory.isAvailable())
            registration.addRecipeCategories(new MegaMultiblockInfoCategory(registration.getJeiHelpers()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        StarForgeInfoCategory.registerRecipes(registration);
        GlossaryCategory.registerRecipes(registration);
        MultiblockIntroductionCategory.registerRecipes(registration);
        FontComparisonCategory.registerRecipes(registration);
        if (MegaMultiblockInfoCategory.isAvailable())
            MegaMultiblockInfoCategory.registerMegaRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(net.minecraft.world.item.Items.BOOK), GlossaryCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(net.minecraft.world.item.Items.BOOK), FontComparisonCategory.RECIPE_TYPE);
        // 各 Trace 付き star_locus をカタリストとして登録
        // (JEI で「使用可能」表示 + R キーで該当ページ呼び出し)
        for (StarForgeTraceData.TraceInfo info : StarForgeTraceData.all()) {
            ItemStack stack = AbstractLocusItem.of(ModItems.STAR_LOCUS.get(), info.trace);
            registration.addRecipeCatalyst(stack, StarForgeInfoCategory.RECIPE_TYPE);
        }
        // 空 star_locus も全 8 軌跡カテゴリで使えるよう登録
        ItemStack emptyLocus = new ItemStack(ModItems.STAR_LOCUS.get());
        registration.addRecipeCatalyst(emptyLocus, StarForgeInfoCategory.RECIPE_TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        Gtcsolo.LOGGER.debug("Captured JEI runtime in GtcSoloJeiPlugin");
    }

    @Override
    public void onRuntimeUnavailable() {
        DIV.gtcsolo.integration.jei.multiblock.MegaPreviewLifecycle.clear();
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
