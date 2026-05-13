package DIV.gtcsolo.machine.starforge;

import DIV.gtcsolo.item.AbstractLocusItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * StarForge の軌跡別パラメータ表。
 *
 * 自作 JEI ページ (StarForgeInfoCategory) が表示するデータの単一情報源 (SoT)。
 * 値が未確定のパラメータは TODO 仮値で置いてある。確定したら本ファイルを編集する。
 *
 * <p>仕様書: docs/StarForge_spec.md (ver.0.4)
 */
public final class StarForgeTraceData {
    private StarForgeTraceData() {}

    public enum Kind {
        /** 構築 → 崩壊。要求アイテム 1 回投入で崩壊フェイズへ、所定アイテム累計消費で履行 */
        NORMAL,
        /** 構築 → 成熟 → 崩壊。成熟フェイズ duration 中に発電 (太陽) or 大量放出 (BH) */
        MATURITY_SUN,
        /** 構築 → 成熟 → 崩壊。BH は崩壊度+超消費の特殊フェイズ */
        MATURITY_BLACK_HOLE
    }

    /**
     * 軌跡 1 件分のパラメータ。
     * すべての軌跡に当てはまるとは限らない (太陽は累計消費量が無い等)。
     * 該当無しは null / 0 / 空リスト。
     */
    public static final class TraceInfo {
        public final String trace;             // AbstractLocusItem.* の値
        public final Kind kind;
        public final String displayKey;        // 翻訳キー (gtcsolo.starforge.trace.<trace>.name)

        // 構築フェイズ (全軌跡)
        public final List<String> starterItemHints;  // "要求アイテム" の候補表記 (TODO: 確定したら ItemStack 化)

        // 崩壊フェイズ (通常型)
        public final List<String> continuousItemHints;  // "所定アイテム" 候補表記
        public final long requiredAmount;       // 累計消費量 (0 なら未確定 or 該当無し)

        // 成熟フェイズ (太陽 / BH)
        public final long maturityDurationTicks; // 0 なら該当無し
        public final long maturityEUt;           // 太陽=生成量、BH=放出量、0 なら該当無し

        // 出力 (全軌跡、未確定だらけなのでヒント表記で先行)
        public final List<String> outputHints;

        // 自由記述 (フェイズ概要、特殊事項など)。翻訳キー (gtcsolo.starforge.trace.<trace>.note)
        public final String noteKey;

        private TraceInfo(Builder b) {
            this.trace = b.trace;
            this.kind = b.kind;
            this.displayKey = b.displayKey;
            this.starterItemHints = List.copyOf(b.starterItemHints);
            this.continuousItemHints = List.copyOf(b.continuousItemHints);
            this.requiredAmount = b.requiredAmount;
            this.maturityDurationTicks = b.maturityDurationTicks;
            this.maturityEUt = b.maturityEUt;
            this.outputHints = List.copyOf(b.outputHints);
            this.noteKey = b.noteKey;
        }

        public static Builder builder(String trace, Kind kind) {
            return new Builder(trace, kind);
        }

        public static final class Builder {
            private final String trace;
            private final Kind kind;
            private String displayKey;
            private List<String> starterItemHints = List.of();
            private List<String> continuousItemHints = List.of();
            private long requiredAmount = 0L;
            private long maturityDurationTicks = 0L;
            private long maturityEUt = 0L;
            private List<String> outputHints = List.of();
            private String noteKey;

            private Builder(String trace, Kind kind) {
                this.trace = trace;
                this.kind = kind;
                this.displayKey = "item.gtcsolo.star_locus." + trace; // 既存翻訳を流用
                this.noteKey = "gtcsolo.starforge.trace." + trace + ".note";
            }

            public Builder starterHints(String... hints)    { this.starterItemHints = List.of(hints); return this; }
            public Builder continuousHints(String... hints) { this.continuousItemHints = List.of(hints); return this; }
            public Builder requiredAmount(long n)           { this.requiredAmount = n; return this; }
            public Builder maturityDuration(long ticks)     { this.maturityDurationTicks = ticks; return this; }
            public Builder maturityEUt(long eut)            { this.maturityEUt = eut; return this; }
            public Builder outputHints(String... hints)     { this.outputHints = List.of(hints); return this; }

            public TraceInfo build() { return new TraceInfo(this); }
        }
    }

    private static final Map<String, TraceInfo> TRACES = new LinkedHashMap<>();

    /**
     * 軌跡を登録する。組み込み 8 軌跡は static initializer で登録される。
     * addon が新規軌跡を追加するときもこれを呼ぶ (FMLCommonSetupEvent タイミング推奨)。
     */
    public static void register(TraceInfo info) {
        TRACES.put(info.trace, info);
    }

    public static TraceInfo get(String trace) {
        return TRACES.get(trace);
    }

    /** 登録順 (= JEI 表示順) */
    public static List<TraceInfo> all() {
        return List.copyOf(TRACES.values());
    }

    static {
        // 通常型 6 軌跡 (TODO 仮値、確定したら本ファイル更新)
        register(TraceInfo.builder(AbstractLocusItem.BROWN_DWARF, Kind.NORMAL)
                .starterHints("TODO: 起動アイテム未確定")
                .continuousHints("TODO: 所定アイテム未確定")
                .outputHints("TODO: 出力アイテム未確定")
                .build());
        register(TraceInfo.builder(AbstractLocusItem.KOI74, Kind.NORMAL)
                .starterHints("TODO").continuousHints("TODO").outputHints("TODO").build());
        register(TraceInfo.builder(AbstractLocusItem.R_ANDROMEDAE, Kind.NORMAL)
                .starterHints("TODO").continuousHints("TODO").outputHints("TODO").build());
        register(TraceInfo.builder(AbstractLocusItem.HD101065, Kind.NORMAL)
                .starterHints("TODO").continuousHints("TODO").outputHints("TODO").build());
        register(TraceInfo.builder(AbstractLocusItem.CEMP_R, Kind.NORMAL)
                .starterHints("TODO").continuousHints("TODO").outputHints("TODO").build());
        register(TraceInfo.builder(AbstractLocusItem.NEUTRON_STAR, Kind.NORMAL)
                .starterHints("TODO").continuousHints("TODO").outputHints("TODO").build());

        // 成熟型 (太陽)
        register(TraceInfo.builder(AbstractLocusItem.SUN, Kind.MATURITY_SUN)
                .starterHints("TODO: 起動アイテム未確定")
                .continuousHints("GTCEu Solar Panel × N (TODO: 個数 / tier 未確定)")
                .outputHints("TODO: 出力アイテム未確定 (ソーラー一部返還 + 少量アイテム)")
                .maturityDuration(0L)  // TODO 確定待ち
                .maturityEUt(0L)
                .build());

        // 成熟型 (ブラックホール)
        register(TraceInfo.builder(AbstractLocusItem.BLACK_HOLE, Kind.MATURITY_BLACK_HOLE)
                .starterHints("TODO: 起動アイテム未確定")
                .continuousHints("TODO: 成熟フェイズで瞬間消費される要求アイテム")
                .outputHints("通常崩壊: TODO / 超消費中に電源 OFF: gtcsolo:star_singularity")
                .maturityDuration(0L)
                .maturityEUt(0L)
                .build());
    }
}
