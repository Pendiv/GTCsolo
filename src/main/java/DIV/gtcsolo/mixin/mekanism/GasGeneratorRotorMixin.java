package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorRotor;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorSupportGas;
import DIV.gtcsolo.integration.mekanism.generator.RotorDrivenGenerator;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.gas.attribute.GasAttributes.Fuel;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mekanism.generators.common.tile.TileEntityGasGenerator", remap = false)
public abstract class GasGeneratorRotorMixin implements RotorDrivenGenerator {
    @Unique private GasGeneratorRotor gtcsolo$rotor;
    @Shadow private FloatingLong generationRate;

    @Redirect(method = "getInitialInventory", at = @At(value = "INVOKE",
            target = "Lmekanism/common/capabilities/holder/slot/InventorySlotHelper;build()Lmekanism/common/capabilities/holder/slot/IInventorySlotHolder;"), remap = false)
    private IInventorySlotHolder gtcsolo$addRotorSlot(InventorySlotHelper builder, IContentsListener listener) {
        var tile = (TileEntityMekanism) (Object) this;
        if (GasGeneratorUpgrades.isTarget(tile)) {
            gtcsolo$rotor = new GasGeneratorRotor(tile, listener);
            builder.addSlot(gtcsolo$rotor.slot(), RelativeSide.BACK, RelativeSide.TOP, RelativeSide.BOTTOM);
        }
        return builder.build();
    }

    @Override public GasGeneratorRotor gtcsolo$getRotor() { return gtcsolo$rotor; }

    @Inject(method = "getToUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$rotorEfficiency(CallbackInfoReturnable<Long> callback) {
        if (gtcsolo$rotor == null) return;
        var tile = (TileEntityMekanism) (Object) this;
        var fuel = tile.getGasTanks(null).get(0).getType().get(Fuel.class);
        generationRate = fuel == null ? FloatingLong.ZERO : gtcsolo$rotor.generationRate(fuel.getEnergyPerTick());
        if (generationRate.isZero()) callback.setReturnValue(0L);
    }

    @Inject(method = "getToUse", at = @At("RETURN"), cancellable = true, remap = false)
    private void gtcsolo$wearRotor(CallbackInfoReturnable<Long> callback) {
        long used = callback.getReturnValue();
        var support = GasGeneratorSupportGas.of((TileEntityMekanism) (Object) this);
        if (support != null) used = support.consume(used, generationRate);
        callback.setReturnValue(used);
        if (gtcsolo$rotor != null && used > 0) gtcsolo$rotor.onBurn();
    }
}
