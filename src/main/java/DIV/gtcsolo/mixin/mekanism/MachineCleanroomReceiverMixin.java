package DIV.gtcsolo.mixin.mekanism;

import com.gregtechceu.gtceu.api.capability.ICleanroomReceiver;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import mekanism.common.capabilities.resolver.BasicCapabilityResolver;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.CapabilityTileEntity;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class MachineCleanroomReceiverMixin extends CapabilityTileEntity implements ICleanroomReceiver {
    @Unique @Nullable private ICleanroomProvider gtcsolo$room;

    protected MachineCleanroomReceiverMixin(TileEntityTypeRegistryObject<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtcsolo$registerReceiver(CallbackInfo ci) {
        if (this instanceof IRecipeLookupHandler<?>) {
            addCapabilityResolver(BasicCapabilityResolver.constant(GTCapability.CAPABILITY_CLEANROOM_RECEIVER, this));
        }
    }

    @Override
    public @Nullable ICleanroomProvider getCleanroom() {
        return gtcsolo$room;
    }

    @Override
    public void setCleanroom(@Nullable ICleanroomProvider provider) {
        gtcsolo$room = provider;
    }
}
