package DIV.gtcsolo.machine.starforge;

import DIV.gtcsolo.item.AbstractLocusItem;
import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModMaterials;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

        // ── ver.0.5 追加: 進捗テーブル ──
        // 構築フェイズで count を進めるアイテム群と必要 count
        public final PhaseProgressionTable buildPhaseTable;
        public final long buildRequiredCount;
        // 崩壊フェイズの所定アイテム群と必要 count (= 構築完了後さらに加算)
        public final PhaseProgressionTable decayPhaseTable;
        public final long decayRequiredCount;

        // ── ver.0.5 追加: GT 標準 JEI ダミーレシピ用の真出力 ──
        // (JEI に乗せる 4-6 品目。 実機運行で出る朽ち果てた星の軌跡はここに含めない = runtime 専属)
        public final List<ItemStack> outputItems;
        public final List<FluidStack> outputFluids;

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
            this.buildPhaseTable = b.buildPhaseTable;
            this.buildRequiredCount = b.buildRequiredCount;
            this.decayPhaseTable = b.decayPhaseTable;
            this.decayRequiredCount = b.decayRequiredCount;
            this.outputItems = List.copyOf(b.outputItems);
            this.outputFluids = List.copyOf(b.outputFluids);
        }

        public static Builder builder(String trace, Kind kind) {
            return new Builder(trace, kind);
        }

        /**
         * 既存 TraceInfo を Builder に復元する (= 一部 field のみ差し替えて再 register する用)。
         * KubeJS から呼ぶ際の標準パターン: {@code info.toBuilder().outputItems(...).build()} → register。
         */
        public Builder toBuilder() {
            Builder b = new Builder(this.trace, this.kind)
                    .displayKey(this.displayKey)
                    .noteKey(this.noteKey)
                    .starterHints(this.starterItemHints.toArray(new String[0]))
                    .continuousHints(this.continuousItemHints.toArray(new String[0]))
                    .requiredAmount(this.requiredAmount)
                    .maturityDuration(this.maturityDurationTicks)
                    .maturityEUt(this.maturityEUt)
                    .outputHints(this.outputHints.toArray(new String[0]));
            if (this.buildPhaseTable != null) b.buildPhase(this.buildPhaseTable, this.buildRequiredCount);
            if (this.decayPhaseTable != null) b.decayPhase(this.decayPhaseTable, this.decayRequiredCount);
            b.outputItems(this.outputItems.toArray(new ItemStack[0]));
            b.outputFluids(this.outputFluids.toArray(new FluidStack[0]));
            return b;
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
            private PhaseProgressionTable buildPhaseTable;
            private long buildRequiredCount = 0L;
            private PhaseProgressionTable decayPhaseTable;
            private long decayRequiredCount = 0L;
            private List<ItemStack> outputItems = new ArrayList<>();
            private List<FluidStack> outputFluids = new ArrayList<>();

            private Builder(String trace, Kind kind) {
                this.trace = trace;
                this.kind = kind;
                this.displayKey = "item.gtcsolo.star_locus." + trace; // 既存翻訳を流用
                this.noteKey = "gtcsolo.starforge.trace." + trace + ".note";
            }

            public Builder displayKey(String key)           { this.displayKey = key; return this; }
            public Builder noteKey(String key)              { this.noteKey = key; return this; }
            public Builder starterHints(String... hints)    { this.starterItemHints = List.of(hints); return this; }
            public Builder continuousHints(String... hints) { this.continuousItemHints = List.of(hints); return this; }
            public Builder requiredAmount(long n)           { this.requiredAmount = n; return this; }
            public Builder maturityDuration(long ticks)     { this.maturityDurationTicks = ticks; return this; }
            public Builder maturityEUt(long eut)            { this.maturityEUt = eut; return this; }
            public Builder outputHints(String... hints)     { this.outputHints = List.of(hints); return this; }

            public Builder buildPhase(PhaseProgressionTable table, long requiredCount) {
                this.buildPhaseTable = table;
                this.buildRequiredCount = requiredCount;
                return this;
            }

            public Builder decayPhase(PhaseProgressionTable table, long requiredCount) {
                this.decayPhaseTable = table;
                this.decayRequiredCount = requiredCount;
                return this;
            }

            public Builder outputItems(ItemStack... stacks) {
                this.outputItems.addAll(Arrays.asList(stacks));
                return this;
            }

            public Builder outputFluids(FluidStack... fluids) {
                this.outputFluids.addAll(Arrays.asList(fluids));
                return this;
            }

            public TraceInfo build() { return new TraceInfo(this); }
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    // 順序保持の list と name→info の lookup を二本立てで保持。
    // ORDERED.indexOf(info) は O(n) だが trace は最大数十件想定なので問題なし。
    private static final List<TraceInfo> ORDERED = new ArrayList<>();
    private static final Map<String, TraceInfo> BY_NAME = new HashMap<>();

    /**
     * 軌跡を登録する。 組み込み 8 軌跡は static initializer で登録される。
     * addon が新規軌跡を追加するときもこれを呼ぶ (FMLCommonSetupEvent タイミング推奨)。
     *
     * <p>既存 trace 名と一致した場合は **位置を維持したまま** 値だけ差し替える
     * (= KubeJS で要求アイテム / 出力を上書きする標準パターン)。
     * 位置も動かしたい場合は {@link #register(TraceInfo, OrderingHint)} を使う。
     */
    public static void register(TraceInfo info) {
        register(info, null);
    }

    /**
     * 順序指定付きで登録する。 既存 trace の場合は **元位置から外して** hint の位置に挿入。
     * hint が null なら既存位置維持 (= {@link #register(TraceInfo)} と同じ)。
     */
    public static void register(TraceInfo info, OrderingHint hint) {
        if (info == null) return;
        TraceInfo existing = BY_NAME.get(info.trace);
        if (existing != null) {
            int idx = ORDERED.indexOf(existing);
            if (hint == null) {
                ORDERED.set(idx, info);
                BY_NAME.put(info.trace, info);
                return;
            }
            ORDERED.remove(idx);
            BY_NAME.remove(info.trace);
        }
        int insertAt = (hint == null) ? ORDERED.size() : hint.resolveIndex(ORDERED, BY_NAME);
        insertAt = Math.max(0, Math.min(insertAt, ORDERED.size()));
        ORDERED.add(insertAt, info);
        BY_NAME.put(info.trace, info);
    }

    public static TraceInfo get(String trace) {
        return BY_NAME.get(trace);
    }

    /**
     * trace を削除する (= KubeJS で組み込み軌跡を無効化したい時用)。
     * 関連する {@link DIV.gtcsolo.item.AbstractLocusItem#VALID_TRACES} 側は別途
     * {@link DIV.gtcsolo.item.AbstractLocusItem#unregisterTrace(String)} で消す。
     */
    public static TraceInfo unregister(String trace) {
        TraceInfo removed = BY_NAME.remove(trace);
        if (removed != null) ORDERED.remove(removed);
        return removed;
    }

    /** 登録順 (= JEI 表示順) */
    public static List<TraceInfo> all() {
        return List.copyOf(ORDERED);
    }

    /**
     * 登録時の挿入位置を指定する hint。 既存 trace を再 register する際の移動先にも使える。
     *
     * <p>用例 (Java):
     * <pre>{@code
     * StarForgeTraceData.register(info, OrderingHint.before("brown_dwarf"));
     * StarForgeTraceData.register(info, OrderingHint.index(3));  // 1-based、 3 番目
     * }</pre>
     *
     * <p>用例 (KubeJS):
     * <pre>{@code
     * GtcsoloStarForge.register(info, GtcsoloStarForgeOrdering.before('brown_dwarf'))
     * }</pre>
     */
    public static abstract class OrderingHint {
        private OrderingHint() {}

        abstract int resolveIndex(List<TraceInfo> ordered, Map<String, TraceInfo> byName);

        public static OrderingHint first() {
            return new OrderingHint() {
                @Override int resolveIndex(List<TraceInfo> ordered, Map<String, TraceInfo> byName) { return 0; }
            };
        }

        public static OrderingHint last() {
            return new OrderingHint() {
                @Override int resolveIndex(List<TraceInfo> ordered, Map<String, TraceInfo> byName) { return ordered.size(); }
            };
        }

        /** target の直前 (= target は 1 つ後ろにずれる)。 target 不在なら warn + last() fallback */
        public static OrderingHint before(String target) {
            return new OrderingHint() {
                @Override int resolveIndex(List<TraceInfo> ordered, Map<String, TraceInfo> byName) {
                    TraceInfo t = byName.get(target);
                    if (t == null) {
                        LOGGER.warn("[StarForge] OrderingHint.before('{}'): trace not found, falling back to last()", target);
                        return ordered.size();
                    }
                    return ordered.indexOf(t);
                }
            };
        }

        /** target の直後。 target 不在なら warn + last() fallback */
        public static OrderingHint after(String target) {
            return new OrderingHint() {
                @Override int resolveIndex(List<TraceInfo> ordered, Map<String, TraceInfo> byName) {
                    TraceInfo t = byName.get(target);
                    if (t == null) {
                        LOGGER.warn("[StarForge] OrderingHint.after('{}'): trace not found, falling back to last()", target);
                        return ordered.size();
                    }
                    return ordered.indexOf(t) + 1;
                }
            };
        }

        /**
         * 1-based の絶対位置。 既存 trace を後ろに押し出す形で挿入。
         * 範囲外 (n &lt; 1 or n &gt; size+1) は warn + clamp。
         */
        public static OrderingHint index(int n) {
            return new OrderingHint() {
                @Override int resolveIndex(List<TraceInfo> ordered, Map<String, TraceInfo> byName) {
                    int max = ordered.size() + 1;
                    if (n < 1 || n > max) {
                        LOGGER.warn("[StarForge] OrderingHint.index({}): out of range [1, {}], clamping", n, max);
                    }
                    return n - 1;  // clamp は呼び出し側でやる
                }
            };
        }
    }

    /**
     * 通常型 6 軌跡共通の進捗テーブル (ver.0.5 spec)。
     * 構築・崩壊フェイズで同じ table を使う (= 「記述なければスタートと同じ」 設計判断)。
     *   - 標準: stone / cobblestone / dirt / gtceu:stone_dust = それぞれ +1
     *   - Effective: 空 star_locus (NBT 無し) = +1296
     */
    private static PhaseProgressionTable buildNormalStarterTable() {
        Item stoneDust = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "stone_dust"));
        return PhaseProgressionTable.builder()
                .addDefault(Items.STONE, Items.COBBLESTONE, Items.DIRT, stoneDust)
                .addEffective(
                        // 空 star_locus = NBT 無し (Trace 未設定) の star_locus
                        stack -> stack.is(ModItems.STAR_LOCUS.get())
                                && AbstractLocusItem.isEmpty(stack),
                        1296L,
                        // 表示は NBT 無し版 (= 空)
                        new ItemStack(ModItems.STAR_LOCUS.get())
                )
                .build();
    }

    private static boolean initialized = false;

    /**
     * 全 8 軌跡を登録する。 GTCEu materials / fluids 解決が必要なので
     * 静的初期化ではなく FMLCommonSetupEvent (= 全 material 登録完了後) から呼ぶ。
     */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // 通常型 6 軌跡 — 構築 12,960 / 崩壊 65,536、 両フェイズ同じ進捗テーブル
        PhaseProgressionTable normalTable = buildNormalStarterTable();
        long buildReq = 12_960L;
        long decayReq = 65_536L;

        // ver.0.5b — 出力 さらに ×15 増量 (= 元設計の ~150-750x) + 種類拡充 (天文学根拠で説得力)
        // 実在元素は GTCEu material 使用、 架空系は gtcsolo material

        // brown_dwarf — 失敗星、 大量の冷却ガス + 軽元素 + 岩石コア
        register(TraceInfo.builder(AbstractLocusItem.BROWN_DWARF, Kind.NORMAL)
                .buildPhase(normalTable, buildReq)
                .decayPhase(normalTable, decayReq)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.BROWN_DWARF_CORE, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Lithium, 3840),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Iron, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Carbon, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Silicon, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Magnesium, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Sodium, 720),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Potassium, 480))
                .outputFluids(
                        GTMaterials.Hydrogen.getFluid(FluidStorageKeys.GAS, 960000),
                        GTMaterials.Helium.getFluid(FluidStorageKeys.GAS, 480000),
                        GTMaterials.Methane.getFluid(FluidStorageKeys.GAS, 240000),
                        GTMaterials.Ammonia.getFluid(FluidStorageKeys.GAS, 120000),
                        GTMaterials.Water.getFluid(240000))
                .build());

        // koi74 — 縮退白色矮星、 結晶化炭素 + Fe-Ni 内核 + 残存外層
        register(TraceInfo.builder(AbstractLocusItem.KOI74, Kind.NORMAL)
                .buildPhase(normalTable, buildReq)
                .decayPhase(normalTable, decayReq)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.DEGENERATE_CARBON, 1920),
                        ChemicalHelper.get(TagPrefix.gem, ModMaterials.WHITE_DWARF_SHARD, 960),
                        ChemicalHelper.get(TagPrefix.gem, GTMaterials.Diamond, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Carbon, 7680),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Iron, 3840),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Nickel, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Silicon, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Cobalt, 480))
                .outputFluids(
                        GTMaterials.Oxygen.getFluid(FluidStorageKeys.GAS, 480000),
                        GTMaterials.Helium.getFluid(FluidStorageKeys.GAS, 240000),
                        GTMaterials.Neon.getFluid(FluidStorageKeys.GAS, 120000))
                .build());

        // r_andromedae — Mira 型 AGB、 s 過程重元素 + SiC ダスト
        register(TraceInfo.builder(AbstractLocusItem.R_ANDROMEDAE, Kind.NORMAL)
                .buildPhase(normalTable, buildReq)
                .decayPhase(normalTable, decayReq)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Technetium, 960),
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.PULSAR_DUST, 2880),
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.MIRA_SILICATE, 3840),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Barium, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Strontium, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Yttrium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Zirconium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Silicon, 2880),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Tin, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Lead, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Carbon, 1920))
                .build());

        // hd101065 — プシビルスキ星、 異常重元素・希土類・超アクチノイド
        register(TraceInfo.builder(AbstractLocusItem.HD101065, Kind.NORMAL)
                .buildPhase(normalTable, buildReq)
                .decayPhase(normalTable, decayReq)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.ingot, ModMaterials.PRZYBYLSKI_ESSENCE, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Holmium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Dysprosium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Erbium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Promethium, 360),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Einsteinium, 240),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Californium, 120),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Cerium, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Lanthanum, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Neodymium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Praseodymium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Europium, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Samarium, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Terbium, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Gadolinium, 720))
                .build());

        // cemp_r — 炭素濃集 + r 過程 (Au, Pt, U, Th, Pb 等の重元素)
        register(TraceInfo.builder(AbstractLocusItem.CEMP_R, Kind.NORMAL)
                .buildPhase(normalTable, buildReq)
                .decayPhase(normalTable, decayReq)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.ingot, ModMaterials.R_PROCESS_METAL, 1440),
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.CEMP_CARBON_DUST, 7680),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Carbon, 7680),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Gold, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Platinum, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Thorium, 720),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Uranium238, 480),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Lead, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Europium, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Gadolinium, 720),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Samarium, 720),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Silver, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Barium, 960))
                .build());

        // neutron_star — 大量ニュートロニウム + バリオン結晶 + kilonova r 過程産物
        register(TraceInfo.builder(AbstractLocusItem.NEUTRON_STAR, Kind.NORMAL)
                .buildPhase(normalTable, buildReq)
                .decayPhase(normalTable, decayReq)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.ingot, ModMaterials.HYPERX_NEUTRONIUM, 960),
                        ChemicalHelper.get(TagPrefix.gem, ModMaterials.BARYON_CRYSTAL, 480),
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.STRANGE_MATTER, 960),
                        ChemicalHelper.get(TagPrefix.gem, ModMaterials.MAGNETAR_CRYSTAL, 240),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Neutronium, 3840),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Iron, 3840),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Nickel, 1920),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Gold, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Platinum, 480),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Uranium238, 480),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Thorium, 480),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Lead, 1440))
                .outputFluids(
                        ModMaterials.QUARK_SOUP.getFluid(60000))
                .build());

        // 成熟型 (太陽) — 核融合産物 + コロナ + ダイソンキューブ収穫
        register(TraceInfo.builder(AbstractLocusItem.SUN, Kind.MATURITY_SUN)
                .maturityDuration(0L)
                .maturityEUt(0L)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.CORONIUM, 1920),
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.PHOTONIC_DUST, 3840),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Iron, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Carbon, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Calcium, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Magnesium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Silicon, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Sulfur, 480),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Nickel, 480))
                .outputFluids(
                        ModMaterials.SOLAR_PLASMA.getFluid(FluidStorageKeys.PLASMA, 480000),
                        GTMaterials.Helium.getFluid(FluidStorageKeys.GAS, 960000),
                        GTMaterials.Hydrogen.getFluid(FluidStorageKeys.GAS, 240000),
                        GTMaterials.Nitrogen.getFluid(FluidStorageKeys.GAS, 60000),
                        GTMaterials.Oxygen.getFluid(FluidStorageKeys.GAS, 120000))
                .build());

        // 成熟型 (ブラックホール) — 重力特異点 + 事象地平線 + ホーキング放射 + r 過程降着
        register(TraceInfo.builder(AbstractLocusItem.BLACK_HOLE, Kind.MATURITY_BLACK_HOLE)
                .maturityDuration(0L)
                .maturityEUt(0L)
                .outputItems(
                        ChemicalHelper.get(TagPrefix.ingot, ModMaterials.STAR_SINGULARITY, 240),
                        ChemicalHelper.get(TagPrefix.gem, ModMaterials.EVENT_HORIZON_SHARD, 480),
                        ChemicalHelper.get(TagPrefix.ingot, ModMaterials.ACCRETION_ALLOY, 960),
                        ChemicalHelper.get(TagPrefix.dust, ModMaterials.GRAVITATIONAL_RESIDUE, 1920),
                        ChemicalHelper.get(TagPrefix.gem, ModMaterials.SPACETIME_FRAGMENT, 120),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Neutronium, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Iron, 1440),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Gold, 960),
                        ChemicalHelper.get(TagPrefix.dust, GTMaterials.Platinum, 480))
                .outputFluids(
                        ModMaterials.HAWKING_RADIATION.getFluid(FluidStorageKeys.PLASMA, 120000),
                        ModMaterials.PENROSE_ERGO.getFluid(60000))
                .build());
    }
}
