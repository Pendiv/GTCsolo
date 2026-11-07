package DIV.gtcsolo.mixin;

import DIV.gtcsolo.api.pattern.GroupedPatternAccess;
import DIV.gtcsolo.api.pattern.GroupedPatternPlan;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No plan means no cancellation or mutation: ordinary GT patterns retain their native path. */
@Mixin(value = BlockPattern.class, remap = false)
public abstract class GroupedBlockPatternMixin implements GroupedPatternAccess {
    @Shadow @Final @Mutable public int[][] aisleRepetitions;
    @Shadow @Final public RelativeDirection[] structureDir;
    @Shadow @Final protected TraceabilityPredicate[][][] blockMatches;
    @Shadow @Final protected int[] centerOffset;
    @Shadow protected int[] formedRepetitionCount;
    @Unique private GroupedPatternPlan gtcsolo$groupPlan;

    @Override
    public boolean gtcsolo$hasGroupedPattern() { return gtcsolo$groupPlan != null; }

    @Override
    public void gtcsolo$attachGroupedPattern(GroupedPatternPlan plan) {
        if (gtcsolo$groupPlan != null) throw new IllegalStateException("Pattern already has module repetition");
        // The unhooked native clone prevents the minimum variant from re-entering this hook.
        plan.useNativeMinimum(new BlockPattern(blockMatches, structureDir.clone(), aisleRepetitions, centerOffset.clone()));
        gtcsolo$groupPlan = plan;
        aisleRepetitions = new int[][]{{plan.minimum(), plan.maximum()}};
        formedRepetitionCount = new int[1];
    }

    @Inject(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void gtcsolo$matchModules(MultiblockState state, boolean save, CallbackInfoReturnable<Boolean> cir) {
        if (gtcsolo$groupPlan == null) return;
        boolean result = gtcsolo$groupPlan.match(state, save);
        formedRepetitionCount[0] = gtcsolo$groupPlan.formedCount();
        cir.setReturnValue(result);
    }

    @Inject(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z",
            at = @At("HEAD"), cancellable = true)
    private void gtcsolo$matchOrientedModules(MultiblockState state, BlockPos center, Direction front,
                                            Direction upwards, boolean flipped, boolean save,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (gtcsolo$groupPlan == null) return;
        boolean result = gtcsolo$groupPlan.match(state, center, front, upwards, flipped, save);
        formedRepetitionCount[0] = gtcsolo$groupPlan.formedCount();
        cir.setReturnValue(result);
    }

    @Inject(method = "getPreview([I)[[[Lcom/lowdragmc/lowdraglib/utils/BlockInfo;", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$previewModules(int[] repetition, CallbackInfoReturnable<BlockInfo[][][]> cir) {
        if (gtcsolo$groupPlan != null) cir.setReturnValue(gtcsolo$groupPlan.preview(repetition));
    }

    @Inject(method = "autoBuild(Lnet/minecraft/world/entity/player/Player;Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;)V",
            at = @At("HEAD"), cancellable = true)
    private void gtcsolo$buildModules(Player player, MultiblockState state, CallbackInfo ci) {
        if (gtcsolo$groupPlan == null) return;
        gtcsolo$groupPlan.autoBuild(player, state);
        ci.cancel();
    }
}
