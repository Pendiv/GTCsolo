package DIV.gtcsolo.integration.jei.multiblock;

import java.util.List;

public record GlossaryPage(String id, String titleKey, List<String> paragraphs, List<String> terms,
                           List<MultiblockIntroduction.PageLink> links) {
    public GlossaryPage {
        if (id == null || !id.matches("[a-z0-9_./-]+") || titleKey == null || titleKey.isBlank())
            throw new IllegalArgumentException("Invalid glossary page");
        paragraphs = List.copyOf(paragraphs);
        terms = List.copyOf(terms);
        links = List.copyOf(links);
    }
    public boolean isIndex() { return id.equals("index"); }
    public String target() { return "glossary:" + id; }
}
