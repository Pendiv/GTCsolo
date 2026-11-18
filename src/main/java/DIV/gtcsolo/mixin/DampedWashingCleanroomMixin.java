package DIV.gtcsolo.mixin;

import DIV.gtcsolo.block.DampedWashingFilterType;
import DIV.gtcsolo.machine.DampedWashingCleanroomMachine;
import DIV.gtcsolo.recipe.condition.GtcsoloRecipeConditions;
import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CleanroomMachine;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = CleanroomMachine.class, remap = false)
public abstract class DampedWashingCleanroomMixin {
    @Shadow private int cleanAmount;

    @Redirect(method = "getPattern", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/pattern/Predicates;cleanroomFilters()Lcom/gregtechceu/gtceu/api/pattern/TraceabilityPredicate;"))
    private TraceabilityPredicate gtcsolo$filterLayer() {
        if ((Object) this instanceof DampedWashingCleanroomMachine) {
            var filter = ModBlocks.DAMPED_WASHING_FILTER_CASING.get();
            return Predicates.custom(state -> {
                if (!state.getBlockState().is(filter)) return false;
                state.getMatchContext().getOrPut("FilterType", DampedWashingFilterType.INSTANCE);
                return true;
            }, () -> new BlockInfo[]{BlockInfo.fromBlock(filter)});
        }
        return Predicates.cleanroomFilters();
    }

    @Inject(method = "getTypes", at = @At("RETURN"), cancellable = true)
    private void gtcsolo$provideDampedWashingCleanroom(CallbackInfoReturnable<Set<CleanroomType>> callback) {
        var damped = GtcsoloRecipeConditions.DAMPED_WASHING_CLEANROOM;
        if (!callback.getReturnValue().contains(damped)) return;

        callback.setReturnValue(cleanAmount == 100
                ? Set.of(CleanroomType.STERILE_CLEANROOM, damped)
                : Set.of(CleanroomType.STERILE_CLEANROOM));
    }
}
