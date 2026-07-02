package DIV.gtcsolo.manual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * マニュアル用の日英両取り lang リーダ (クライアント専用)。
 *
 * <p>Minecraft 本体の {@code Language} は「選択言語 + en_us フォールバック」の合成 1 枚しか持たず、
 * 現在言語に関係なく ja / en 双方を取る API が無い。 そこで対象 namespace の
 * {@code lang/ja_jp.json} / {@code lang/en_us.json} を ResourceManager から直接読んで保持する。
 * 全 namespace を舐めると modpack では重いので、 必要な namespace を {@link #ensureLoaded} で
 * 指定された分だけ遅延ロードする。
 */
public final class ManualLangs {

    private static final Map<String, String> JA = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();
    private static final Set<String> LOADED = new java.util.HashSet<>();

    private ManualLangs() {}

    /** 指定 namespace 群の ja/en lang を未ロード分だけ読み込む。 */
    public static void ensureLoaded(Set<String> namespaces) {
        for (String ns : namespaces) {
            if (LOADED.add(ns)) {
                loadInto(JA, ns, "ja_jp");
                loadInto(EN, ns, "en_us");
            }
        }
    }

    /**
     * 「日本語名 / English Name」 の併記文字列。 片方しか無ければそれのみ、 同一なら 1 回、
     * 両方無ければ key をそのまま返す。
     */
    public static String dual(String key) {
        String ja = JA.get(key);
        String en = EN.get(key);
        if (ja == null && en == null) return key;
        if (ja == null) return en;
        if (en == null || ja.equals(en)) return ja;
        return ja + " / " + en;
    }

    private static void loadInto(Map<String, String> map, String namespace, String locale) {
        var rm = Minecraft.getInstance().getResourceManager();
        // 低優先 → 高優先の順で重ね、 リソースパック等の上書きを正しく反映する。
        for (Resource res : rm.getResourceStack(new ResourceLocation(namespace, "lang/" + locale + ".json"))) {
            try (Reader reader = res.openAsReader()) {
                JsonObject json = GsonHelper.parse(reader);
                for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                    if (e.getValue().isJsonPrimitive()) {
                        map.put(e.getKey(), e.getValue().getAsString());
                    }
                }
            } catch (Exception ignored) {
                // 壊れた lang は黙って飛ばす (本体側のロードでも警告が出る)
            }
        }
    }
}
