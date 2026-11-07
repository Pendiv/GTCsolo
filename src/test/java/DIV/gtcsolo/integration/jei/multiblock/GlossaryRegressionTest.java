package DIV.gtcsolo.integration.jei.multiblock;

import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipFile;

/** Native text/style/button data checks. Does not claim a rendered JEI click test. */
public final class GlossaryRegressionTest {
    public static void main(String[] args) throws Exception {
        if (!GlossaryRegressionTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        Path root = Path.of(args[0]);
        var entries = GlossaryCatalog.parse(JsonParser.parseString(Files.readString(
                root.resolve("src/main/resources/assets/gtcsolo/glossary/core.json"))).getAsJsonObject());
        assert entries.size() == 10;
        GlossaryCatalog.install(entries);
        assert GlossaryCatalog.pages().size() == 11;
        GlossaryCatalog.validateLinks(Set.of("hpca", "research_station", "network_switch"));
        assert GlossaryCatalog.isGlossaryTarget("glossary:computation");
        assert GlossaryCatalog.isGlossaryTarget("glossary:hpca_components");
        assert !GlossaryCatalog.isGlossaryTarget("https://example.invalid");
        assert !GlossaryCatalog.isGlossaryTarget("glossary:missing");
        var duplicates = new ArrayList<>(entries); duplicates.add(entries.get(0));
        try { GlossaryCatalog.install(duplicates); throw new AssertionError("duplicate id accepted"); }
        catch (IllegalArgumentException expected) { /* Preserve the last valid catalog. */ }
        assert GlossaryCatalog.pages().size() == 11;
        int translations = 0;
        for (String locale : List.of("ja_jp", "en_us")) {
            Map<String, String> lang = new HashMap<>();
            try (var jar = new ZipFile(root.resolve("libs/gtceu-1.20.1-1.6.4.jar").toFile())) {
                read(lang, new String(jar.getInputStream(jar.getEntry("assets/gtceu/lang/" + locale + ".json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
            read(lang, Files.readString(root.resolve("src/main/resources/assets/gtcsolo/lang/" + locale + ".json")));
            read(lang, Files.readString(root.resolve("src/main/resources/assets/gtcsolo_glossary/lang/" + locale + ".json")));
            for (var page : GlossaryCatalog.pages()) {
                var keys = new ArrayList<>(page.paragraphs()); keys.add(page.titleKey()); keys.addAll(page.terms());
                for (var link : page.links()) keys.add(link.labelKey());
                for (var key : keys) { assert lang.containsKey(key) : locale + ": " + key; translations++; }
            }
            var terms = GlossaryCatalog.terms(key -> lang.getOrDefault(key, key));
            assert terms.size() > 20;
            Language.inject(new MapLanguage(lang));
            if (locale.equals("ja_jp")) checkJapanese(terms, lang);
            else {
                var input = Component.literal("process OCEAN industrial overclocking industrial overclocking OC");
                var result = GlossaryLinks.decorate(input, terms, new HashSet<>(), "");
                assert result.getString().equals(input.getString());
                assert click(result, input.getString().indexOf("OCEAN")) == null;
                assert click(result, input.getString().indexOf("industrial")).endsWith("industrial_overclocking");
                assert click(result, input.getString().lastIndexOf("industrial")) == null;
                assert click(result, input.getString().lastIndexOf("OC")).endsWith("overclocking");
            }
        }
        System.out.println("PASS: 10 topics + index, both locales (" + translations + " keys), cross-page destinations, "
                + "longest terms, repeated/self-link suppression, word boundaries, formatting and translated arguments.");
    }

    private static void checkJapanese(List<GlossaryLinks.Term> terms, Map<String, String> language) {
        String phrase = "工業的OC 工業的OC OC 入出力ボーナス サブティック並列化";
        var result = GlossaryLinks.decorate(Component.literal(phrase), terms, new HashSet<>(), "");
        assert result.getString().equals(phrase);
        assert click(result, 0).equals("@!glossary:industrial_overclocking");
        assert click(result, phrase.indexOf("工業的OC", 1) + 3) == null;
        assert click(result, phrase.indexOf(" OC ") + 1).equals("@!glossary:overclocking");
        assert click(result, phrase.indexOf("サブティック")).equals("@!glossary:subtick_parallel");
        var own = GlossaryLinks.decorate(Component.literal("工業的OCと通常のOC"), terms, new HashSet<>(), "industrial_overclocking");
        assert click(own, 3) == null && click(own, own.getString().lastIndexOf("OC")).equals("@!glossary:overclocking");
        var seen = new HashSet<String>();
        GlossaryLinks.decorate(Component.literal("計算力"), terms, seen, "");
        assert click(GlossaryLinks.decorate(Component.literal("CWU/t"), terms, seen, ""), 0) == null;
        var colored = GlossaryLinks.decorate(Component.literal("§4注意：工業的OCを利用§r 🔧75%")
                .withStyle(ChatFormatting.BLACK), terms, new HashSet<>(), "");
        assert colored.getString().equals("注意：工業的OCを利用 🔧75%");
        assert style(colored, 0).getColor().getValue() == ChatFormatting.DARK_RED.getColor();
        assert style(colored, colored.getString().indexOf("を利用")).getColor().getValue() == ChatFormatting.DARK_RED.getColor();
        assert style(colored, colored.getString().indexOf("75%")).getColor().getValue() == ChatFormatting.BLACK.getColor();
        assert style(colored, 3).isUnderlined() && style(colored, 3).getHoverEvent() != null;
        var split = Component.literal("工業的").withStyle(ChatFormatting.RED)
                .append(Component.literal("OC").withStyle(ChatFormatting.BOLD));
        var linked = GlossaryLinks.decorate(split, terms, new HashSet<>(), "");
        assert click(linked, 0).equals(click(linked, 3));
        assert style(linked, 3).isBold();
        var external = Component.literal("計算力").withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "@!hpca")));
        assert click(GlossaryLinks.decorate(external, terms, new HashSet<>(), ""), 0).equals("@!hpca");
        var hidden = GlossaryLinks.decorate(Component.literal("§kOC§r OC"), terms, new HashSet<>(), "");
        assert click(hidden, 0) == null && click(hidden, 3).equals("@!glossary:overclocking");
        language.put("test.glossary.arguments", "%sを使う。時間は90%%。");
        var translated = Component.translatable("test.glossary.arguments", Component.literal("工業的OC").withStyle(ChatFormatting.BOLD));
        var translatedLink = GlossaryLinks.decorate(translated, terms, new HashSet<>(), "");
        assert translatedLink.getString().equals("工業的OCを使う。時間は90%。");
        assert style(translatedLink, 0).isBold() && click(translatedLink, 0).endsWith("industrial_overclocking");
    }

    private static String click(Component component, int index) {
        var event = style(component, index).getClickEvent();
        if (event == null) return null;
        assert event.getAction() == ClickEvent.Action.OPEN_URL;
        return event.getValue();
    }
    private static Style style(Component component, int index) {
        int[] cursor = {0};
        return component.<Style>visit((style, text) -> {
            int start = cursor[0]; cursor[0] += text.length();
            return index >= start && index < cursor[0] ? Optional.of(style) : Optional.empty();
        }, Style.EMPTY).orElse(Style.EMPTY);
    }
    private static void read(Map<String, String> target, String text) {
        JsonParser.parseString(text).getAsJsonObject().entrySet().forEach(entry -> target.put(entry.getKey(), entry.getValue().getAsString()));
    }
    private static final class MapLanguage extends Language {
        private final Map<String, String> values;
        MapLanguage(Map<String, String> values) { this.values = values; }
        @Override public String getOrDefault(String key, String fallback) { return values.getOrDefault(key, fallback); }
        @Override public boolean has(String key) { return values.containsKey(key); }
        @Override public boolean isDefaultRightToLeft() { return false; }
        @Override public FormattedCharSequence getVisualOrder(FormattedText text) {
            return FormattedCharSequence.forward(text.getString(), Style.EMPTY);
        }
    }
}
