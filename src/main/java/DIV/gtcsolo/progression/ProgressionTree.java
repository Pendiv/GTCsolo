package DIV.gtcsolo.progression;

import com.google.gson.JsonSyntaxException;

/** ノードの所属ツリー。 stat = attribute 操作 / skill = 状態保持のみ。 */
public enum ProgressionTree {
    STAT,
    SKILL;

    public static ProgressionTree fromString(String s) {
        for (ProgressionTree t : values()) {
            if (t.name().equalsIgnoreCase(s)) return t;
        }
        throw new JsonSyntaxException("unknown tree: '" + s + "' (expected 'stat' or 'skill')");
    }
}
