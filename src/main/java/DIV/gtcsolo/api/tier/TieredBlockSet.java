package DIV.gtcsolo.api.tier;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 「電圧tier ↔ 構造ブロック」の対応表。
 * GTValues 互換の voltage tier (LV=1 .. MAX=14) と Block の写像を保持し、
 * - マルチブロック構造で同 tier ブロックの統一性を強制する Predicate
 * - 形成済マルチブロックから現在の tier を取り出すヘルパー
 * - JEI shapeInfos プレビューを tier ごとに展開するヘルパー
 * を提供する。
 *
 * GTCEu 既存のコイル機構 (ICoilType / heatingCoils()) とは独立した別系統。
 */
public final class TieredBlockSet {

    private final String contextKey;
    private final SortedMap<Integer, Supplier<? extends Block>> tiers;
    private final String errorTranslationKey;

    public TieredBlockSet(String contextKey,
                          Map<Integer, Supplier<? extends Block>> tiers,
                          String errorTranslationKey) {
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("TieredBlockSet must have at least one tier");
        }
        this.contextKey = contextKey;
        this.tiers = Collections.unmodifiableSortedMap(new TreeMap<>(tiers));
        this.errorTranslationKey = errorTranslationKey;
    }

    public String getContextKey() {
        return contextKey;
    }

    public Set<Integer> getTiers() {
        return tiers.keySet();
    }

    public Block getBlockForTier(int tier) {
        Supplier<? extends Block> s = tiers.get(tier);
        return s == null ? null : s.get();
    }

    /**
     * tier ブロック専用の Predicate。
     * - 候補ブロックのいずれかにマッチした最初のスロットで tier を MatchContext に固定
     * - 以降のスロットで違う tier ブロックが来たら統一性違反として PatternStringError を立てる
     */
    public TraceabilityPredicate predicate() {
        SimplePredicate sp = new SimplePredicate("tier_block",
                state -> {
                    var blockState = state.getBlockState();
                    for (Map.Entry<Integer, Supplier<? extends Block>> entry : tiers.entrySet()) {
                        Block tierBlock = entry.getValue().get();
                        if (blockState.is(tierBlock)) {
                            Object current = state.getMatchContext().getOrPut(contextKey, tierBlock);
                            if (current != tierBlock) {
                                state.setError(new PatternStringError(errorTranslationKey));
                                return false;
                            }
                            return true;
                        }
                    }
                    return false;
                },
                () -> {
                    BlockInfo[] arr = new BlockInfo[tiers.size()];
                    int i = 0;
                    for (Supplier<? extends Block> s : tiers.values()) {
                        arr[i++] = BlockInfo.fromBlockState(s.get().defaultBlockState());
                    }
                    return arr;
                });
        TraceabilityPredicate result = new TraceabilityPredicate(sp);
        result.addTooltips(Component.translatable(errorTranslationKey));
        return result;
    }

    /**
     * 形成済マルチブロックの MatchContext から tier を取り出す。
     * 未形成 / 未マッチなら -1。
     */
    public int getTierFromMachine(IMultiController machine) {
        if (!machine.isFormed()) return -1;
        Object found = machine.getMultiblockState().getMatchContext().get(contextKey);
        if (!(found instanceof Block fb)) return -1;
        for (Map.Entry<Integer, Supplier<? extends Block>> entry : tiers.entrySet()) {
            if (entry.getValue().get() == fb) return entry.getKey();
        }
        return -1;
    }

    /**
     * 各 tier ごとに ShapeInfoBuilder を生成し、tierBlockSymbol の位置にその tier のブロックを差し替えて
     * MultiblockShapeInfo を tier 数だけ返す。JEI のマルチブロックプレビューが自動で切替UIを付ける。
     */
    public List<MultiblockShapeInfo> generateShapeInfos(
            Function<Integer, MultiblockShapeInfo.ShapeInfoBuilder> builderFactory,
            char tierBlockSymbol) {
        List<MultiblockShapeInfo> list = new ArrayList<>();
        for (Map.Entry<Integer, Supplier<? extends Block>> entry : tiers.entrySet()) {
            int tier = entry.getKey();
            Block block = entry.getValue().get();
            list.add(builderFactory.apply(tier).where(tierBlockSymbol, block).build());
        }
        return list;
    }

    public static Builder builder(String contextKey) {
        return new Builder(contextKey);
    }

    public static final class Builder {
        private final String contextKey;
        private final Map<Integer, Supplier<? extends Block>> tiers = new HashMap<>();
        private String errorTranslationKey = "gtcsolo.multiblock.error.tier_block_mismatch";

        private Builder(String contextKey) {
            this.contextKey = contextKey;
        }

        public Builder tier(int voltageTier, Supplier<? extends Block> block) {
            tiers.put(voltageTier, block);
            return this;
        }

        public Builder errorKey(String key) {
            this.errorTranslationKey = key;
            return this;
        }

        public TieredBlockSet build() {
            return new TieredBlockSet(contextKey, tiers, errorTranslationKey);
        }
    }
}
