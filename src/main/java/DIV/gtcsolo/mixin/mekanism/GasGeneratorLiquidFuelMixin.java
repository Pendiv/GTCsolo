package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorLiquidFuel;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorRotor;
import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorSupportGas;
import DIV.gtcsolo.integration.mekanism.generator.LiquidFuelGenerator;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.base.SubstanceType;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ISustainedData;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mekanism.generators.common.tile.TileEntityGasGenerator", remap = false)
public abstract class GasGeneratorLiquidFuelMixin extends TileEntityMekanism implements LiquidFuelGenerator, ISustainedData {
    @Shadow private int maxBurnTicks;
    @Shadow private FloatingLong generationRate;
    @Shadow private double gasUsedLastTick;
    @Unique private GasGeneratorLiquidFuel gtcsolo$liquidFuel;
    @Unique private boolean gtcsolo$lastLiquidTick;

    protected GasGeneratorLiquidFuelMixin(IBlockProvider block, BlockPos pos, BlockState state) { super(block, pos, state); }

    @Override protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        if (!GasGeneratorUpgrades.isTarget(this)) return super.getInitialFluidTanks(listener);
        gtcsolo$liquidFuel = new GasGeneratorLiquidFuel(this, listener);
        var builder = FluidTankHelper.forSide(this::getDirection);
        builder.addTank(gtcsolo$liquidFuel.tank(), RelativeSide.LEFT, RelativeSide.RIGHT,
                RelativeSide.BACK, RelativeSide.TOP, RelativeSide.BOTTOM);
        return builder.build();
    }

    @Override public GasGeneratorLiquidFuel gtcsolo$getLiquidFuel() { return gtcsolo$liquidFuel; }

    @Override public void writeSustainedData(net.minecraft.nbt.CompoundTag data) {
        if (gtcsolo$liquidFuel != null) gtcsolo$liquidFuel.write(data);
        var rotor = GasGeneratorRotor.of(this);
        if (rotor != null) rotor.write(data);
        var support = GasGeneratorSupportGas.of(this);
        if (support != null) support.write(data);
    }
    @Override public void readSustainedData(net.minecraft.nbt.CompoundTag data) {
        if (gtcsolo$liquidFuel != null) gtcsolo$liquidFuel.read(data);
        var rotor = GasGeneratorRotor.of(this);
        if (rotor != null) rotor.read(data);
        var support = GasGeneratorSupportGas.of(this);
        if (support != null) support.read(data);
    }
    @Override public java.util.Map<String, String> getTileDataRemap() {
        return gtcsolo$liquidFuel == null ? java.util.Map.of()
                : java.util.Map.of(GasGeneratorLiquidFuel.SAVE_KEY, GasGeneratorLiquidFuel.SAVE_KEY,
                        GasGeneratorRotor.SAVE_KEY, GasGeneratorRotor.SAVE_KEY,
                        GasGeneratorSupportGas.SAVE_KEY, GasGeneratorSupportGas.SAVE_KEY);
    }

    @Inject(method = "makesComparatorDirty", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$trackLiquidLevel(SubstanceType type, CallbackInfoReturnable<Boolean> callback) {
        // This also runs while the native constructor selects the tank's change listener.
        if (type == SubstanceType.FLUID && GasGeneratorUpgrades.isTarget(this)) callback.setReturnValue(true);
    }

    @Inject(method = "getRedstoneLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtcsolo$liquidComparator(CallbackInfoReturnable<Integer> callback) {
        var support = GasGeneratorSupportGas.of(this);
        if (gtcsolo$liquidFuel != null && (support == null ? getGasTanks(null).get(0).isEmpty() : !support.hasGasFuel())) {
            var tank = gtcsolo$liquidFuel.tank();
            callback.setReturnValue(gtcsolo$liquidFuel.hasFuel()
                    ? MekanismUtils.redstoneLevelFromContents(tank.getFluidAmount(), tank.getCapacity()) : 0);
        }
    }

    @Inject(method = "onUpdateServer", at = @At(value = "INVOKE",
            target = "Lmekanism/common/inventory/slot/chemical/GasInventorySlot;fillTank()V", shift = At.Shift.AFTER),
            cancellable = true, remap = false)
    private void gtcsolo$burnLiquid(CallbackInfo callback) {
        if (gtcsolo$liquidFuel == null) return;
        var rotor = GasGeneratorRotor.of(this);
        if (rotor == null || !rotor.isReady()) {
            gasUsedLastTick = 0;
            setActive(false);
            callback.cancel();
            return;
        }
        // Keep native output, charging and gas-container refill before choosing a fuel.
        var support = GasGeneratorSupportGas.of(this);
        boolean gasFuel = support == null ? !getGasTanks(null).get(0).isEmpty() : support.hasGasFuel();
        if (gasFuel || !gtcsolo$liquidFuel.hasFuel()) {
            if (gtcsolo$lastLiquidTick) {
                generationRate = FloatingLong.ZERO;
                maxBurnTicks = 0;
                gasUsedLastTick = 0;
                gtcsolo$lastLiquidTick = false;
            }
            if (!gasFuel) {
                generationRate = FloatingLong.ZERO;
                maxBurnTicks = 0;
                gasUsedLastTick = 0;
                setActive(false);
                callback.cancel();
            }
            return;
        }
        var burned = MekanismUtils.canFunction(this) ? gtcsolo$liquidFuel.burn((BasicEnergyContainer) getEnergyContainers(null).get(0))
                : GasGeneratorLiquidFuel.BurnResult.IDLE;
        gasUsedLastTick = burned.usedFluid();
        setActive(burned.usedTicks() > 0);
        if (burned.usedTicks() > 0) {
            generationRate = burned.rate();
            maxBurnTicks = burned.burnTime();
            ((GeneratorOutputAccess) this).gtcsolo$updateMaxOutput(MekanismConfig.general.FROM_H2.get().max(burned.generated()));
        }
        gtcsolo$lastLiquidTick = true;
        callback.cancel();
    }
}
