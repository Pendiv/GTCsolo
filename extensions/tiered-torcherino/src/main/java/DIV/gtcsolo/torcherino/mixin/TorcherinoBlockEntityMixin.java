package DIV.gtcsolo.torcherino.mixin;

import DIV.gtcsolo.torcherino.Budget;
import DIV.gtcsolo.torcherino.BudgetEntity;
import DIV.gtcsolo.torcherino.Policies;
import DIV.gtcsolo.torcherino.Profile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import torcherino.block.entity.TorcherinoBlockEntity;

@Mixin(value = TorcherinoBlockEntity.class, remap = false)
public abstract class TorcherinoBlockEntityMixin implements BudgetEntity {
    @Shadow private int xRange;
    @Shadow private int yRange;
    @Shadow private int zRange;
    @Shadow private int speed;
    @Shadow private Iterable<BlockPos> area;

    @Override public Budget.State tiered$getState() { return new Budget.State(xRange, yRange, zRange, speed); }
    @Override public void tiered$setState(Budget.State state) {
        xRange = state.x(); yRange = state.y(); zRange = state.z(); speed = state.speed();
        BlockPos pos = ((TorcherinoBlockEntity) (Object) this).getBlockPos();
        area = BlockPos.betweenClosed(pos.offset(-state.x(), -state.y(), -state.z()), pos.offset(state.x(), state.y(), state.z()));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tiered$baseline(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (Profile.from(state) != null) speed = 1;
    }
    @Inject(method = "readClientData", at = @At("HEAD"), cancellable = true)
    private void tiered$validate(int x, int z, int y, int requestedSpeed, int mode, CallbackInfoReturnable<Boolean> cir) {
        int power = Policies.power(((TorcherinoBlockEntity) (Object) this).getBlockState());
        if (power >= 0 && !Budget.valid(new Budget.State(x, y, z, requestedSpeed), power)) cir.setReturnValue(false);
    }
    // Third-party overrides retain their own names; cover dev and production MC mappings explicitly.
    @Inject(method = {"load", "m_142466_"}, at = @At("HEAD"))
    private void tiered$sanitizeSave(CompoundTag tag, CallbackInfo ci) {
        // A remote server may use a different COMMON config. Its saved state and tier
        // synchronization are authoritative; never clamp client data using local settings.
        if (net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()) return;
        int power = Policies.power(((TorcherinoBlockEntity) (Object) this).getBlockState());
        if (power < 0) return;
        Budget.State state = Budget.normalize(new Budget.State(tag.getInt("XRange"), tag.getInt("YRange"),
                tag.getInt("ZRange"), tag.getInt("Speed")), power);
        tag.putInt("XRange", state.x()); tag.putInt("YRange", state.y()); tag.putInt("ZRange", state.z());
        tag.putInt("Speed", state.speed());
        tag.putInt("RedstoneMode", Math.max(0, Math.min(3, tag.getInt("RedstoneMode"))));
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private static void tiered$validateTick(Level level, BlockPos pos, BlockState blockState,
                                           TorcherinoBlockEntity entity, CallbackInfo ci) {
        if (level.isClientSide) return;
        int power = Policies.power(blockState);
        if (power < 0) return;
        BudgetEntity access = (BudgetEntity) entity;
        Budget.State old = access.tiered$getState();
        if (Budget.valid(old, power)) return;
        Budget.State safe = Budget.normalize(old, power);
        access.tiered$setState(safe);
        entity.setChanged();
    }
}
