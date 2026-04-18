package DIV.gtcsolo.integration.jei;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.integration.mekanism.ChemicalBridge;
import DIV.gtcsolo.registry.ModItems;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.client.jei.ChemicalStackRenderer;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ChemicalConversionCategory implements IRecipeCategory<ChemicalConversionRecipe> {

    public static final ResourceLocation UID = new ResourceLocation(Gtcsolo.MODID, "chemical_conversion");
    public static final RecipeType<ChemicalConversionRecipe> RECIPE_TYPE =
            new RecipeType<>(UID, ChemicalConversionRecipe.class);

    private static final int WIDTH = 120;
    private static final int HEIGHT = 36;

    // スロット位置
    private static final int FLUID_X = 8;
    private static final int FLUID_Y = 4;
    private static final int ICON_X = 48;
    private static final int ICON_Y = 2;
    private static final int CHEM_X = 80;
    private static final int CHEM_Y = 4;
    private static final int SLOT_SIZE = 24;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableStatic conversionArrow;

    public ChemicalConversionCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModItems.CONVERSION_SYSTEM.get()));
        conversionArrow = guiHelper.drawableBuilder(
                new ResourceLocation(Gtcsolo.MODID, "textures/block/port/conversion/conversion_jei.png"),
                0, 0, 16, 16)
                .setTextureSize(16, 16)
                .build();
    }

    @Override
    public @NotNull RecipeType<ChemicalConversionRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("gui.gtcsolo.chemical_conversion");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(@NotNull ChemicalConversionRecipe recipe, @NotNull mezz.jei.api.gui.ingredient.IRecipeSlotsView slotsView,
                     @NotNull net.minecraft.client.gui.GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 中央に変換アイコン描画
        conversionArrow.draw(guiGraphics, ICON_X, ICON_Y + 8);
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ChemicalConversionRecipe recipe,
                          @NotNull IFocusGroup focuses) {
        // 左: GT液体
        builder.addSlot(RecipeIngredientRole.INPUT, FLUID_X + 1, FLUID_Y + 1)
                .addIngredients(ForgeTypes.FLUID_STACK, List.of(recipe.fluid()))
                .setFluidRenderer(1000, false, SLOT_SIZE - 2, SLOT_SIZE - 2);

        // 右: Mekanism Chemical
        String[] parts = recipe.chemKey().split(":", 3);
        if (parts.length < 3) return;
        ResourceLocation chemId = new ResourceLocation(parts[1], parts[2]);

        switch (recipe.chemType()) {
            case GAS -> {
                Gas gas = MekanismAPI.gasRegistry().getValue(chemId);
                if (gas != null && !gas.isEmptyType()) {
                    builder.addSlot(RecipeIngredientRole.OUTPUT, CHEM_X + 1, CHEM_Y + 1)
                            .addIngredients(MekanismJEI.TYPE_GAS, List.of(new GasStack(gas, 1000)))
                            .setCustomRenderer(MekanismJEI.TYPE_GAS,
                                    new ChemicalStackRenderer<>(1000, SLOT_SIZE - 2, SLOT_SIZE - 2));
                }
            }
            case INFUSION -> {
                InfuseType infuseType = MekanismAPI.infuseTypeRegistry().getValue(chemId);
                if (infuseType != null && !infuseType.isEmptyType()) {
                    builder.addSlot(RecipeIngredientRole.OUTPUT, CHEM_X + 1, CHEM_Y + 1)
                            .addIngredients(MekanismJEI.TYPE_INFUSION, List.of(new InfusionStack(infuseType, 1000)))
                            .setCustomRenderer(MekanismJEI.TYPE_INFUSION,
                                    new ChemicalStackRenderer<>(1000, SLOT_SIZE - 2, SLOT_SIZE - 2));
                }
            }
            default -> {
                // Pigment/Slurry — 未対応（将来拡張）
            }
        }
    }
}