package DIV.gtcsolo.integration.jei.multiblock;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

/** Editable text sections; structural previews, fluid slots and navigation stay separate. */
public record DocumentationPage(String id, Component title, List<Component> body, List<Component> details,
                                boolean hasFluidSection, boolean hasLiveValues) {
    public DocumentationPage { body = List.copyOf(body); details = List.copyOf(details); }

    public static DocumentationPage glossary(GlossaryPage page) {
        return new DocumentationPage("glossary/" + page.id(), Component.translatable(page.titleKey()),
                page.paragraphs().stream().<Component>map(Component::translatable).toList(), List.of(), false, false);
    }

    public static DocumentationPage introduction(MultiblockIntroduction page, int variantIndex) {
        var source = page.paragraphs(variantIndex);
        var paragraphs = source.stream().map(MultiblockIntroduction.Paragraph::component).toList();
        boolean illustrated = !page.fluidSection().groups().isEmpty();
        int split = illustrated ? Math.max(1, page.fluidSection().afterParagraph()) : paragraphs.size();
        var id = page.isSingleBlock() ? page.singleBlock().id() : page.variants().get(variantIndex).definition().getId();
        return new DocumentationPage("multiblock/" + id, paragraphs.get(0),
                paragraphs.subList(1, split), illustrated ? paragraphs.subList(split, paragraphs.size()) : List.of(),
                illustrated, source.stream().anyMatch(p -> !p.arguments().isEmpty()));
    }

    public DocumentationStore.Text text() {
        return new DocumentationStore.Text(title.getString(), join(body), join(details));
    }

    public DocumentationPage apply(DocumentationStore.Text override) {
        return new DocumentationPage(id, override.title() == null ? title : Component.literal(override.title()),
                override.body() == null ? body : lines(override.body()),
                override.details() == null ? details : lines(override.details()), hasFluidSection, hasLiveValues);
    }

    private static String join(List<Component> paragraphs) {
        return paragraphs.stream().map(Component::getString).collect(Collectors.joining("\n"));
    }
    private static List<Component> lines(String text) {
        return text.isEmpty() ? List.of() : text.lines().<Component>map(Component::literal).toList();
    }
}
