package DIV.gtcsolo.progression;

import DIV.gtcsolo.Gtcsolo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 恒久強化ノードの datapack ローダ＆レジストリ。
 * {@code data/<ns>/progression/node/*.json} を読み、 {@link ProgressionNode#parse} で<b>厳格検証</b>し、
 * 不正は skip (ログ警告) する。 サーバ起動 / {@code /reload} で再読込。
 */
public class ProgressionNodes extends SimpleJsonResourceReloadListener {

    public static final ProgressionNodes INSTANCE = new ProgressionNodes();

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "progression/node";

    private Map<ResourceLocation, ProgressionNode> nodes = Map.of();

    private ProgressionNodes() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, ProgressionNode> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                ProgressionNode node = ProgressionNode.parse(id,
                        GsonHelper.convertToJsonObject(entry.getValue(), "node"));
                loaded.put(id, node);
            } catch (Exception e) {
                Gtcsolo.LOGGER.warn("[progression] skipping invalid node {}: {}", id, e.getMessage());
            }
        }
        // 診断: 存在しないノードを前提に持つ場合は警告 (= そのノードは永久ロックのまま・クラッシュはしない)
        for (ProgressionNode n : loaded.values()) {
            for (ProgressionNode.Requirement req : n.requirements()) {
                if (!loaded.containsKey(req.node())) {
                    Gtcsolo.LOGGER.warn("[progression] node {} requires unknown node {} (stays locked)",
                            n.id(), req.node());
                }
            }
        }
        nodes = Map.copyOf(loaded);
        Gtcsolo.LOGGER.info("[progression] loaded {} node(s)", nodes.size());
    }

    public ProgressionNode get(ResourceLocation id) {
        return nodes.get(id);
    }

    public Collection<ProgressionNode> all() {
        return nodes.values();
    }

    public Map<ResourceLocation, ProgressionNode> map() {
        return nodes;
    }
}
