package DIV.gtcsolo.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * プレイヤー1人分の恒久強化データ (スキル/ステータスツリー)。
 *
 * <p>仕様: {@code refs/claude/2026-05-31_progression_tree_spec.md}
 * <ul>
 *   <li>{@code lifetimeXp} — 獲得XPの累積 (死亡/消費で減らない)。 これを起点に {@code permanentLevel} を算出。</li>
 *   <li>付与量 = {@code floor(permanentLevel / 10)}。 スキルP/ステータスPを<b>それぞれ満額</b>付与。</li>
 *   <li>{@code nodeLevels} — 購入済みノードの {@code id → level} (両ツリー共用)。</li>
 * </ul>
 *
 * <p>本クラスは<b>純粋なデータ層</b>。 ノードのコスト計算・前提判定・attribute 適用は別レイヤ (Phase 3) が担う。
 */
public class PlayerProgression {

    private long lifetimeXp = 0L;
    private int permanentLevel = 0;

    /** 利用可能なポイント (= 付与済み − 消費済み)。 */
    private int skillPoints = 0;
    private int statPoints = 0;

    /** 累計付与ポイント (リスペック時の全額返却に使う)。 */
    private int skillEarned = 0;
    private int statEarned = 0;

    /** 既存キャラの現在XPで一度だけ初期化したか。 */
    private boolean initialized = false;

    private final Map<ResourceLocation, Integer> nodeLevels = new HashMap<>();

    // ---- バニラXP曲線 (累積XP → レベル) -------------------------------------

    /** レベル {@code level} に到達するのに必要な累積XP (バニラ式・整数厳密)。 */
    public static long totalXpToReach(int level) {
        if (level <= 0) return 0L;
        long l = level;
        if (level <= 16) return l * l + 6L * l;                 // L^2 + 6L
        if (level <= 31) return (5L * l * l - 81L * l + 720L) / 2L;   // 2.5L^2 - 40.5L + 360
        return (9L * l * l - 325L * l + 4440L) / 2L;            // 4.5L^2 - 162.5L + 2220
    }

    // ---- XP 積算 -----------------------------------------------------------

    /**
     * 累積XPを加算し、 上がった分のレベルアップに応じてポイントを付与する。
     * @return このコールで上がったレベル数 (0 以上)
     */
    public int addXp(long amount) {
        if (amount <= 0) return 0;
        lifetimeXp += amount;
        int gained = 0;
        while (lifetimeXp >= totalXpToReach(permanentLevel + 1)) {
            permanentLevel++;
            int award = permanentLevel / 10;   // floor(level/10)
            skillPoints += award;
            statPoints += award;
            skillEarned += award;
            statEarned += award;
            gained++;
        }
        return gained;
    }

    /** 既存キャラ向け: 未初期化なら現在の累積XPを一度だけ流し込む。 */
    public void seedIfNeeded(long currentTotalXp) {
        if (initialized) return;
        initialized = true;
        addXp(currentTotalXp);
    }

    // ---- ポイント消費 (Phase 3 から使用) -----------------------------------

    public boolean trySpendSkillPoints(int cost) {
        if (cost < 0 || skillPoints < cost) return false;
        skillPoints -= cost;
        return true;
    }

    public boolean trySpendStatPoints(int cost) {
        if (cost < 0 || statPoints < cost) return false;
        statPoints -= cost;
        return true;
    }

    // ---- ノード -------------------------------------------------------------

    public int getNodeLevel(ResourceLocation id) {
        return nodeLevels.getOrDefault(id, 0);
    }

    public void setNodeLevel(ResourceLocation id, int level) {
        if (level <= 0) nodeLevels.remove(id);
        else nodeLevels.put(id, level);
    }

    public Map<ResourceLocation, Integer> getNodeLevels() {
        return nodeLevels;
    }

    // ---- リスペック ---------------------------------------------------------

    /**
     * 全ノードを解除しポイントを全額返却する (基本無料・何度でも)。
     * コスト未知でも {@code available = earned} に戻すだけで正しく全返却される。
     */
    public void respecAll() {
        nodeLevels.clear();
        skillPoints = skillEarned;
        statPoints = statEarned;
    }

    // ---- getter -------------------------------------------------------------

    public long getLifetimeXp() { return lifetimeXp; }
    public int getPermanentLevel() { return permanentLevel; }
    public int getSkillPoints() { return skillPoints; }
    public int getStatPoints() { return statPoints; }
    public int getSkillEarned() { return skillEarned; }
    public int getStatEarned() { return statEarned; }

    // ---- NBT ----------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("lifetimeXp", lifetimeXp);
        t.putInt("permanentLevel", permanentLevel);
        t.putInt("skillPoints", skillPoints);
        t.putInt("statPoints", statPoints);
        t.putInt("skillEarned", skillEarned);
        t.putInt("statEarned", statEarned);
        t.putBoolean("initialized", initialized);
        CompoundTag nodes = new CompoundTag();
        nodeLevels.forEach((k, v) -> nodes.putInt(k.toString(), v));
        t.put("nodes", nodes);
        return t;
    }

    public void load(CompoundTag t) {
        lifetimeXp = t.getLong("lifetimeXp");
        permanentLevel = t.getInt("permanentLevel");
        skillPoints = t.getInt("skillPoints");
        statPoints = t.getInt("statPoints");
        skillEarned = t.getInt("skillEarned");
        statEarned = t.getInt("statEarned");
        initialized = t.getBoolean("initialized");
        nodeLevels.clear();
        CompoundTag nodes = t.getCompound("nodes");
        for (String key : nodes.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) nodeLevels.put(id, nodes.getInt(key));
        }
    }
}
