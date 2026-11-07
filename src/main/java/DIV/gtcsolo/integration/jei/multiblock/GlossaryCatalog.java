package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/** Small resource-backed text catalog; no machines or preview worlds are needed for glossary pages. */
public final class GlossaryCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile List<GlossaryPage> pages = List.of();
    private GlossaryCatalog() {}

    public static List<GlossaryPage> load(ResourceManager resources) {
        var result = new ArrayList<GlossaryPage>();
        var sources = resources.listResources("glossary", id -> id.getNamespace().equals(Gtcsolo.MODID) && id.getPath().endsWith(".json"));
        for (var source : sources.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).toList()) {
            try (var reader = source.getValue().openAsReader()) {
                result.addAll(parse(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (IOException | RuntimeException error) {
                LOGGER.error("Could not load glossary {}", source.getKey(), error);
            }
        }
        install(result);
        return pages;
    }

    static List<GlossaryPage> parse(JsonObject root) {
        var result = new ArrayList<GlossaryPage>();
        for (var raw : root.getAsJsonArray("entries")) {
            var entry = raw.getAsJsonObject();
            var links = new ArrayList<MultiblockIntroduction.PageLink>();
            if (entry.has("links")) for (var link : entry.getAsJsonArray("links")) {
                var value = link.getAsJsonObject();
                links.add(new MultiblockIntroduction.PageLink(value.get("target").getAsString(), value.get("label").getAsString()));
            }
            result.add(new GlossaryPage(entry.get("id").getAsString(), entry.get("title").getAsString(),
                    strings(entry.getAsJsonArray("paragraphs")), strings(entry.getAsJsonArray("terms")), links));
        }
        return List.copyOf(result);
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) return List.of();
        var result = new ArrayList<String>();
        for (var element : array) {
            String value = element.getAsString();
            if (value.isBlank()) throw new IllegalArgumentException("Empty glossary translation key");
            result.add(value);
        }
        return result;
    }

    static void install(List<GlossaryPage> entries) {
        var ids = new HashSet<String>();
        ids.add("index");
        for (var page : entries) if (!ids.add(page.id()))
            throw new IllegalArgumentException("Duplicate glossary page: " + page.id());
        var result = new ArrayList<GlossaryPage>();
        result.add(new GlossaryPage("index", "gtcsolo.glossary.index.title",
                List.of("gtcsolo.glossary.index.text"), List.of(), List.of()));
        result.addAll(entries);
        pages = List.copyOf(result);
    }

    public static List<GlossaryPage> pages() { return pages; }
    public static boolean hasPage(String id) { return pages.stream().anyMatch(page -> page.id().equals(id)); }
    public static boolean isGlossaryTarget(String target) {
        return target.startsWith("glossary:") && hasPage(target.substring("glossary:".length()));
    }
    public static void clear() { pages = List.of(); }

    public static void validateLinks(Set<String> introductionIds) {
        for (var page : pages) for (var link : page.links()) {
            if (!isGlossaryTarget(link.targetId()) && !introductionIds.contains(link.targetId()))
                throw new IllegalStateException("Missing glossary destination: " + link.targetId());
        }
    }

    static List<GlossaryLinks.Term> terms(Function<String, String> translate) {
        var terms = new LinkedHashMap<String, GlossaryLinks.Term>();
        var ambiguous = new HashSet<String>();
        for (var page : pages) {
            if (page.isIndex()) continue;
            var keys = new ArrayList<>(page.terms());
            keys.add(page.titleKey());
            String title = translate.apply(page.titleKey());
            for (var key : keys) {
                String text = translate.apply(key);
                if (text == null || text.equals(key)) continue;
                text = ChatFormatting.stripFormatting(text).strip();
                if (text.isEmpty()) continue;
                String folded = text.toLowerCase(Locale.ROOT);
                if (ambiguous.contains(folded)) continue;
                var term = new GlossaryLinks.Term(page.id(), text, title);
                var previous = terms.putIfAbsent(folded, term);
                if (previous != null && !previous.pageId().equals(page.id())) {
                    terms.remove(folded);
                    ambiguous.add(folded);
                    LOGGER.warn("Ambiguous glossary term: {}", text);
                }
            }
        }
        return terms.values().stream().sorted(Comparator.comparingInt((GlossaryLinks.Term term) -> term.text().length())
                .reversed().thenComparing(GlossaryLinks.Term::pageId)).toList();
    }
}
