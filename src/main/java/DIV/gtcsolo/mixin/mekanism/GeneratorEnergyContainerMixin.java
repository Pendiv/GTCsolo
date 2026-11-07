package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import mekanism.api.IContentsListener;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use the native upgrade-aware container only for the gas generator. */
@Pseudo
@Mixin(targets = "mekanism.generators.common.tile.TileEntityGenerator", remap = false)
public abstract class GeneratorEnergyContainerMixin {
    @Redirect(method = "getInitialEnergyContainers", at = @At(value = "INVOKE",
            target = "Lmekanism/common/capabilities/energy/BasicEnergyContainer;output(Lmekanism/api/math/FloatingLong;Lmekanism/api/IContentsListener;)Lmekanism/common/capabilities/energy/BasicEnergyContainer;"), remap = false)
    private BasicEnergyContainer gtcsolo$upgradeAwareStorage(FloatingLong base, IContentsListener listener) {
        return GasGeneratorUpgrades.createEnergyContainer((TileEntityMekanism) (Object) this, base, listener);
    }
}
