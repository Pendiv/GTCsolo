package DIV.gtcsolo.mixin.mekanism;

import java.util.List;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.tile.prefab.TileEntityProgressMachine;
import mekanism.common.tile.prefab.TileEntityRecipeMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityProgressMachine.class, remap = false)
public abstract class ProgressMachineDurationMixin extends TileEntityRecipeMachine<MekanismRecipe> {
    @Shadow public abstract int getOperatingTicks();
    @Unique private int gtcsolo$displayDuration;

    protected ProgressMachineDurationMixin(IBlockProvider block, BlockPos pos, BlockState state, List<RecipeError> errors) {
        super(block, pos, state, errors);
    }

    @Inject(method = "addContainerTrackers", at = @At("TAIL"))
    private void gtcsolo$trackDuration(MekanismContainer container, CallbackInfo ci) {
        container.track(SyncableInt.create(() -> {
            CachedRecipe<?> cached = recipeCacheLookupMonitor.getCachedRecipe(0);
            return cached == null ? 0 : ((CachedRecipeConditionsAccess) cached).gtcsolo$getRequiredTicks().getAsInt();
        }, ticks -> gtcsolo$displayDuration = ticks));
    }

    @Inject(method = "getScaledProgress", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$scaleProgress(CallbackInfoReturnable<Double> result) {
        if (gtcsolo$displayDuration > 0) result.setReturnValue((double) getOperatingTicks() / gtcsolo$displayDuration);
    }
}
