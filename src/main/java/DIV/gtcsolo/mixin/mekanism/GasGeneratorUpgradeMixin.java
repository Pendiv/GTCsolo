package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import mekanism.api.Upgrade;
import mekanism.api.math.FloatingLong;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Increase the native fuel budget; its remaining-fuel and free-storage limits still apply afterward. */
@Pseudo
@Mixin(targets = "mekanism.generators.common.tile.TileEntityGasGenerator", remap = false)
public abstract class GasGeneratorUpgradeMixin implements Upgrade.IUpgradeInfoHandler {
    @Shadow private FloatingLong generationRate;
    @Redirect(method = "getToUse", at = @At(value = "INVOKE", target = "Ljava/lang/Math;ceil(D)D"), remap = false)
    private double gtcsolo$upgradeBurnLimit(double nativeLimit) {
        return GasGeneratorUpgrades.burnLimit((TileEntityMekanism) (Object) this, nativeLimit, generationRate);
    }

    @Override public List<Component> getInfo(Upgrade upgrade) {
        return GasGeneratorUpgrades.upgradeInfo((TileEntityMekanism) (Object) this, upgrade);
    }
}
