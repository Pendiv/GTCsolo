package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.MegaMixerMachines;
import DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin;
import DIV.gtcsolo.registry.OreProcessingFactoryMachines;
import DIV.gtcsolo.registry.MegaAlloyBlastFurnaceMachines;
import DIV.gtcsolo.registry.FusionVariantMachines;
import DIV.gtcsolo.machine.fusion.FusionVariantSettings;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** JEI indexing stores only descriptors; full-volume native GT previews are never created. */
public final class MegaMultiblockInfoCategory extends ModularUIRecipeCategory<MegaPreviewRecipe> {
    public static final RecipeType<MegaPreviewRecipe> TYPE = new RecipeType<>(
            new ResourceLocation(Gtcsolo.MODID, "mega_multiblock_info"), MegaPreviewRecipe.class);
    private final IDrawable megaIcon, background;

    public MegaMultiblockInfoCategory(IJeiHelpers helpers) {
        super(new LiveRecipeWrapperFactory<>(recipe -> {
            var wrapper = new ModularWrapper<>(new MegaPreviewWidget(recipe, (stack, uses) -> {
                var runtime = GtcSoloJeiPlugin.getRuntimeOrNull();
                if (runtime != null && !stack.isEmpty()) runtime.getRecipesGui().show(List.of(
                        helpers.getFocusFactory().createFocus(uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT,
                                VanillaTypes.ITEM_STACK, stack)));
            }));
            wrapper.setRecipeWidget(0, 0);
            return wrapper;
        }));
        megaIcon = helpers.getGuiHelper().createDrawableItemStack((MegaMixerMachines.CORE!=null
                ? MegaMixerMachines.CORE : FusionVariantMachines.DEFINITIONS.get(FusionVariantSettings.MEGA_MK4)).asStack());
        background = new IDrawable() {
            @Override public int getWidth() { return MegaPreviewWidget.WIDTH; }
            @Override public int getHeight() { return MegaPreviewWidget.availableHeight(); }
            @Override public void draw(GuiGraphics graphics, int x, int y) {}
        };
    }

    public static void registerMegaRecipes(IRecipeRegistration registration) {
        var recipes = new java.util.ArrayList<MegaPreviewRecipe>();
        if (MegaMixerMachines.CORE != null) recipes.add(new MegaPreviewRecipe(MegaMixerMachines.CORE,
                "mega_mixer", 'T', "gtceu:reaction_safe_mixing_casing", false, "gtcsolo.mega_preview.ports.mixer"));
        if (OreProcessingFactoryMachines.CORE != null) recipes.add(new MegaPreviewRecipe(OreProcessingFactoryMachines.CORE,
                "ore_processing_factory", '7', "gtceu:secure_maceration_casing", false, "gtcsolo.mega_preview.ports.factory"));
        if (MegaAlloyBlastFurnaceMachines.CORE != null) recipes.add(new MegaPreviewRecipe(MegaAlloyBlastFurnaceMachines.CORE,
                "mega_alloy_blast_furnace", '%', "gtceu:high_temperature_smelting_casing", true, "gtcsolo.mega_preview.ports.blast"));
        var fusion=FusionVariantMachines.DEFINITIONS.get(FusionVariantSettings.MEGA_MK4);
        if (fusion!=null) recipes.add(new MegaPreviewRecipe(fusion,"mega_fusion",'\0',
                "gtcsolo:fusion_casing_mk4",false,"gtcsolo.mega_fusion.heat"));
        registration.addRecipes(TYPE, recipes);
    }

    public static boolean isAvailable() {
        return MegaMixerMachines.CORE!=null || FusionVariantMachines.DEFINITIONS.containsKey(FusionVariantSettings.MEGA_MK4);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, MegaPreviewRecipe recipe, IFocusGroup focuses) {
        // Also called during startup indexing: no widget, scene, or shape expansion here.
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(recipe.definition().asStack());
    }
    @Override public RecipeType<MegaPreviewRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("gtcsolo.jei.mega_multiblock_info"); }
    @Override public IDrawable getIcon() { return megaIcon; }
    @Override public IDrawable getBackground() { return background; }
}
