package DIV.gamacrafting.compat.jei;

import DIV.gamacrafting.Gamacrafting;
import DIV.gamacrafting.content.crafting.desk.DeskBlockEntity;
import DIV.gamacrafting.content.crafting.desk.DeskMenu;
import DIV.gamacrafting.content.crafting.desk.DeskScreen;
import DIV.gamacrafting.content.crafting.desk.IDeskRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class GamacraftingJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = Gamacrafting.id("jei");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new DeskRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(DeskRecipeCategory.TYPE, level.getRecipeManager()
                .getAllRecipesFor(Gamacrafting.DESK_TYPE.get()).stream()
                .filter(IDeskRecipe::showToJei)
                .toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(DeskRecipeCategory.TYPE, Gamacrafting.DESK_ITEM.get());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(DeskMenu.class, Gamacrafting.DESK_MENU.get(), DeskRecipeCategory.TYPE,
                1, DeskBlockEntity.GRID_SIZE, 1 + DeskBlockEntity.GRID_SIZE, 36);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(DeskScreen.class, 56, 186, 14, 15, DeskRecipeCategory.TYPE);
    }
}
