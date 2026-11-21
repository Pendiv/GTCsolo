package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.MachineVoltage;
import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.factory.TileEntityFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MachineEnergyContainer.class, remap = false)
public abstract class MachinePacketCapacityMixin {
    @Shadow @Final protected TileEntityMekanism tile;

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true)
    private void gtcsolo$holdInputPackets(CallbackInfoReturnable<FloatingLong> result) {
        if (tile instanceof IRecipeLookupHandler<?>) {
            // A fixed floor avoids discarding stored power when upgrades are removed.
            int slots = tile instanceof TileEntityFactory<?> ? 1 + SlotUpgrade.MAX : 1;
            result.setReturnValue(result.getReturnValue().max(MachineVoltage.joules(MachineVoltage.rating(tile), 2L * slots)));
        }
    }
}
