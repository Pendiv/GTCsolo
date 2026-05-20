package DIV.gtcsolo.machine.starforge;

/**
 * StarForge の軌跡 Kind ごとの動作を実装する Strategy インターフェース。
 *
 * <p>{@link StarForgeMachine} は構築フェイズを共通実装で扱い、 成熟/崩壊フェイズと
 * 出力タイミングを Kind 固有 logic に委譲する。
 *
 * <p>各メソッドは server-side tick から呼ばれる。 client 側からは呼ばれない。
 */
public interface StarForgeKindLogic {

    /** tickDecay の結果コード。 orchestrator はこれで出力分岐を判断する。 */
    enum DecayResult {
        /** 崩壊フェイズ継続。 何もしない */
        CONTINUE,
        /** 成功完了。 出力搬出 + IDLE 復帰 (Kind ごとに emitOutputs 呼ばれる) */
        SUCCESS,
        /** 失敗完了。 出力なし、 IDLE 復帰のみ */
        FAILURE
    }

    /**
     * 構築フェイズが完了した直後の遷移先を返す。
     */
    StarForgeMachine.Phase nextPhaseAfterBuild();

    /**
     * 成熟フェイズの 1 tick を処理する。 NORMAL は呼ばれない。
     * @return true なら次 tick で崩壊フェイズへ遷移
     */
    default boolean tickMaturity(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        return false;
    }

    /**
     * 崩壊フェイズの 1 tick を処理する。
     * @return {@link DecayResult} 結果コード
     */
    DecayResult tickDecay(StarForgeMachine machine, StarForgeTraceData.TraceInfo info);

    /**
     * 崩壊成功時に「最速で」 出力する朽ち果てた星の軌跡。
     * orchestrator は 通常出力より先にこれを呼ぶ。
     * デフォルト: {@code AbstractLocusItem.of(decaying_star_locus, info.trace)} を 1 個出力。
     * BH 等 「軌跡を返さない」 Kind は override して no-op に。
     */
    default void emitDecayingLocus(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        net.minecraft.world.item.ItemStack stack = DIV.gtcsolo.item.AbstractLocusItem.of(
                DIV.gtcsolo.registry.ModItems.DECAYING_STAR_LOCUS.get(), info.trace);
        machine.outputItem(stack);
    }

    /**
     * 崩壊フェイズが {@link DecayResult#SUCCESS} で終わったときに呼ばれる出力処理。
     * デフォルトでは {@link StarForgeMachine#emitCommonOutputs} を呼ぶ。
     */
    default void emitOutputsOnSuccess(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        machine.emitCommonOutputs(info);
    }

    /**
     * フェイズリセット時に内部 state を初期化したいときに呼ばれる。
     * 例: BH の崩壊度や閾値乱数を新規生成。
     */
    default void onReset(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        // default: no-op
    }

    /**
     * BUILD → MATURITY 遷移時の特殊初期化。 BH の崩壊度乱数生成等。
     * 共通実装で BUILD 完了直後に呼ばれる。
     */
    default void onMaturityStart(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        // default: no-op
    }

    /**
     * MATURITY → DECAY (or BUILD → DECAY for NORMAL) 遷移時の特殊初期化。
     */
    default void onDecayStart(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        // default: no-op
    }

    /**
     * Kind に応じた logic を生成。
     */
    static StarForgeKindLogic forKind(StarForgeTraceData.Kind kind) {
        return switch (kind) {
            case NORMAL -> NormalKindLogic.INSTANCE;
            case MATURITY_SUN -> SunKindLogic.INSTANCE;
            case MATURITY_BLACK_HOLE -> BlackHoleKindLogic.INSTANCE;
        };
    }
}
