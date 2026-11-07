package DIV.gtcsolo.integration.jei.multiblock;

import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Real filesystem transactions in an isolated audit directory, never the player's config. */
public final class DocumentationStoreTest {
    public static void main(String[] args) throws Exception {
        if (!DocumentationStoreTest.class.desiredAssertionStatus()) throw new IllegalStateException("Use -ea");
        Path directory = Files.createTempDirectory(Path.of(args[0]), "store-test-");
        var defaults = new DocumentationStore.Text("工業的オーバークロック", "§4注意§r\n入出力2倍・時間90%\n改行", "詳細");
        var store = new DocumentationStore(directory, "ja_jp");
        assert store.loadError() == null && !Files.exists(store.file());
        assert store.get("glossary/industrial_overclocking").apply(defaults).equals(defaults);
        var draft = new DocumentationStore.Text("工業的なOC", "説明を編集\n100%\n水と🔧", defaults.details());
        assert !Files.exists(store.file()) : "A private draft must not write anything";
        var patch = draft.difference(defaults);
        assert patch.details() == null;
        String id = "glossary/industrial_overclocking";
        store.save(id, DocumentationStore.Text.EMPTY, patch);
        var reopened = new DocumentationStore(directory, "ja_jp");
        assert reopened.get(id).apply(defaults).equals(draft);
        assert reopened.get("glossary/overclocking").isEmpty();
        var english = new DocumentationStore(directory, "en_us");
        assert english.get(id).isEmpty() && !Files.exists(english.file());

        // Separate machine variants and a concurrent save to a different page are retained.
        reopened.save("multiblock/gtceu:fusion_reactor_mk1", DocumentationStore.Text.EMPTY, new DocumentationStore.Text(null, "MK1", null));
        store.save("multiblock/gtceu:fusion_reactor_mk2", DocumentationStore.Text.EMPTY, new DocumentationStore.Text(null, "MK2", null));
        store.reload();
        assert store.get("multiblock/gtceu:fusion_reactor_mk1").body().equals("MK1");
        assert store.get("multiblock/gtceu:fusion_reactor_mk2").body().equals("MK2");
        assert Files.isRegularFile(store.file().resolveSibling("ja_jp.json.bak"));
        byte[] saved = Files.readAllBytes(store.file());
        try { reopened.save(id, DocumentationStore.Text.EMPTY, DocumentationStore.Text.EMPTY); throw new AssertionError("stale save accepted"); }
        catch (IOException expected) { assert java.util.Arrays.equals(saved, Files.readAllBytes(store.file())); }

        // Restore is a draft operation until Save, then removes overrides rather than freezing defaults.
        var restore = defaults.difference(defaults);
        assert restore.isEmpty() && !store.get(id).isEmpty();
        store.save(id, patch, restore);
        assert new DocumentationStore(directory, "ja_jp").get(id).isEmpty();
        var illustrated = new DocumentationPage("multiblock/gtceu:test", Component.literal("タイトル"),
                List.of(Component.literal("概要")), List.of(Component.literal("詳細1"), Component.literal("詳細2")), true, false);
        var edited = illustrated.apply(new DocumentationStore.Text(null, "概要変更\n追記\n", null));
        assert edited.hasFluidSection() && edited.body().size() == 2;
        assert edited.details().equals(illustrated.details());
        var blank = illustrated.apply(new DocumentationStore.Text(null, "", null));
        assert blank.body().isEmpty() && blank.details().size() == 2;
        var styled = illustrated.apply(DocumentationStore.Text.EMPTY);
        assert styled.equals(illustrated);

        // Broken files and unsupported versions are not silently overwritten.
        Files.writeString(store.file(), "{broken"); store.reload();
        assert store.loadError() != null;
        try { store.save(id, DocumentationStore.Text.EMPTY, patch); throw new AssertionError("broken file overwritten"); }
        catch (IOException expected) { assert Files.readString(store.file()).equals("{broken"); }
        Files.writeString(store.file(), "{\"version\":2,\"pages\":{}}"); store.reload();
        assert store.loadError() != null;
        try { new DocumentationStore(directory, "../../elsewhere"); throw new AssertionError("unsafe language accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new DocumentationStore.Text(null, "x".repeat(DocumentationStore.TEXT_LIMIT + 1), null); throw new AssertionError("oversize accepted"); }
        catch (IllegalArgumentException expected) { }
        try (var files = Files.list(directory)) { assert files.noneMatch(file -> file.toString().endsWith(".tmp")); }
        System.out.println("PASS: draft isolation, UTF-8 round trip, language/variant isolation, merge/conflict protection, backup, reset, fluid section separation, malformed file protection and size/path limits");
    }
}
