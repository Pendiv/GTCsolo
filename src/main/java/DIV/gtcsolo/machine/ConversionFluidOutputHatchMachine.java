package DIV.gtcsolo.machine;

import DIV.gtcsolo.integration.mekanism.ChemicalBridge;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 化学変換液体搬出ハッチ。
 *
 * GTマルチブロックの液体搬出ハッチ (PartAbility.EXPORT_FLUIDS) として機能しつつ、
 * 内部タンクのGT液体を対応するMekanism Gas/Infusion に変換して隣接チューブへ搬出する。
 *
 * 動作: GT液体 → Chemical 方向固定。
 */
public class ConversionFluidOutputHatchMachine extends FluidHatchPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ConversionFluidOutputHatchMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);

    private static final int TRANSFER_RATE = 256;

    // LDLib は GasStack/InfusionStack のシリアライザを持たないため手動NBT
    private GasStack gasOutputBuffer = GasStack.EMPTY;
    private InfusionStack infusionOutputBuffer = InfusionStack.EMPTY;

    private TickableSubscription conversionSub;

    private LazyOptional<IGasHandler> gasOpt;
    private LazyOptional<IInfusionHandler> infusionOpt;

    public ConversionFluidOutputHatchMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.OUT, ConversionFluidHatchMachine.getTierCapacity(tier), 1);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // =========================================================================
    //  ライフサイクル
    // =========================================================================

    @Override
    public void onLoad() {
        super.onLoad();
        gasOpt = LazyOptional.of(() -> new InternalGasHandler());
        infusionOpt = LazyOptional.of(() -> new InternalInfusionHandler());
        if (conversionSub == null) {
            conversionSub = subscribeServerTick(this::onConversionTick);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (conversionSub != null) {
            unsubscribe(conversionSub);
            conversionSub = null;
        }
        if (gasOpt != null) gasOpt.invalidate();
        if (infusionOpt != null) infusionOpt.invalidate();
    }

    // =========================================================================
    //  変換ティック: tank 内の GT液体 → Chemical バッファ → 隣接チューブへ push
    // =========================================================================

    private void onConversionTick() {
        if (getLevel() == null || getLevel().isClientSide) return;

        convertFluidToChemical();

        if (!gasOutputBuffer.isEmpty()) pushGasToNeighbors();
        if (!infusionOutputBuffer.isEmpty()) pushInfusionToNeighbors();
    }

    private void convertFluidToChemical() {
        if (tank.isEmpty()) return;

        // tank の最初のスロットから液体を取得
        FluidStack fluidStack = tank.getFluidInTank(0);
        if (fluidStack.isEmpty()) return;

        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid());
        if (fluidId == null) return;

        String materialName = findMaterialNameFromFluid(fluidId);
        if (materialName == null) return;

        String chemKey = ChemicalBridge.getChemKey(materialName);
        if (chemKey == null) return;

        String[] parts = chemKey.split(":", 3);
        if (parts.length < 3) return;

        String chemType = parts[0];
        ResourceLocation chemId = new ResourceLocation(parts[1], parts[2]);

        if ("gas".equals(chemType)) {
            convertToGas(fluidStack, chemId);
        } else if ("infusion".equals(chemType)) {
            convertToInfusion(fluidStack, chemId);
        }
    }

    private void convertToGas(FluidStack fluidStack, ResourceLocation chemId) {
        long capacity = ConversionFluidHatchMachine.getTierCapacity(getTier());
        if (!gasOutputBuffer.isEmpty() && gasOutputBuffer.getAmount() >= capacity) return;
        Gas gas = MekanismAPI.gasRegistry().getValue(chemId);
        if (gas == null || gas.isEmptyType()) return;
        if (!gasOutputBuffer.isEmpty() && !gasOutputBuffer.getType().equals(gas)) return;

        long space = capacity - (gasOutputBuffer.isEmpty() ? 0 : gasOutputBuffer.getAmount());
        int toConvert = (int) Math.min(fluidStack.getAmount(), Math.min(space, TRANSFER_RATE));
        if (toConvert <= 0) return;

        tank.drain(toConvert, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (gasOutputBuffer.isEmpty()) {
            gasOutputBuffer = new GasStack(gas, toConvert);
        } else {
            gasOutputBuffer.grow(toConvert);
        }
    }

    private void convertToInfusion(FluidStack fluidStack, ResourceLocation chemId) {
        long capacity = ConversionFluidHatchMachine.getTierCapacity(getTier());
        if (!infusionOutputBuffer.isEmpty() && infusionOutputBuffer.getAmount() >= capacity) return;
        InfuseType infuseType = MekanismAPI.infuseTypeRegistry().getValue(chemId);
        if (infuseType == null || infuseType.isEmptyType()) return;
        if (!infusionOutputBuffer.isEmpty() && !infusionOutputBuffer.getType().equals(infuseType)) return;

        long space = capacity - (infusionOutputBuffer.isEmpty() ? 0 : infusionOutputBuffer.getAmount());
        int toConvert = (int) Math.min(fluidStack.getAmount(), Math.min(space, TRANSFER_RATE));
        if (toConvert <= 0) return;

        tank.drain(toConvert, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (infusionOutputBuffer.isEmpty()) {
            infusionOutputBuffer = new InfusionStack(infuseType, toConvert);
        } else {
            infusionOutputBuffer.grow(toConvert);
        }
    }

    // =========================================================================
    //  隣接チューブへ積極的に push
    // =========================================================================

    private void pushGasToNeighbors() {
        Direction front = getFrontFacing();
        BlockEntity neighbor = getLevel().getBlockEntity(getPos().relative(front));
        if (neighbor == null) return;
        neighbor.getCapability(Capabilities.GAS_HANDLER, front.getOpposite()).ifPresent(handler -> {
            long toSend = Math.min(gasOutputBuffer.getAmount(), TRANSFER_RATE);
            GasStack offered = new GasStack(gasOutputBuffer.getType(), toSend);
            GasStack remainder = handler.insertChemical(offered, Action.EXECUTE);
            long inserted = toSend - remainder.getAmount();
            if (inserted > 0) {
                gasOutputBuffer.shrink(inserted);
                if (gasOutputBuffer.getAmount() <= 0) gasOutputBuffer = GasStack.EMPTY;
            }
        });
    }

    private void pushInfusionToNeighbors() {
        Direction front = getFrontFacing();
        BlockEntity neighbor = getLevel().getBlockEntity(getPos().relative(front));
        if (neighbor == null) return;
        neighbor.getCapability(Capabilities.INFUSION_HANDLER, front.getOpposite()).ifPresent(handler -> {
            long toSend = Math.min(infusionOutputBuffer.getAmount(), TRANSFER_RATE);
            InfusionStack offered = new InfusionStack(infusionOutputBuffer.getType(), toSend);
            InfusionStack remainder = handler.insertChemical(offered, Action.EXECUTE);
            long inserted = toSend - remainder.getAmount();
            if (inserted > 0) {
                infusionOutputBuffer.shrink(inserted);
                if (infusionOutputBuffer.getAmount() <= 0) infusionOutputBuffer = InfusionStack.EMPTY;
            }
        });
    }

    // =========================================================================
    //  変換解決
    // =========================================================================

    @Nullable
    private String findMaterialNameFromFluid(ResourceLocation fluidId) {
        if (!"gtcsolo".equals(fluidId.getNamespace())) return null;
        String path = fluidId.getPath();
        for (String matName : ChemicalBridge.getAllMappings().values()) {
            if (path.equals(matName) || path.equals("liquid_" + matName)) {
                return matName;
            }
        }
        return null;
    }

    // =========================================================================
    //  Capability（AttachCapabilitiesEvent 経由で公開）
    // =========================================================================

    public LazyOptional<IGasHandler> getGasCapability() {
        return gasOpt != null ? gasOpt : LazyOptional.empty();
    }

    public LazyOptional<IInfusionHandler> getInfusionCapability() {
        return infusionOpt != null ? infusionOpt : LazyOptional.empty();
    }

    // =========================================================================
    //  Gas ハンドラ（搬出専用）
    // =========================================================================

    private class InternalGasHandler implements IGasHandler {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull GasStack getChemicalInTank(int tank) { return gasOutputBuffer; }
        @Override public void setChemicalInTank(int tank, @NotNull GasStack stack) { gasOutputBuffer = stack; }
        @Override public long getTankCapacity(int tank) { return ConversionFluidHatchMachine.getTierCapacity(getTier()); }
        @Override public @NotNull GasStack getEmptyStack() { return GasStack.EMPTY; }
        @Override public boolean isValid(int tank, @NotNull GasStack stack) { return false; }

        @Override
        public @NotNull GasStack insertChemical(int tank, @NotNull GasStack stack, @NotNull Action action) {
            return stack; // 搬出専用、受け入れない
        }

        @Override
        public @NotNull GasStack extractChemical(int tank, long amount, @NotNull Action action) {
            if (gasOutputBuffer.isEmpty() || amount <= 0) return GasStack.EMPTY;
            long toExtract = Math.min(amount, gasOutputBuffer.getAmount());
            GasStack result = new GasStack(gasOutputBuffer.getType(), toExtract);
            if (action == Action.EXECUTE) {
                gasOutputBuffer.shrink(toExtract);
                if (gasOutputBuffer.getAmount() <= 0) gasOutputBuffer = GasStack.EMPTY;
            }
            return result;
        }
    }

    // =========================================================================
    //  Infusion ハンドラ（搬出専用）
    // =========================================================================

    private class InternalInfusionHandler implements IInfusionHandler {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull InfusionStack getChemicalInTank(int tank) { return infusionOutputBuffer; }
        @Override public void setChemicalInTank(int tank, @NotNull InfusionStack stack) { infusionOutputBuffer = stack; }
        @Override public long getTankCapacity(int tank) { return ConversionFluidHatchMachine.getTierCapacity(getTier()); }
        @Override public @NotNull InfusionStack getEmptyStack() { return InfusionStack.EMPTY; }
        @Override public boolean isValid(int tank, @NotNull InfusionStack stack) { return false; }

        @Override
        public @NotNull InfusionStack insertChemical(int tank, @NotNull InfusionStack stack, @NotNull Action action) {
            return stack; // 搬出専用
        }

        @Override
        public @NotNull InfusionStack extractChemical(int tank, long amount, @NotNull Action action) {
            if (infusionOutputBuffer.isEmpty() || amount <= 0) return InfusionStack.EMPTY;
            long toExtract = Math.min(amount, infusionOutputBuffer.getAmount());
            InfusionStack result = new InfusionStack(infusionOutputBuffer.getType(), toExtract);
            if (action == Action.EXECUTE) {
                infusionOutputBuffer.shrink(toExtract);
                if (infusionOutputBuffer.getAmount() <= 0) infusionOutputBuffer = InfusionStack.EMPTY;
            }
            return result;
        }
    }

    public static ConversionFluidOutputHatchMachine create(IMachineBlockEntity holder, int tier) {
        return new ConversionFluidOutputHatchMachine(holder, tier);
    }
}