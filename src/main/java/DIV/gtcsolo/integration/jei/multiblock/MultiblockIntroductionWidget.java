package DIV.gtcsolo.integration.jei.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidStackHandler;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

/** Native GT preview above an independently clipped, vertically scrolling document. */
public final class MultiblockIntroductionWidget extends WidgetGroup {
    public static final int WIDTH = 232;
    public static final int HEIGHT = 260;
    private static final int DOCUMENT_Y = 162;
    private static final int SELECTOR_TEXT = 0xfff2f2f2, SELECTOR_BACKGROUND = 0xff30343b;
    private static final int SELECTOR_BORDER = 0xff9199a4, SELECTOR_SELECTED = 0xff48627d;
    private final MultiblockIntroduction introduction;
    private final WidgetGroup previewArea;
    private final DraggableScrollableWidgetGroup document;
    private final int documentY;
    private final int headerHeight;
    private final WidgetGroup editToolbar;
    private long documentRevision = -1;
    private String documentLanguage = "";
    private final Map<MultiblockMachineDefinition, WidgetGroup> previews = new HashMap<>();
    private final Function<MultiblockMachineDefinition, WidgetGroup> previewFactory;
    private SelectorWidget selector;
    private List<String> labels = List.of();
    private int selected = -1;

    public MultiblockIntroductionWidget(MultiblockIntroduction introduction) {
        this(introduction, definition -> new IntroductionPatternPreview(definition, !introduction.isGrouped()));
    }

    MultiblockIntroductionWidget(MultiblockIntroduction introduction,
                                Function<MultiblockMachineDefinition, WidgetGroup> previewFactory) {
        super(0, 0, WIDTH, HEIGHT);
        this.introduction = introduction;
        this.previewFactory = previewFactory;
        setClientSideWidget();
        boolean namedGroup = introduction.isGrouped() && !introduction.pageId().isEmpty();
        headerHeight = introduction.isGrouped() ? (namedGroup ? 34 : 20) : 0;
        documentY = introduction.isSingleBlock() ? 52 : DOCUMENT_Y;
        previewArea = new WidgetGroup(0, headerHeight, WIDTH, documentY - 2);
        previewArea.setClientSideWidget();
        addWidget(previewArea);
        document = new DraggableScrollableWidgetGroup(0, documentY + headerHeight, WIDTH, HEIGHT - documentY - headerHeight)
                .setYScrollBarWidth(4)
                .setYBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON)
                .setScrollable(true)
                .setDraggable(false)
                .setUseScissor(true);
        document.setBackground(new ColorRectTexture(0xffeeeeee));
        document.setClientSideWidget();
        addWidget(document);
        editToolbar = DocumentationEditing.toolbar(WIDTH, HEIGHT - 20,
                () -> DocumentationEditing.edit(DocumentationPage.introduction(introduction, selected),
                        () -> MultiblockIntroductionCategory.openVariant(introduction, selected)));
        addWidget(editToolbar);
        selectVariant(0);
        if (introduction.isGrouped()) {
            if (namedGroup) addWidget(new ImageWidget(0, 0, WIDTH, 12,
                    new GuiTextureGroup(new ColorRectTexture(SELECTOR_BACKGROUND),
                            new TextTexture(Component.translatable("gtcsolo.jei.introduction.group." + introduction.pageId()).getString(),
                                    SELECTOR_TEXT).setWidth(WIDTH).setDropShadow(false))));
            labels = introduction.variants().stream()
                    .map(v -> Component.translatable(v.definition().getDescriptionId()).getString()).toList();
            selector = new SelectorWidget(0, headerHeight - 20, WIDTH, 18, labels, SELECTOR_TEXT)
                    // LDLib's constructor colors the closed label but leaves the popup fontColor at its default.
                    .setFontColor(SELECTOR_TEXT)
                    .setButtonBackground(new ColorRectTexture(SELECTOR_BACKGROUND), new ColorBorderTexture(1, SELECTOR_BORDER))
                    .setBackground(new GuiTextureGroup(new ColorRectTexture(SELECTOR_BACKGROUND), new ColorBorderTexture(1, SELECTOR_BORDER)))
                    .setValue(labels.get(selected))
                    .setOnChanged(label -> {
                        int index = labels.indexOf(label);
                        if (index >= 0) {
                            selectVariant(index);
                            // Rebuild JEI's slot bindings too, so tooltips and R/U use this machine's parts.
                            MultiblockIntroductionCategory.openVariant(introduction, index);
                        }
                    });
            selector.textTexture.setDropShadow(false);
            for (var row : selector.getWidgetsByType(SelectableWidgetGroup.class)) {
                row.setOverlayUnderWidgets(true);
                row.setSelectedTexture(new GuiTextureGroup(new ColorRectTexture(SELECTOR_SELECTED),
                        new ColorBorderTexture(1, SELECTOR_BORDER)));
                row.setHoverTexture(new ColorRectTexture(0xff3d4b5c));
            }
            for (var button : selector.getWidgetsByType(ButtonWidget.class))
                button.setHoverBorderTexture(1, SELECTOR_TEXT);
            selector.setClientSideWidget();
            addWidget(selector); // popup must draw and receive input above the preview
        }
    }

    void selectVariant(int index) {
        if (index == selected) return;
        if (introduction.isSingleBlock()) {
            selected = 0;
            previewArea.clearAllWidgets();
            previewArea.addWidget(new ImageWidget((WIDTH - 40) / 2, 4, 40, 40,
                    new ItemStackTexture(introduction.subject(0))));
            rebuildDocument();
            return;
        }
        var variant = introduction.variants().get(index);
        selected = index;
        previewArea.clearAllWidgets();
        previewArea.addWidget(previews.computeIfAbsent(variant.definition(), previewFactory));
        rebuildDocument();
        if (selector != null) selector.setValue(labels.get(index));
    }

    @Override public void updateScreen() {
        refreshDocument();
        super.updateScreen();
    }

    void refreshDocument() {
        updateEditorControls();
        if (documentRevision != DocumentationEdits.revision() || !documentLanguage.equals(DocumentationEdits.language()))
            rebuildDocument();
    }

    private void updateEditorControls() {
        boolean editing = DocumentationEditing.selecting(DocumentationEditing.Kind.INTRODUCTION);
        editToolbar.setVisible(editing).setActive(editing);
        int height = HEIGHT - documentY - headerHeight - (editing ? 20 : 0);
        if (document.getSize().height != height) { document.setSizeHeight(height); document.computeMax(); }
    }

    private void rebuildDocument() {
        int documentOffset = document.getScrollYOffset();
        documentRevision = DocumentationEdits.revision(); documentLanguage = DocumentationEdits.language();
        updateEditorControls();
        document.clearAllWidgets();
        var page = DocumentationEdits.resolve(DocumentationPage.introduction(introduction, selected));
        var paragraphs = new ArrayList<Component>();
        var details = new ArrayList<Component>();
        var terms = GlossaryLinks.currentTerms();
        var linked = new HashSet<String>();
        if (!introduction.isGrouped() || !page.title().getString().equals(DocumentationPage.introduction(introduction, selected).title().getString()))
            (page.hasFluidSection() && introduction.fluidSection().afterParagraph() == 0 ? details : paragraphs)
                    .add(page.title().copy().withStyle(ChatFormatting.BLACK));
        for (var paragraph : page.body()) paragraphs.add(GlossaryLinks.decorate(
                paragraph.copy().withStyle(ChatFormatting.BLACK), terms, linked, ""));
        for (var paragraph : page.details()) details.add(GlossaryLinks.decorate(
                paragraph.copy().withStyle(ChatFormatting.BLACK), terms, linked, ""));
        if (!introduction.relatedPages().isEmpty()) {
            var links = Component.empty();
            for (var link : introduction.relatedPages()) {
                if (!links.getSiblings().isEmpty()) links.append(Component.literal(" / ").withStyle(ChatFormatting.BLACK));
                links.append(ComponentPanelWidget.withButton(
                        Component.translatable(link.labelKey()).withStyle(ChatFormatting.UNDERLINE),
                        link.targetId(), 0x2255aa));
            }
            (page.hasFluidSection() ? details : paragraphs).add(links);
        }
        if (!page.hasFluidSection()) {
            addText(4, paragraphs);
        } else {
            int y = addText(4, paragraphs) + 4;
            y = addFluidOutputs(y);
            addText(y + 4, details);
        }
        document.computeMax();
        document.setScrollYOffset(Math.min(documentOffset,
                Math.max(0, document.getWidgetBottomHeight() - document.getSize().height)));
    }

    private int addText(int y, List<Component> paragraphs) {
        if (paragraphs.isEmpty()) return y;
        var text = new DocumentationTextPanel(5, y, paragraphs)
                .setMaxWidthLimit(WIDTH - 16).setSpace(2);
        text.clickHandler((pageId, clickData) -> MultiblockIntroductionCategory.openPage(pageId));
        text.setClientSideWidget();
        document.addWidget(text);
        return y + text.getSize().height;
    }

    private int addFluidOutputs(int y) {
        var groups = introduction.fluidSection().groups();
        int columns = Math.min(3, groups.size());
        int width = (WIDTH - 16) / columns;
        for (int row = 0; row * columns < groups.size(); row++) {
            int bottom = y;
            for (int column = 0; column < columns && row * columns + column < groups.size(); column++) {
                var group = groups.get(row * columns + column);
                int x = 5 + column * width;
                var tank = new TankWidget(new CycleFluidStackHandler(List.of(group.fluids())),
                        x + (width - 18) / 2, y, false, false)
                        .setShowAmount(false).setIngredientIO(IngredientIO.OUTPUT);
                tank.setBackground(new GuiTextureGroup(new ColorRectTexture(0xffdddddd),
                        new ColorBorderTexture(1, 0xff777777)));
                tank.setClientSideWidget();
                document.addWidget(tank);
                var label = new DocumentationTextPanel(x, y + 20,
                        List.of(Component.translatable(group.labelKey()).withStyle(ChatFormatting.BLACK)))
                        .setMaxWidthLimit(width - 4).setCenter(true).setSpace(2);
                label.setClientSideWidget();
                document.addWidget(label);
                bottom = Math.max(bottom, y + 20 + label.getSize().height);
            }
            y = bottom + 4;
        }
        return y;
    }

}
