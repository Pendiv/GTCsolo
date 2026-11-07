package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorSupportGas;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import DIV.gtcsolo.integration.mekanism.generator.SupportGasGenerator;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.api.chemical.gas.Gas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "mekanism.generators.common.tile.TileEntityGasGenerator", remap = false)
public abstract class GasGeneratorSupportGasMixin implements SupportGasGenerator {
    @Unique private GasGeneratorSupportGas gtcsolo$supportGas;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "getInitialGasTanks", at = @At(value = "INVOKE",
            target = "Lmekanism/common/capabilities/holder/chemical/ChemicalTankHelper;addTank(Lmekanism/api/chemical/IChemicalTank;[Lmekanism/api/RelativeSide;)Lmekanism/api/chemical/IChemicalTank;"), remap = false)
    private IChemicalTank gtcsolo$addSupportTank(ChemicalTankHelper builder, IChemicalTank fuel,
                                                RelativeSide[] sides, IContentsListener listener) {
        builder.addTank(fuel, sides);
        var tile = (TileEntityMekanism) (Object) this;
        if (GasGeneratorUpgrades.isTarget(tile)) {
            gtcsolo$supportGas = new GasGeneratorSupportGas(tile, (IGasTank) fuel, listener);
            var access = (ChemicalTankValidatorAccess) fuel;
            var original = (java.util.function.Predicate) access.gtcsolo$getValidator();
            access.gtcsolo$setValidator(gas -> original.test(gas) || gtcsolo$supportGas.acceptsGas((Gas) gas));
        }
        return fuel;
    }

    @Override public GasGeneratorSupportGas gtcsolo$getSupportGas() { return gtcsolo$supportGas; }
}
