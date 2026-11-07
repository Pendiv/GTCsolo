package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin;
import DIV.gtcsolo.integration.jei.performance.NativePreviewIngredientIndex;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class GlossaryCategory extends ModularUIRecipeCategory<GlossaryPage> {
    public static final RecipeType<GlossaryPage> RECIPE_TYPE = new RecipeType<>(
            new ResourceLocation(Gtcsolo.MODID, "glossary"), GlossaryPage.class);
    private static final int WIDTH = 232, HEIGHT = 260;
    private final IDrawable background, icon;

    public GlossaryCategory(IJeiHelpers helpers) {
        super(new LiveRecipeWrapperFactory<GlossaryPage, ModularWrapper<?>>(page -> new ModularWrapper<>(new PageWidget(page)))
                .andThen(wrapper -> { ((PageWidget) wrapper.getWidget()).refreshDocument(); return wrapper; }));
        background = helpers.getGuiHelper().createBlankDrawable(WIDTH, HEIGHT);
        icon = helpers.getGuiHelper().createDrawableItemStack(new ItemStack(Items.BOOK));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RECIPE_TYPE, GlossaryCatalog.load(Minecraft.getInstance().getResourceManager()));
    }

    public static void openPage(String id) {
        var runtime = GtcSoloJeiPlugin.getRuntimeOrNull();
        if (runtime == null) return;
        runtime.getRecipeManager().createRecipeLookup(RECIPE_TYPE).get().filter(page -> page.id().equals(id))
                .findFirst().ifPresent(page -> runtime.getRecipesGui().showRecipes(
                        runtime.getRecipeManager().getRecipeCategory(RECIPE_TYPE), List.of(page), List.of()));
    }

    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull GlossaryPage page, @NotNull IFocusGroup focuses) {
        // Indexing text pages never needs a widget or a preview world.
        if (!NativePreviewIngredientIndex.isIngredientIndexBuilder(builder)) super.setRecipe(builder, page, focuses);
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(new ItemStack(Items.BOOK));
    }
    @Override public @NotNull RecipeType<GlossaryPage> getRecipeType() { return RECIPE_TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("gtcsolo.glossary.title"); }
    @Override public @NotNull IDrawable getBackground() { return background; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    private static final class PageWidget extends WidgetGroup {
        private final GlossaryPage page;
        private final DraggableScrollableWidgetGroup document;
        private final WidgetGroup editToolbar;
        private long documentRevision = -1;
        private String documentLanguage = "";

        PageWidget(GlossaryPage page) {
            super(0, 0, WIDTH, HEIGHT);
            this.page = page;
            setClientSideWidget();
            document = new DraggableScrollableWidgetGroup(0, 0, WIDTH, HEIGHT)
                    .setYScrollBarWidth(4).setYBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON)
                    .setScrollable(true).setDraggable(false).setUseScissor(true);
            document.setBackground(new ColorRectTexture(0xffeeeeee));
            document.setClientSideWidget();
            addWidget(document);
            editToolbar = DocumentationEditing.toolbar(WIDTH, HEIGHT - 20,
                    () -> DocumentationEditing.edit(DocumentationPage.glossary(page), () -> openPage(page.id())));
            addWidget(editToolbar);
            rebuildDocument();
        }

        @Override public void updateScreen() {
            refreshDocument();
            super.updateScreen();
        }

        private void refreshDocument() {
            updateEditorControls();
            if (documentRevision != DocumentationEdits.revision() || !documentLanguage.equals(DocumentationEdits.language()))
                rebuildDocument();
        }

        private void updateEditorControls() {
            boolean editing = DocumentationEditing.selecting(DocumentationEditing.Kind.GLOSSARY);
            editToolbar.setVisible(editing).setActive(editing);
            int height = HEIGHT - (editing ? 20 : 0);
            if (document.getSize().height != height) { document.setSizeHeight(height); document.computeMax(); }
        }

        private void rebuildDocument() {
            int offset = document.getScrollYOffset();
            documentRevision = DocumentationEdits.revision(); documentLanguage = DocumentationEdits.language();
            updateEditorControls();
            document.clearAllWidgets();
            var edited = DocumentationEdits.resolve(DocumentationPage.glossary(page));
            var paragraphs = new ArrayList<Component>();
            paragraphs.add(edited.title().copy().withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
            var terms = GlossaryLinks.currentTerms();
            var seen = new HashSet<String>();
            for (var paragraph : edited.body()) paragraphs.add(GlossaryLinks.decorate(
                    paragraph.copy().withStyle(ChatFormatting.BLACK), terms, seen, page.id()));
            if (page.isIndex()) {
                for (var entry : GlossaryCatalog.pages()) if (!entry.isIndex()) paragraphs.add(Component.literal("• ")
                        .withStyle(ChatFormatting.BLACK).append(ComponentPanelWidget.withButton(
                                DocumentationEdits.resolve(DocumentationPage.glossary(entry)).title().copy()
                                        .withStyle(ChatFormatting.UNDERLINE), entry.target(), 0x2255aa)));
            } else {
                var links = Component.empty();
                for (var link : page.links()) {
                    if (!links.getSiblings().isEmpty()) links.append(Component.literal(" / ").withStyle(ChatFormatting.BLACK));
                    links.append(button(link.targetId(), link.labelKey()));
                }
                if (!links.getSiblings().isEmpty()) paragraphs.add(links);
            }
            var text = new DocumentationTextPanel(5, 4, paragraphs).setMaxWidthLimit(WIDTH - 16).setSpace(2);
            text.clickHandler((id, click) -> MultiblockIntroductionCategory.openPage(id));
            text.setClientSideWidget();
            document.addWidget(text);
            document.computeMax();
            document.setScrollYOffset(Math.min(offset, Math.max(0, document.getWidgetBottomHeight() - document.getSize().height)));
        }
        private static Component button(String target, String label) {
            return ComponentPanelWidget.withButton(Component.translatable(label).withStyle(ChatFormatting.UNDERLINE), target, 0x2255aa);
        }
    }
}
