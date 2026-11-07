package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import mekanism.api.math.FloatingLong;
import mekanism.common.util.MekanismUtils;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the carried block's energy capability and tooltip consistent with its placed storage. */
@Mixin(value = MekanismUtils.class, remap = false)
public abstract class GasGeneratorItemCapacityMixin {
    @Inject(method = "getMaxEnergy(Lnet/minecraft/world/item/ItemStack;Lmekanism/api/math/FloatingLong;)Lmekanism/api/math/FloatingLong;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtcsolo$gasItemCapacity(ItemStack stack, FloatingLong base,
                                               CallbackInfoReturnable<FloatingLong> cir) {
        if (GasGeneratorUpgrades.isTarget(stack)) cir.setReturnValue(GasGeneratorUpgrades.itemCapacity(stack, base));
    }
}
