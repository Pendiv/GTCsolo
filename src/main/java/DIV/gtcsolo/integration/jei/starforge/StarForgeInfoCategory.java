package DIV.gtcsolo.integration.jei.starforge;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.machine.starforge.PhaseProgressionTable;
import DIV.gtcsolo.machine.starforge.StarForgeTraceData;
import DIV.gtcsolo.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.world.item.Item;
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
    private static final int SLOT_W = 18;
    private static final int BUILD_INPUT_Y = 56;   // 構築入力 slot 列の Y
    private static final int DECAY_INPUT_Y = 104;  // 崩壊入力 slot 列の Y

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

        // 構築フェイズの入力アイテム群を INPUT slot として並べる
        // (= 仕様に従い「同じ table を使う場合は崩壊フェイズも繰り返し表記」、 親切表記なし)
        layoutPhaseInputs(builder, recipe.info.buildPhaseTable, 4, BUILD_INPUT_Y);
        layoutPhaseInputs(builder, recipe.info.decayPhaseTable, 4, DECAY_INPUT_Y);
    }

    /**
     * フェイズ table の Default + Effective を slot として並べる。
     * Effective slot は tooltip に「Effective」 + 「count: +N」 を表示。
     */
    private static void layoutPhaseInputs(IRecipeLayoutBuilder builder,
                                          PhaseProgressionTable table,
                                          int startX, int startY) {
        if (table == null) return;
        int x = startX;
        int y = startY;
        for (Item item : table.getDefaultItems()) {
            if (item == null) continue;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStack(new ItemStack(item));
            x += SLOT_W;
        }
        for (PhaseProgressionTable.EffectiveEntry e : table.getEffectiveEntries()) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStack(e.displayStack);
            // tooltip callback: Effective + count: +N
            slot.addRichTooltipCallback((view, tooltip) -> {
                tooltip.add(Component.translatable("gtcsolo.jei.starforge_info.effective")
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable(
                                "gtcsolo.jei.starforge_info.count_plus", e.value)
                        .withStyle(ChatFormatting.AQUA));
            });
            x += SLOT_W;
        }
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

        // 構築フェイズ
        if (recipe.info.buildPhaseTable != null) {
            gui.drawString(font, Component.translatable("gtcsolo.jei.starforge_info.section.build")
                    .withStyle(ChatFormatting.AQUA), x, BUILD_INPUT_Y - LINE_HEIGHT - 2, 0xFFFFFF, false);
            gui.drawString(font, Component.translatable(
                            "gtcsolo.jei.starforge_info.required_count", recipe.info.buildRequiredCount)
                    .withStyle(ChatFormatting.LIGHT_PURPLE),
                    x, BUILD_INPUT_Y + SLOT_W + 1, 0xFFFFFF, false);
        }

        // 崩壊フェイズ
        if (recipe.info.decayPhaseTable != null) {
            gui.drawString(font, Component.translatable("gtcsolo.jei.starforge_info.section.decay")
                    .withStyle(ChatFormatting.YELLOW), x, DECAY_INPUT_Y - LINE_HEIGHT - 2, 0xFFFFFF, false);
            gui.drawString(font, Component.translatable(
                            "gtcsolo.jei.starforge_info.required_count", recipe.info.decayRequiredCount)
                    .withStyle(ChatFormatting.LIGHT_PURPLE),
                    x, DECAY_INPUT_Y + SLOT_W + 1, 0xFFFFFF, false);
        }

        // 成熟フェイズ (太陽/BH のみ)
        if (recipe.info.maturityDurationTicks > 0 || recipe.info.maturityEUt > 0) {
            gui.drawString(font, Component.translatable(
                            "gtcsolo.jei.starforge_info.maturity",
                            recipe.info.maturityDurationTicks, recipe.info.maturityEUt)
                    .withStyle(ChatFormatting.RED), x, HEIGHT - LINE_HEIGHT * 3, 0xFFFFFF, false);
        }
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
