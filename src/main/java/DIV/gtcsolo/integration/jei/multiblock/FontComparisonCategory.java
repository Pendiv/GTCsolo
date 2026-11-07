package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Temporary, local font comparison: no global font or resource-pack changes. */
public final class FontComparisonCategory extends ModularUIRecipeCategory<FontComparisonCategory.Page> {
    private static final int WIDTH = 232;
    private static final int HEIGHT = 260;
    private static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");
    private static final ResourceLocation UNIFORM_FONT = new ResourceLocation("minecraft", "uniform");
    public static final RecipeType<Page> RECIPE_TYPE = new RecipeType<>(
            new ResourceLocation(Gtcsolo.MODID, "font_comparison"), Page.class);
    private final IDrawable background;
    private final IDrawable icon;

    public record Page(ResourceLocation font, String nameKey, boolean dark) {}

    public FontComparisonCategory(IJeiHelpers helpers) {
        super(page -> new ModularWrapper<>(new ComparisonWidget(page)));
        background = helpers.getGuiHelper().createBlankDrawable(WIDTH, HEIGHT);
        icon = helpers.getGuiHelper().createDrawableItemStack(new ItemStack(Items.BOOK));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<Page> pages = new ArrayList<>();
        for (boolean dark : new boolean[]{false, true}) {
            pages.add(new Page(DEFAULT_FONT, "gtcsolo.font_compare.default", dark));
            pages.add(new Page(UNIFORM_FONT, "gtcsolo.font_compare.uniform", dark));
        }
        registration.addRecipes(RECIPE_TYPE, pages);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
        super.setRecipe(builder, page, focuses);
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(new ItemStack(Items.BOOK));
    }
    @Override public RecipeType<Page> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("gtcsolo.font_compare.title"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    private static final class ComparisonWidget extends WidgetGroup {
        private int nextY = 4;
        private final Page page;
        private final DraggableScrollableWidgetGroup document;

        private ComparisonWidget(Page page) {
            super(0, 0, WIDTH, HEIGHT);
            setClientSideWidget();
            this.page = page;
            document = new DraggableScrollableWidgetGroup(0, 0, WIDTH, HEIGHT)
                    .setYScrollBarWidth(4).setYBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON)
                    .setScrollable(true).setDraggable(false).setUseScissor(true);
            document.setClientSideWidget();
            document.setBackground(new ColorRectTexture(page.dark() ? 0xff202020 : 0xffeeeeee));
            addWidget(document);
            String name = Component.translatable(page.nameKey()).getString();
            String backdrop = Component.translatable(page.dark() ? "gtcsolo.font_compare.dark" : "gtcsolo.font_compare.light").getString();
            sample(name + " / " + backdrop, page.font().toString(), 1, false);
            String text = Component.translatable("gtcsolo.font_compare.sample").getString();
            for (float size : new float[]{1, 1.5f}) {
                sample("x" + size + " / " + Component.translatable("gtcsolo.font_compare.no_shadow").getString(), text, size, false);
                sample("x" + size + " / " + Component.translatable("gtcsolo.font_compare.shadow").getString(), text, size, true);
            }
            String paragraph = Component.translatable("gtcsolo.lcr.introduction.1").getString();
            for (float size : new float[]{1, 1.5f})
                sample("x" + size + " / " + Component.translatable("gtcsolo.font_compare.paragraph").getString(),
                        paragraph, size, false);
            for (String code : new String[]{"l", "o", "n", "m", "k"}) {
                String value = code.equals("k") ? "ABC123" : text;
                sample("§" + code + " / " + Component.translatable("gtcsolo.font_compare.code." + code).getString(),
                        "§" + code + value + "§r", 1.5f, false);
            }
            sample("§r / " + Component.translatable("gtcsolo.font_compare.code.r").getString(),
                    "§c§lABC §rABC 123", 1.5f, false);
            sample("§6§l / " + Component.translatable("gtcsolo.font_compare.combined").getString(),
                    "§6§l" + text + "§r", 1.5f, false);
            for (char color : "0123456789abcdef".toCharArray())
                sample("§" + color, "§" + color + text + "§r", 1, false);
        }

        private void sample(String label, String text, float scale, boolean shadow) {
            SampleWidget widget = new SampleWidget(5, nextY, WIDTH - 16, page, label, text, scale, shadow);
            document.addWidget(widget);
            nextY += widget.getSize().height + 7;
        }
    }

    /** Labels render § literally; the sample uses Minecraft's real legacy-format parser. */
    private static final class SampleWidget extends Widget {
        private final String label;
        private final List<FormattedCharSequence> lines;
        private final float scale;
        private final boolean shadow;
        private final int color;

        private SampleWidget(int x, int y, int width, Page page, String label, String text, float scale, boolean shadow) {
            super(x, y, width, 1);
            setClientSideWidget();
            this.label = label;
            this.scale = scale;
            this.shadow = shadow;
            color = page.dark() ? 0xffffffff : 0xff000000;
            var font = Minecraft.getInstance().font;
            Component component = Component.literal(text).withStyle(style -> style.withFont(page.font()));
            lines = font.split(component, Math.max(1, (int) (width / scale)));
            setSize(width, 13 + (int) Math.ceil(lines.size() * (font.lineHeight + 2) * scale));
        }

        @Override public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            var font = Minecraft.getInstance().font;
            var position = getPosition();
            graphics.drawString(font, FormattedCharSequence.forward(label, Style.EMPTY.withFont(DEFAULT_FONT)),
                    position.x, position.y, color, false);
            var pose = graphics.pose();
            pose.pushPose();
            try {
                pose.translate(position.x, position.y + 13, 0);
                pose.scale(scale, scale, 1);
                for (int i = 0; i < lines.size(); i++)
                    graphics.drawString(font, lines.get(i), 0, i * (font.lineHeight + 2), color, shadow);
            } finally {
                pose.popPose();
            }
        }
    }
}
