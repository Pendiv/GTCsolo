package DIV.gtcsolo.machine.starforge;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * NORMAL 軌跡 (= 通常型 6 軌跡) 用の Strategy。
 * 仕様: docs/StarForge_spec.md §4
 */
public final class NormalKindLogic implements StarForgeKindLogic {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final NormalKindLogic INSTANCE = new NormalKindLogic();
    private NormalKindLogic() {}

    @Override
    public StarForgeMachine.Phase nextPhaseAfterBuild() {
        LOGGER.debug("[StarForge:Normal] BUILD complete -> DECAY");
        return StarForgeMachine.Phase.DECAY;
    }

    @Override
    public void onDecayStart(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        LOGGER.debug("[StarForge:Normal:{}] DECAY started, required count = {}",
                info.trace, info.decayRequiredCount);
    }

    @Override
    public DecayResult tickDecay(StarForgeMachine machine, StarForgeTraceData.TraceInfo info) {
        long gain = machine.consumeAndProgressFromTable(info.decayPhaseTable, /*budget=*/64);
        if (gain > 0) {
            machine.addDecayProgress(gain);
        }
        if (machine.getDecayProgress() >= info.decayRequiredCount) {
            LOGGER.debug("[StarForge:Normal:{}] DECAY complete (success)", info.trace);
            return DecayResult.SUCCESS;
        }
        return DecayResult.CONTINUE;
    }
}
