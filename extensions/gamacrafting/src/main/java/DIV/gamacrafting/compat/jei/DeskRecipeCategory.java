package DIV.gamacrafting.compat.jei;

import DIV.gamacrafting.Gamacrafting;
import DIV.gamacrafting.content.crafting.desk.DeskBlockEntity;
import DIV.gamacrafting.content.crafting.desk.DeskShapedRecipe;
import DIV.gamacrafting.content.crafting.desk.IDeskRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public final class DeskRecipeCategory implements IRecipeCategory<IDeskRecipe> {
    static final RecipeType<IDeskRecipe> TYPE = new RecipeType<>(Gamacrafting.id("desk"), IDeskRecipe.class);

    private static final Component TITLE = Component.translatable("recipe.gamacrafting.desk");
    private static final int GRID_COLUMNS = DeskBlockEntity.GRID_WIDTH;
    private static final int SLOT_SPACING = 18;
    private static final int GRID_WIDTH = GRID_COLUMNS * SLOT_SPACING;
    private static final ResourceLocation TEXTURE = Gamacrafting.id("textures/gui/container/desk.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable minecraftOff;
    private final IDrawable pickaxeOn;
    private final IDrawable blockOn;

    public DeskRecipeCategory(IGuiHelper helper) {
        this.background = helper.drawableBuilder(TEXTURE, 7, 17, GRID_WIDTH, 192).setTextureSize(512, 512).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, Gamacrafting.DESK_ITEM.get().getDefaultInstance());
        this.minecraftOff = helper.drawableBuilder(TEXTURE, 176, 0, 15, 12).setTextureSize(512, 512).build();
        this.pickaxeOn = helper.drawableBuilder(TEXTURE, 176, 12, 8, 8).setTextureSize(512, 512).build();
        this.blockOn = helper.drawableBuilder(TEXTURE, 176, 20, 10, 10).setTextureSize(512, 512).build();
    }

    @Override
    public RecipeType<IDeskRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void draw(IDeskRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (recipe.isMinecraft()) {
            this.pickaxeOn.draw(graphics, 106, 172);
            this.blockOn.draw(graphics, 99, 174);
        } else {
            this.minecraftOff.draw(graphics, 99, 172);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IDeskRecipe recipe, IFocusGroup focuses) {
        builder.moveRecipeTransferButton(GRID_WIDTH - 13, 178);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 73, 171).addItemStack(recipe.getResultItem(null));

        // JEI transfer addresses every desk slot in row-major order, including empty slots.
        IRecipeSlotBuilder[][] slots = new IRecipeSlotBuilder[GRID_COLUMNS][GRID_COLUMNS];
        for (int row = 0; row < GRID_COLUMNS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                slots[row][column] = builder.addSlot(RecipeIngredientRole.INPUT,
                        column * SLOT_SPACING + 1, row * SLOT_SPACING + 1);
            }
        }

        List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof DeskShapedRecipe shaped) {
            int width = shaped.getRecipeWidth();
            int height = shaped.getRecipeHeight();
            int columnOffset = width % 2 == 0 ? 0 : (GRID_COLUMNS - width) / 2;
            int rowOffset = height % 2 == 0 ? 0 : (GRID_COLUMNS - height) / 2;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    slots[row + rowOffset][column + columnOffset].addIngredients(ingredients.get(row * width + column));
                }
            }
        } else {
            builder.setShapeless(0, 163);
            int ingredientCount = Math.min(DeskBlockEntity.GRID_SIZE, ingredients.size());
            for (int index = 0; index < ingredientCount; index++) {
                slots[index / GRID_COLUMNS][index % GRID_COLUMNS].addIngredients(ingredients.get(index));
            }
        }
    }
}
