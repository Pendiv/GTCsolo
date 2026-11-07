package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

/** Drafts are private to this screen. Only the explicit Save action touches the file. */
public final class DocumentationEditorScreen extends Screen {
    private final Screen parent;
    private final DocumentationPage page;
    private final DocumentationStore store;
    private final String language;
    private final Runnable afterSave;
    private final DocumentationStore.Text expected, initial;
    private String draftTitle, draftBody, draftDetails;
    private boolean details;
    private MultiLineEditBox editor;
    private EditBox titleEditor;
    private Component error = Component.empty();
    private int left, areaWidth;

    public DocumentationEditorScreen(Screen parent, DocumentationPage page, DocumentationStore store, String language,
                                     Runnable afterSave) {
        super(Component.translatable("gtcsolo.documentation.edit_title"));
        this.parent = parent; this.page = page; this.store = store; this.language = language;
        this.afterSave = afterSave;
        expected = store.get(page.id()); initial = expected.apply(page.text());
        setDraft(initial);
        if (store.loadError() != null) error = Component.translatable("gtcsolo.documentation.read_error");
    }

    private void setDraft(DocumentationStore.Text text) {
        draftTitle = text.title(); draftBody = text.body(); draftDetails = text.details();
    }
    private DocumentationStore.Text draft() { return new DocumentationStore.Text(draftTitle, draftBody, draftDetails); }

    @Override protected void init() {
        areaWidth = Math.min(740, width - 40); left = (width - areaWidth) / 2;
        titleEditor = addRenderableWidget(new EditBox(font, left, 40, areaWidth, 20,
                Component.translatable("gtcsolo.documentation.heading")));
        titleEditor.setMaxLength(DocumentationStore.TEXT_LIMIT);
        titleEditor.setValue(draftTitle);
        titleEditor.setResponder(value -> draftTitle = value);
        int top = page.hasFluidSection() ? 90 : 78;
        if (page.hasFluidSection()) {
            var overview = addRenderableWidget(Button.builder(Component.translatable("gtcsolo.documentation.before_fluids"),
                    button -> switchSection(false)).bounds(left, 64, 132, 20).build());
            var detail = addRenderableWidget(Button.builder(Component.translatable("gtcsolo.documentation.after_fluids"),
                    button -> switchSection(true)).bounds(left + 136, 64, 132, 20).build());
            overview.active = details; detail.active = !details;
        }
        editor = addRenderableWidget(new MultiLineEditBox(font, left, top, areaWidth - 8,
                Math.max(40, height - top - 85), Component.empty(),
                Component.translatable("gtcsolo.documentation.body")));
        editor.setCharacterLimit(DocumentationStore.TEXT_LIMIT);
        editor.setValue(details ? draftDetails : draftBody);
        editor.setValueListener(value -> { if (details) draftDetails = value; else draftBody = value; });
        int buttonWidth = Math.min(100, (areaWidth - 12) / 3);
        var save = addRenderableWidget(Button.builder(Component.translatable("gtcsolo.documentation.save"), button -> save())
                .bounds(left, height - 28, buttonWidth, 20).build());
        save.active = store.loadError() == null;
        addRenderableWidget(Button.builder(Component.translatable("gtcsolo.documentation.discard"), button -> closeEditor())
                .bounds(left + buttonWidth + 6, height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gtcsolo.documentation.restore"), button -> {
            setDraft(page.text()); rebuildWidgets();
        }).bounds(left + (buttonWidth + 6) * 2, height - 28, buttonWidth, 20).build());
        setInitialFocus(editor);
    }

    private void switchSection(boolean details) { this.details = details; rebuildWidgets(); }

    private void save() {
        if (saveDraft()) {
            closeEditor();
            // JEI caches ingredient slot positions separately from the widget's text layout.
            afterSave.run();
        }
    }

    boolean saveDraft() {
        try {
            store.save(page.id(), expected, draft().difference(page.text()));
            DocumentationEdits.changed();
            return true;
        } catch (IOException failure) {
            Gtcsolo.LOGGER.error("Cannot save documentation {} to {}", page.id(), store.file(), failure);
            error = Component.translatable("gtcsolo.documentation.save_error");
            return false;
        }
    }

    private void closeEditor() {
        DocumentationEditing.editorClosed();
        minecraft.setScreen(parent);
    }
    @Override public void onClose() {
        if (draft().equals(initial)) closeEditor();
        else minecraft.setScreen(new ConfirmScreen(discard -> {
            if (discard) closeEditor(); else minecraft.setScreen(this);
        }, Component.translatable("gtcsolo.documentation.discard_question"), Component.empty(),
                Component.translatable("gtcsolo.documentation.discard"), Component.translatable("gui.cancel")));
    }
    @Override public void tick() { editor.tick(); titleEditor.tick(); }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xffffff);
        graphics.drawString(font, Component.translatable("gtcsolo.documentation.heading"), left, 28, 0xc0c0c0, false);
        graphics.drawString(font, language, left + areaWidth - font.width(language), 28, 0xc0c0c0, false);
        if (!page.hasFluidSection()) graphics.drawString(font, Component.translatable("gtcsolo.documentation.body"), left, 65, 0xc0c0c0, false);
        if (page.hasLiveValues()) graphics.drawWordWrap(font, Component.translatable("gtcsolo.documentation.fixed_values"),
                left, height - 60, areaWidth, 0xe0c080);
        graphics.drawString(font, error, left, height - 40, 0xff8080, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
