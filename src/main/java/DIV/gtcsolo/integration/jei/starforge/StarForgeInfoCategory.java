package DIV.gtcsolo.integration.jei.starforge;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.machine.starforge.StarForgeTraceData;
import DIV.gtcsolo.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * StarForge 自作 JEI ページのカテゴリ。8 軌跡 = 8 ページ。
 *
 * <p>vanilla JEI の {@link IRecipeCategory} を直接 implement (lowdraglib 非依存)。
 * 描画はテキスト中心、シンプル構成。
 */
public class StarForgeInfoCategory implements IRecipeCategory<StarForgeInfoWrapper> {

    public static final RecipeType<StarForgeInfoWrapper> RECIPE_TYPE = new RecipeType<>(
            new ResourceLocation(Gtcsolo.MODID, "starforge_info"),
            StarForgeInfoWrapper.class);

    private static final int WIDTH  = 162;
    private static final int HEIGHT = 160;
    private static final int LINE_HEIGHT = 10;

    private final IDrawable background;
    private final IDrawable icon;

    public StarForgeInfoCategory(IJeiHelpers helpers) {
        IGuiHelper g = helpers.getGuiHelper();
        this.background = g.createBlankDrawable(WIDTH, HEIGHT);
        // アイコン: star_locus (代表アイテム)
        this.icon = g.createDrawableItemStack(new ItemStack(ModItems.STAR_LOCUS.get()));
    }

    @Override
    public @NotNull RecipeType<StarForgeInfoWrapper> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("gtcsolo.jei.starforge_info.title");
    }

    @Override
    public @NotNull IDrawable getBackground() { return background; }

    @Override
    public @NotNull IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder,
                          @NotNull StarForgeInfoWrapper recipe,
                          @NotNull IFocusGroup focuses) {
        // 該当軌跡の star_locus を catalyst スロットに置く (検索でマッチさせるため)
        ItemStack tracedLocus = DIV.gtcsolo.item.AbstractLocusItem.of(
                ModItems.STAR_LOCUS.get(), recipe.info.trace);
        builder.addSlot(RecipeIngredientRole.CATALYST, 4, 4)
                .addItemStack(tracedLocus);
    }

    @Override
    public void draw(@NotNull StarForgeInfoWrapper recipe,
                     @NotNull mezz.jei.api.gui.ingredient.IRecipeSlotsView slotsView,
                     @NotNull GuiGraphics gui,
                     double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int x = 24, y = 6;

        // 軌跡名 + 区分
        Component name = Component.translatable(recipe.info.displayKey);
        gui.drawString(font, name.copy().withStyle(ChatFormatting.GOLD), x, y, 0xFFFFFF, false);
        y += LINE_HEIGHT + 2;

        Component kindLabel = Component.translatable(
                "gtcsolo.jei.starforge_info.kind." + recipe.info.kind.name().toLowerCase());
        gui.drawString(font, kindLabel.copy().withStyle(ChatFormatting.GRAY), x, y, 0xFFFFFF, false);
        y += LINE_HEIGHT + 4;
        x = 4;

        y = drawSection(gui, font, x, y, "gtcsolo.jei.starforge_info.section.starter",
                ChatFormatting.AQUA, recipe.info.starterItemHints);

        y = drawSection(gui, font, x, y, "gtcsolo.jei.starforge_info.section.continuous",
                ChatFormatting.YELLOW, recipe.info.continuousItemHints);

        if (recipe.info.requiredAmount > 0) {
            gui.drawString(font, Component.translatable(
                            "gtcsolo.jei.starforge_info.required_amount", recipe.info.requiredAmount)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), x + 4, y, 0xFFFFFF, false);
            y += LINE_HEIGHT;
        }

        if (recipe.info.maturityDurationTicks > 0 || recipe.info.maturityEUt > 0) {
            gui.drawString(font, Component.translatable(
                            "gtcsolo.jei.starforge_info.maturity",
                            recipe.info.maturityDurationTicks, recipe.info.maturityEUt)
                    .withStyle(ChatFormatting.RED), x + 4, y, 0xFFFFFF, false);
            y += LINE_HEIGHT;
        }

        y = drawSection(gui, font, x, y, "gtcsolo.jei.starforge_info.section.output",
                ChatFormatting.GREEN, recipe.info.outputHints);

        // フッタ note (kind 別)
        Component note = Component.translatable(recipe.info.noteKey)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        gui.drawString(font, note, x, y + 2, 0xFFFFFF, false);
    }

    private static int drawSection(GuiGraphics gui, Font font, int x, int y,
                                   String headerKey, ChatFormatting headerColor,
                                   java.util.List<String> hints) {
        if (hints.isEmpty()) return y;
        gui.drawString(font, Component.translatable(headerKey).withStyle(headerColor),
                x, y, 0xFFFFFF, false);
        y += LINE_HEIGHT;
        for (String h : hints) {
            gui.drawString(font, "  " + h, x + 4, y, 0xAAAAAA, false);
            y += LINE_HEIGHT;
        }
        y += 2;
        return y;
    }

    /** Plugin 側から呼ぶ。8 軌跡分の Wrapper を JEI に登録 */
    public static void registerRecipes(IRecipeRegistration registry) {
        registry.addRecipes(RECIPE_TYPE,
                StarForgeTraceData.all().stream()
                        .map(StarForgeInfoWrapper::new)
                        .toList());
    }
}
