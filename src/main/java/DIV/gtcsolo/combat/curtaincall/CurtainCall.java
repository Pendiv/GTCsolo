package DIV.gtcsolo.combat.curtaincall;

import DIV.gtcsolo.l2.util.L2EntityUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 激戦の幕引き — 戦闘膠着・スタック増加・ボス遷移特例・位置検証を一括管理。
 *
 * <p>継戦判定 (= 状態継続):
 * <ul>
 *   <li>soft alive: 直近 {@link #LIGHT_WINDOW_TICKS} (= 15s) に与ダメ OR 被ダメ あり</li>
 *   <li>hard alive (= 膠着): 直近 {@link #HARD_WINDOW_TICKS} (= 60s) に与ダメ AND 被ダメ あり</li>
 *   <li>soft alive 切れる → state 全リセット</li>
 *   <li>hard alive 中で position check (= 60s 毎 player or 最長 mob が r > {@link #POSITION_THRESHOLD} 移動) 成立で stack 増加対象</li>
 * </ul>
 *
 * <p>スタック増加: 上記 stack 増加対象状態で {@link #STACK_INTERVAL_DEFAULT_TICKS} (= 5 分) 経過ごと +1。
 * ボス相手で特例 (boss carryover) 残存中は {@link #STACK_INTERVAL_BOSS_BOOST_TICKS} (= 2100 tick) に短縮。
 *
 * <p>ボス遷移 (= 最長戦闘相手が雑魚 → ボスに変わった瞬間):
 * <ul>
 *   <li>現 stacks を {@code bossCarryover} に保存、 stacks=0 リセット</li>
 *   <li>新ボス相手で stacks < bossCarryover の間、 stack 増加が 2100 tick 間隔</li>
 *   <li>stacks ≥ bossCarryover になったら通常 6000 tick 間隔へ戻る</li>
 *   <li>逆 (ボス → 雑魚) は特例なし、 そのまま継続</li>
 * </ul>
 */
public final class CurtainCall {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CurtainCall() {}

    public static final long LIGHT_WINDOW_TICKS = 300L;     // 15s: 与ダメ OR 被ダメ で継戦継続
    public static final long HARD_WINDOW_TICKS = 1200L;     // 60s: 与ダメ AND 被ダメ で stack 対象
    public static final long POSITION_CHECK_INTERVAL = 1200L; // 60s 毎に position snapshot 更新
    public static final double POSITION_THRESHOLD = 3.0;    // r > 3 移動必須

    public static final long STACK_INTERVAL_DEFAULT_TICKS = 6000L; // 通常 5 分
    public static final long STACK_INTERVAL_BOSS_BOOST_TICKS = 2100L; // ボス carryover 中 = 2100 tick

    /** tick 評価間隔 = 1 秒 (= ラグ軽減、 stack 増減/15s/60s 全ての判定がこの粒度で動く) */
    public static final long EVAL_INTERVAL_TICKS = 20L;

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static class State {
        public int stacks = 0;
        public long lastIncrementTick = Long.MIN_VALUE;
        public long lastDealtTick = Long.MIN_VALUE;
        public long lastTakenTick = Long.MIN_VALUE;

        /** UUID → engagement info (= player と該当 mob の戦闘記録) */
        public final Map<UUID, MobEngagement> engagedMobs = new HashMap<>();

        /** 現「最長戦闘相手」 UUID (= engagedSinceTick が最古の mob) */
        public UUID longestMobId = null;
        public boolean longestIsBoss = false;
        /** engagedMobs に変更があったか (= 最長 mob 再計算が必要か) */
        public boolean engagedMobsDirty = false;

        /** 雑魚 → ボス遷移時に保存される 「雑魚で貯めた層数」 */
        public int bossCarryover = 0;

        /** 60s 毎 position snapshot */
        public Vec3 lastPlayerPos = null;
        public Vec3 lastMobPos = null;
        public long lastPositionCheckTick = Long.MIN_VALUE;
        public boolean positionCheckPassed = false; // 直近 60s check の結果保持

        /** tick 評価の最終実行 tick (= 1 秒粒度 throttle 用) */
        public long lastEvalTick = Long.MIN_VALUE;
    }

    public static class MobEngagement {
        public final long engagedSinceTick;
        public long lastInteractionTick;
        /** isBoss を engagement 作成時にキャッシュ (= 以降 Entity 解決不要) */
        public final boolean isBoss;
        public MobEngagement(long now, boolean isBoss) {
            this.engagedSinceTick = now;
            this.lastInteractionTick = now;
            this.isBoss = isBoss;
        }
    }

    public static State get(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new State());
    }

    public static int getStacks(Player player) {
        return get(player).stacks;
    }

    public static void clear(Player player) {
        STATES.remove(player.getUUID());
    }

    // === ダメージイベントから呼ばれる ===

    /** player が hostile mob にダメージを与えた瞬間 */
    public static void onDealtToHostile(Player player, LivingEntity hostile, long gameTime) {
        State s = get(player);
        s.lastDealtTick = gameTime;
        registerEngagement(s, hostile, gameTime);
    }

    /** player が hostile mob からダメージを受けた瞬間 */
    public static void onTakenFromHostile(Player player, LivingEntity hostile, long gameTime) {
        State s = get(player);
        s.lastTakenTick = gameTime;
        registerEngagement(s, hostile, gameTime);
    }

    private static void registerEngagement(State s, LivingEntity mob, long now) {
        MobEngagement e = s.engagedMobs.get(mob.getUUID());
        if (e == null) {
            s.engagedMobs.put(mob.getUUID(), new MobEngagement(now, isBoss(mob)));
            s.engagedMobsDirty = true;
        } else {
            e.lastInteractionTick = now;
        }
    }

    // === tick で呼ばれる main update ===

    public static void tick(Player player, long gameTime) {
        State s = get(player);

        // throttle: 1 秒に 1 度しか走らない (= ラグ軽減)
        if (gameTime - s.lastEvalTick < EVAL_INTERVAL_TICKS) return;
        s.lastEvalTick = gameTime;

        // 早期離脱: ダメージ往来一度も無し → state 何もない、 やることなし (= cold path)
        if (s.lastDealtTick == Long.MIN_VALUE && s.lastTakenTick == Long.MIN_VALUE) return;

        // 1. 期限切れ engagement を削除 (HARD_WINDOW 以上前)
        int beforeSize = s.engagedMobs.size();
        s.engagedMobs.entrySet().removeIf(e -> (gameTime - e.getValue().lastInteractionTick) > HARD_WINDOW_TICKS);
        if (s.engagedMobs.size() != beforeSize) s.engagedMobsDirty = true;

        // 2. soft alive 確認 (= 15s 内に与ダメ OR 被ダメ あり)
        boolean softAlive = (gameTime - s.lastDealtTick) <= LIGHT_WINDOW_TICKS
                          || (gameTime - s.lastTakenTick) <= LIGHT_WINDOW_TICKS;
        if (!softAlive) {
            // 戦闘終了、 state 全リセット (= stacks も bossCarryover も消える)
            if (s.stacks > 0 || s.bossCarryover > 0) {
                LOGGER.info("[CurtainCall] {} combat ended (soft alive lost), full reset (was stacks={}, carryover={})",
                        player.getName().getString(), s.stacks, s.bossCarryover);
            }
            resetState(s);
            return;
        }

        // 3. 最長戦闘相手の特定 (= engagedMobs 変更時のみ再計算)
        if (s.engagedMobsDirty) {
            UUID newLongestId = null;
            boolean newLongestIsBoss = false;
            long oldestSince = Long.MAX_VALUE;
            for (var entry : s.engagedMobs.entrySet()) {
                if (entry.getValue().engagedSinceTick < oldestSince) {
                    oldestSince = entry.getValue().engagedSinceTick;
                    newLongestId = entry.getKey();
                    newLongestIsBoss = entry.getValue().isBoss;
                }
            }
            // 4. 最長相手変化検知 → ボス遷移処理
            if (newLongestId != null && !newLongestId.equals(s.longestMobId)) {
                // 旧最長が雑魚で新最長がボス → 特例付与
                if (s.longestMobId != null && !s.longestIsBoss && newLongestIsBoss) {
                    s.bossCarryover = s.stacks;
                    LOGGER.info("[CurtainCall] {} transition 雑魚→Boss: bossCarryover = {} (stacks reset)",
                            player.getName().getString(), s.bossCarryover);
                    s.stacks = 0;
                    s.lastIncrementTick = Long.MIN_VALUE;
                }
                s.longestMobId = newLongestId;
                s.longestIsBoss = newLongestIsBoss;
            }
            s.engagedMobsDirty = false;
        }

        // 5. 60s 毎 position check
        boolean positionMoved = checkPositionSinceLast(player, s, gameTime);

        // 6. hard alive (= 膠着) 判定
        boolean hardAlive = (gameTime - s.lastDealtTick) <= HARD_WINDOW_TICKS
                         && (gameTime - s.lastTakenTick) <= HARD_WINDOW_TICKS;
        if (!hardAlive || !s.positionCheckPassed) {
            // hard 不成立 → stack 進行しない (= soft alive なので state は維持)
            return;
        }

        // 7. stack 増加判定 — 間隔は boss carryover 残存中なら 2100、 さもなければ 6000
        long interval = (s.longestIsBoss && s.stacks < s.bossCarryover)
                ? STACK_INTERVAL_BOSS_BOOST_TICKS
                : STACK_INTERVAL_DEFAULT_TICKS;
        if (s.lastIncrementTick == Long.MIN_VALUE) {
            // 膠着初発火、 timer 開始 (= 即 +1 しない、 interval 後に最初の stack)
            s.lastIncrementTick = gameTime;
            LOGGER.info("[CurtainCall] {} stalemate confirmed, timer started (interval={}t, longest={}, isBoss={})",
                    player.getName().getString(), interval, s.longestMobId, s.longestIsBoss);
            return;
        }
        if (gameTime - s.lastIncrementTick >= interval) {
            s.stacks++;
            s.lastIncrementTick = gameTime;
            // boss carryover に到達したら carryover 消費完了
            if (s.longestIsBoss && s.stacks >= s.bossCarryover && s.bossCarryover > 0) {
                LOGGER.info("[CurtainCall] {} boss carryover consumed, switching to default interval",
                        player.getName().getString());
                s.bossCarryover = 0;
            }
            LOGGER.info("[CurtainCall] {} gained stack {} (interval was {}t, longest={}, isBoss={})",
                    player.getName().getString(), s.stacks, interval, s.longestMobId, s.longestIsBoss);
        }
    }

    /** 60s 毎の position check — snapshot 更新 + 移動判定。 戻り値 = 直近 check が pass したか。 */
    private static boolean checkPositionSinceLast(Player player, State s, long gameTime) {
        if (s.lastPositionCheckTick == Long.MIN_VALUE
                || gameTime - s.lastPositionCheckTick >= POSITION_CHECK_INTERVAL) {
            // snapshot 更新タイミング
            Vec3 curPlayerPos = player.position();
            LivingEntity longest = s.longestMobId != null ? findEntityByUUID(player, s.longestMobId) : null;
            Vec3 curMobPos = longest != null ? longest.position() : null;
            // 前回 snapshot との比較
            boolean moved = false;
            if (s.lastPlayerPos != null && s.lastPlayerPos.distanceTo(curPlayerPos) > POSITION_THRESHOLD) {
                moved = true;
            }
            if (!moved && s.lastMobPos != null && curMobPos != null
                    && s.lastMobPos.distanceTo(curMobPos) > POSITION_THRESHOLD) {
                moved = true;
            }
            // 初回 (= snapshot まだ無い) は moved=true 扱い (= 立ち上がり許容)
            if (s.lastPlayerPos == null) moved = true;
            s.positionCheckPassed = moved;
            s.lastPlayerPos = curPlayerPos;
            s.lastMobPos = curMobPos;
            s.lastPositionCheckTick = gameTime;
            if (!moved) {
                LOGGER.info("[CurtainCall] {} position check failed (both stationary), stack progression halted",
                        player.getName().getString());
            }
        }
        return s.positionCheckPassed;
    }

    private static void resetState(State s) {
        s.stacks = 0;
        s.bossCarryover = 0;
        s.lastIncrementTick = Long.MIN_VALUE;
        s.engagedMobs.clear();
        s.engagedMobsDirty = false;
        s.longestMobId = null;
        s.longestIsBoss = false;
        s.lastPlayerPos = null;
        s.lastMobPos = null;
        s.lastPositionCheckTick = Long.MIN_VALUE;
        s.positionCheckPassed = false;
    }

    /**
     * mob 死亡時の処理。 死んだ mob が誰かの「最長戦闘相手」 かつ ボスなら、 その player の state を full リセット。
     * 非ボス mob 死亡 or 非最長 mob 死亡 → 何もしない (= stacks 据え置き、 tick で次最長へ自動移行)。
     */
    public static void handleMobDeath(LivingEntity dyingMob) {
        if (!isBoss(dyingMob)) return;
        UUID dyingId = dyingMob.getUUID();
        for (var entry : STATES.entrySet()) {
            State s = entry.getValue();
            if (dyingId.equals(s.longestMobId)) {
                LOGGER.info("[CurtainCall] boss {} (longest target of player {}) killed, full reset (was stacks={}, carryover={})",
                        dyingId, entry.getKey(), s.stacks, s.bossCarryover);
                resetState(s);
            }
        }
    }

    private static LivingEntity findEntityByUUID(Player player, UUID id) {
        Entity e = ((net.minecraft.server.level.ServerLevel) player.level()).getEntity(id);
        return e instanceof LivingEntity le ? le : null;
    }

    /** ボス判定 → {@link L2EntityUtil#isBoss(LivingEntity)} に委譲 */
    public static boolean isBoss(LivingEntity entity) {
        return L2EntityUtil.isBoss(entity);
    }

    // === 効果計算 ===

    /** 効果 1: 与ダメ倍率 = 1 + min(10.0, 0.15 × stacks²) */
    public static double damageMultiplier(int stacks) {
        if (stacks <= 0) return 1.0;
        return 1.0 + Math.min(10.0, 0.15 * stacks * stacks);
    }

    /** 効果 2: 軽減減衰量 = min(0.24, 0.03 × min(8, stacks)) */
    public static double reductionDecay(int stacks) {
        if (stacks <= 0) return 0.0;
        return Math.min(0.24, 0.03 * Math.min(8, stacks));
    }

    /** 効果 2 適用後の damage_reduction 属性 multiplier */
    public static double reductionAttrMultiplier(double reductionAttr, int stacks) {
        if (reductionAttr >= 1.0 || reductionAttr <= 0) return 1.0;
        double decay = reductionDecay(stacks);
        double after = Math.min(1.0, reductionAttr + decay);
        return after / reductionAttr;
    }

    /** 効果 3: heal 減衰係数 (= 1=0.33 / 2=0.55 / 3+=0.66、 上限 3 stack) */
    public static double healDecay(int stacks) {
        if (stacks <= 0) return 0.0;
        if (stacks == 1) return 0.33;
        if (stacks == 2) return 0.55;
        return 0.66;
    }
}
