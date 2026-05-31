package DIV.gtcsolo.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * datapack 定義 1 ノード分。 仕様: {@code refs/claude/2026-05-31_progression_tree_spec.md}。
 *
 * <p>{@link #parse} は不正を {@link JsonSyntaxException} で弾く (= ローダ側で skip)。 ここを通ったノードのみ有効。
 *
 * @param id           ノードID (= datapack の resource location)
 * @param tree         所属ツリー
 * @param title        表示名 (lang key 想定、 未指定なら id 文字列)
 * @param icon         アイコン item の id (任意)
 * @param x            キャンバスX座標
 * @param y            キャンバスY座標
 * @param maxLevel     最大レベル (必須・1以上)
 * @param cost         レベル別コスト配列 (長さ == maxLevel)。 null なら初項1フィボナッチ
 * @param requirements 前提 (全 AND)
 * @param effect       stat ツリーの attribute 効果 (skill では null)
 */
public record ProgressionNode(
        ResourceLocation id,
        ProgressionTree tree,
        String title,
        @Nullable ResourceLocation icon,
        int x,
        int y,
        int maxLevel,
        @Nullable long[] cost,
        List<Requirement> requirements,
        @Nullable NodeEffect effect
) {

    /** 前提条件: {@code node} を {@code minLevel} 以上にしていること。 minLevel=1 ＝「獲得済み」。 */
    public record Requirement(ResourceLocation node, int minLevel) {}

    /** stat ノードの attribute 効果。 1 レベルあたり {@code perLevel} を {@code operation} で加算。 */
    public record NodeEffect(Attribute attribute, AttributeModifier.Operation operation, double perLevel) {}

    /** {@code level} (1 始まり、 1=獲得) に到達するのに要するポイント。 */
    public long costForLevel(int level) {
        if (cost != null && level >= 1 && level <= cost.length) return cost[level - 1];
        return fibCost(level);
    }

    /** {@code level} まで上げる累計コスト (= 1..level の合計)。 */
    public long totalCostUpTo(int level) {
        long sum = 0;
        for (int l = 1; l <= level; l++) sum += costForLevel(l);
        return sum;
    }

    /** 初項1のフィボナッチ: 1,1,2,3,5,8,13… ({@code level>=1})。 */
    public static long fibCost(int level) {
        if (level <= 2) return 1L;
        long a = 1, b = 1;
        for (int i = 3; i <= level; i++) {
            long c = a + b;
            a = b;
            b = c;
        }
        return b;
    }

    // ---- パース ------------------------------------------------------------

    public static ProgressionNode parse(ResourceLocation id, JsonObject json) {
        ProgressionTree tree = ProgressionTree.fromString(GsonHelper.getAsString(json, "tree"));

        int maxLevel = GsonHelper.getAsInt(json, "max_level");
        if (maxLevel < 1) throw new JsonSyntaxException("max_level must be >= 1 (got " + maxLevel + ")");

        String title = GsonHelper.getAsString(json, "title", id.toString());

        ResourceLocation icon = json.has("icon")
                ? new ResourceLocation(GsonHelper.getAsString(json, "icon")) : null;

        int x = 0, y = 0;
        if (json.has("pos")) {
            JsonObject pos = GsonHelper.getAsJsonObject(json, "pos");
            x = GsonHelper.getAsInt(pos, "x", 0);
            y = GsonHelper.getAsInt(pos, "y", 0);
        }

        long[] cost = null;
        if (json.has("cost")) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "cost");
            if (arr.size() != maxLevel) {
                throw new JsonSyntaxException("cost length (" + arr.size()
                        + ") must equal max_level (" + maxLevel + ")");
            }
            cost = new long[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                cost[i] = arr.get(i).getAsLong();
                if (cost[i] < 0) throw new JsonSyntaxException("cost entries must be >= 0");
            }
        }

        List<Requirement> reqs = new ArrayList<>();
        if (json.has("requirements")) {
            for (JsonElement el : GsonHelper.getAsJsonArray(json, "requirements")) {
                JsonObject r = GsonHelper.convertToJsonObject(el, "requirement");
                ResourceLocation node = new ResourceLocation(GsonHelper.getAsString(r, "node"));
                int minLevel = GsonHelper.getAsInt(r, "min_level", 1);
                if (minLevel < 1) throw new JsonSyntaxException("requirement min_level must be >= 1");
                reqs.add(new Requirement(node, minLevel));
            }
        }

        NodeEffect effect = null;
        if (tree == ProgressionTree.STAT) {
            if (!json.has("effect")) throw new JsonSyntaxException("stat node requires 'effect'");
            JsonObject e = GsonHelper.getAsJsonObject(json, "effect");
            ResourceLocation attrId = new ResourceLocation(GsonHelper.getAsString(e, "attribute"));
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(attrId);
            if (attr == null) throw new JsonSyntaxException("unknown attribute: " + attrId);
            AttributeModifier.Operation op = parseOperation(GsonHelper.getAsString(e, "operation"));
            double perLevel = GsonHelper.getAsDouble(e, "per_level");
            effect = new NodeEffect(attr, op, perLevel);
        } else if (json.has("effect")) {
            throw new JsonSyntaxException("skill node must not declare 'effect'");
        }

        return new ProgressionNode(id, tree, title, icon, x, y, maxLevel, cost, reqs, effect);
    }

    private static AttributeModifier.Operation parseOperation(String s) {
        switch (s.toLowerCase()) {
            case "addition":
                return AttributeModifier.Operation.ADDITION;
            case "multiply_base":
                return AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total":
                return AttributeModifier.Operation.MULTIPLY_TOTAL;
            default:
                throw new JsonSyntaxException("unknown operation: '" + s
                        + "' (expected addition / multiply_base / multiply_total)");
        }
    }
}
