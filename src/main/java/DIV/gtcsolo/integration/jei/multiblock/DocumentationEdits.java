package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/** Client-local, language-specific authoring data. Never mutates a world or installed JAR. */
public final class DocumentationEdits {
    private static final Map<String, DocumentationStore> STORES = new HashMap<>();
    private static long revision;
    private DocumentationEdits() {}

    public static String language() { return Minecraft.getInstance().getLanguageManager().getSelected(); }
    public static long revision() { return revision; }
    public static void changed() { revision++; }
    public static DocumentationStore store(String language) {
        return STORES.computeIfAbsent(language, key -> {
            var store = new DocumentationStore(Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config/gtcsolo/documentation"), key);
            if (store.loadError() != null) Gtcsolo.LOGGER.error("Cannot read {}", store.file(), store.loadError());
            return store;
        });
    }
    public static DocumentationPage resolve(DocumentationPage page) { return page.apply(store(language()).get(page.id())); }
    public static void reload() { STORES.values().forEach(DocumentationStore::reload); changed(); }
}
