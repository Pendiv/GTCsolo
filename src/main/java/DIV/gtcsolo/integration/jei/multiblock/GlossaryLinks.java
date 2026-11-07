package DIV.gtcsolo.integration.jei.multiblock;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Link the first occurrence of each term, preserving translated arguments and legacy formatting. */
public final class GlossaryLinks {
    public record Term(String pageId, String text, String title) {}
    private record Span(int start, int end, Style style) {}
    private static final int LINK_COLOR = 0x2255aa;
    private GlossaryLinks() {}

    public static List<Term> currentTerms() {
        var terms = new ArrayList<>(GlossaryCatalog.terms(key -> Component.translatable(key).getString()));
        for (var page : GlossaryCatalog.pages()) {
            if (page.isIndex()) continue;
            String title = DocumentationEdits.store(DocumentationEdits.language()).get("glossary/" + page.id()).title();
            if (title == null || title.isBlank()) continue;
            title = net.minecraft.ChatFormatting.stripFormatting(title);
            String label = title;
            terms.replaceAll(term -> term.pageId().equals(page.id()) ? new Term(term.pageId(), term.text(), label) : term);
            if (!title.isBlank() && terms.stream().noneMatch(term -> term.text().equalsIgnoreCase(label)))
                terms.add(new Term(page.id(), title, title));
        }
        return terms.stream().sorted(Comparator.comparingInt((Term term) -> term.text().length()).reversed()).toList();
    }

    public static Component decorate(Component source, List<Term> terms, Set<String> linked, String currentPage) {
        var plain = new StringBuilder();
        var spans = new ArrayList<Span>();
        source.visit((style, text) -> {
            StringDecomposer.iterateFormatted(text, style, (index, glyphStyle, codePoint) -> {
                int start = plain.length();
                plain.appendCodePoint(codePoint);
                int last = spans.size() - 1;
                if (last >= 0 && spans.get(last).style().equals(glyphStyle)) {
                    var previous = spans.get(last);
                    spans.set(last, new Span(previous.start(), plain.length(), glyphStyle));
                } else spans.add(new Span(start, plain.length(), glyphStyle));
                return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        String text = plain.toString();
        MutableComponent result = Component.empty();
        int unchanged = 0, position = 0;
        while (position < text.length()) {
            Term found = null;
            for (var term : terms) {
                int end = position + term.text().length();
                if (end > text.length() || !text.regionMatches(true, position, term.text(), 0, term.text().length())) continue;
                if (latinWord(term.text().charAt(0)) && position > 0 && latinWord(text.charAt(position - 1))) continue;
                if (latinWord(term.text().charAt(term.text().length() - 1)) && end < text.length() && latinWord(text.charAt(end))) continue;
                found = term;
                break;
            }
            if (found == null) {
                position += Character.charCount(text.codePointAt(position));
                continue;
            }
            int end = position + found.text().length();
            int from = position;
            boolean protectedText = spans.stream().anyMatch(span -> span.start() < end && span.end() > from &&
                    (span.style().getClickEvent() != null || span.style().isObfuscated()));
            if (found.pageId().equals(currentPage) || linked.contains(found.pageId()) || protectedText) {
                // Still consume the longest phrase: a repeated industrial OC must not turn its
                // inner "OC" into a misleading link to ordinary overclocking.
                position = end;
                continue;
            }
            append(result, text, spans, unchanged, position, null);
            append(result, text, spans, position, end, found);
            linked.add(found.pageId());
            unchanged = position = end;
        }
        append(result, text, spans, unchanged, text.length(), null);
        return result;
    }

    private static boolean latinWord(char value) {
        return value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z' || value == '_';
    }

    private static void append(MutableComponent output, String text, List<Span> spans, int start, int end, Term term) {
        for (var span : spans) {
            int left = Math.max(start, span.start()), right = Math.min(end, span.end());
            if (left >= right) continue;
            MutableComponent part = Component.literal(text.substring(left, right)).setStyle(span.style());
            if (term == null) output.append(part);
            else {
                var button = ComponentPanelWidget.withButton(part.withStyle(style -> style.withUnderlined(true)),
                        "glossary:" + term.pageId(), LINK_COLOR);
                output.append(button.copy().withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("gtcsolo.glossary.link_hint", term.title())))));
            }
        }
    }
}
