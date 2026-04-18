package DIV.gtcsolo.block;

import DIV.gtcsolo.integration.mekanism.ChemicalBridge;
import DIV.gtcsolo.registry.ModBlockEntities;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 化学変換システムブロック。
 *
 * Chemical→Liquid モード: 隣接チューブからGas/Infusionを吸い込み、対応GT液体を生成して搬出。
 * Liquid→Chemical モード: 隣接パイプからGT液体を吸い込み、対応Mek Chemical を生成して搬出。
 *
 * 変換比: 1:1 (1 mB = 1 mB)
 */
public class ConversionSystemBlockEntity extends BlockEntity {

    private static final int TANK_CAPACITY = 16000;
    private static final int TRANSFER_RATE = 256;

    // モード: true = Chemical→Liquid, false = Liquid→Chemical
    private boolean chemToLiquid = true;

    // 液体タンク（両モード共通）
    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY);

    // Gas バッファ
    private GasStack gasBuffer = GasStack.EMPTY;
    private GasStack gasOutputBuffer = GasStack.EMPTY;

    // Infusion バッファ
    private InfusionStack infusionBuffer = InfusionStack.EMPTY;
    private InfusionStack infusionOutputBuffer = InfusionStack.EMPTY;

    // Capability
    private final LazyOptional<IFluidHandler> fluidOpt = LazyOptional.of(() -> fluidTank);
    private final LazyOptional<IGasHandler> gasOpt = LazyOptional.of(() -> new InternalGasHandler());
    private final LazyOptional<IInfusionHandler> infusionOpt = LazyOptional.of(() -> new InternalInfusionHandler());

    public ConversionSystemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVERSION_SYSTEM.get(), pos, state);
    }

    public boolean isChemToLiquid() {
        return chemToLiquid;
    }

    // 旧API互換
    public boolean isGasToLiquid() {
        return chemToLiquid;
    }

    public void toggleMode() {
        chemToLiquid = !chemToLiquid;
        fluidTank.drain(TANK_CAPACITY, IFluidHandler.FluidAction.EXECUTE);
        gasBuffer = GasStack.EMPTY;
        gasOutputBuffer = GasStack.EMPTY;
        infusionBuffer = InfusionStack.EMPTY;
        infusionOutputBuffer = InfusionStack.EMPTY;
        setChanged();
    }

    // =========================================================================
    //  サーバーティック
    // =========================================================================

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        if (chemToLiquid) {
            convertGasInputToFluid();
            convertInfusionInputToFluid();
            if (fluidTank.getFluidAmount() > 0) pushFluidToNeighbors();
        } else {
            convertFluidToChemicalOutput();
            if (!gasOutputBuffer.isEmpty()) pushGasToNeighbors();
            if (!infusionOutputBuffer.isEmpty()) pushInfusionToNeighbors();
        }
    }

    // =========================================================================
    //  Chemical→Liquid 変換
    // =========================================================================

    private void convertGasInputToFluid() {
        if (gasBuffer.isEmpty()) return;
        FluidStack converted = resolveChemicalToFluid("gas", MekanismAPI.gasRegistry().getKey(gasBuffer.getType()));
        if (converted.isEmpty()) return;
        converted = new FluidStack(converted.getFluid(), (int) Math.min(gasBuffer.getAmount(), TRANSFER_RATE));
        int filled = fluidTank.fill(converted, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            gasBuffer.shrink(filled);
            if (gasBuffer.getAmount() <= 0) gasBuffer = GasStack.EMPTY;
            setChanged();
        }
    }

    private void convertInfusionInputToFluid() {
        if (infusionBuffer.isEmpty()) return;
        FluidStack converted = resolveChemicalToFluid("infusion", MekanismAPI.infuseTypeRegistry().getKey(infusionBuffer.getType()));
        if (converted.isEmpty()) return;
        converted = new FluidStack(converted.getFluid(), (int) Math.min(infusionBuffer.getAmount(), TRANSFER_RATE));
        int filled = fluidTank.fill(converted, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            infusionBuffer.shrink(filled);
            if (infusionBuffer.getAmount() <= 0) infusionBuffer = InfusionStack.EMPTY;
            setChanged();
        }
    }

    // =========================================================================
    //  Liquid→Chemical 変換
    // =========================================================================

    private void convertFluidToChemicalOutput() {
        if (fluidTank.getFluidAmount() <= 0) return;

        ResourceLocation fluidId = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluidTank.getFluid().getFluid());
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
            convertFluidToGas(chemId);
        } else if ("infusion".equals(chemType)) {
            convertFluidToInfusion(chemId);
        }
    }

    private void convertFluidToGas(ResourceLocation chemId) {
        if (!gasOutputBuffer.isEmpty() && gasOutputBuffer.getAmount() >= TANK_CAPACITY) return;
        Gas gas = MekanismAPI.gasRegistry().getValue(chemId);
        if (gas == null || gas.isEmptyType()) return;

        long space = TANK_CAPACITY - (gasOutputBuffer.isEmpty() ? 0 : gasOutputBuffer.getAmount());
        int toConvert = (int) Math.min(fluidTank.getFluidAmount(), Math.min(space, TRANSFER_RATE));
        if (toConvert <= 0) return;

        // 種類チェック
        if (!gasOutputBuffer.isEmpty() && !gasOutputBuffer.getType().equals(gas)) return;

        fluidTank.drain(toConvert, IFluidHandler.FluidAction.EXECUTE);
        if (gasOutputBuffer.isEmpty()) {
            gasOutputBuffer = new GasStack(gas, toConvert);
        } else {
            gasOutputBuffer.grow(toConvert);
        }
        setChanged();
    }

    private void convertFluidToInfusion(ResourceLocation chemId) {
        if (!infusionOutputBuffer.isEmpty() && infusionOutputBuffer.getAmount() >= TANK_CAPACITY) return;
        InfuseType infuseType = MekanismAPI.infuseTypeRegistry().getValue(chemId);
        if (infuseType == null || infuseType.isEmptyType()) return;

        long space = TANK_CAPACITY - (infusionOutputBuffer.isEmpty() ? 0 : infusionOutputBuffer.getAmount());
        int toConvert = (int) Math.min(fluidTank.getFluidAmount(), Math.min(space, TRANSFER_RATE));
        if (toConvert <= 0) return;

        if (!infusionOutputBuffer.isEmpty() && !infusionOutputBuffer.getType().equals(infuseType)) return;

        fluidTank.drain(toConvert, IFluidHandler.FluidAction.EXECUTE);
        if (infusionOutputBuffer.isEmpty()) {
            infusionOutputBuffer = new InfusionStack(infuseType, toConvert);
        } else {
            infusionOutputBuffer.grow(toConvert);
        }
        setChanged();
    }

    // =========================================================================
    //  変換解決（共通）
    // =========================================================================

    private FluidStack resolveChemicalToFluid(String type, @Nullable ResourceLocation chemicalId) {
        if (chemicalId == null) return FluidStack.EMPTY;

        String chemKey = type + ":" + chemicalId.getNamespace() + ":" + chemicalId.getPath();
        String materialName = ChemicalBridge.getMaterialName(chemKey);
        if (materialName == null) return FluidStack.EMPTY;

        Material mat = GTCEuAPI.materialManager.getMaterial("gtcsolo:" + materialName);
        if (mat == null || !mat.hasFluid()) return FluidStack.EMPTY;

        net.minecraft.world.level.material.Fluid fluid = mat.getFluid();
        if (fluid == null) return FluidStack.EMPTY;

        return new FluidStack(fluid, 1);
    }

    private boolean isConvertibleChemical(String type, @Nullable ResourceLocation chemicalId) {
        if (chemicalId == null) return false;
        String chemKey = type + ":" + chemicalId.getNamespace() + ":" + chemicalId.getPath();
        return ChemicalBridge.getMaterialName(chemKey) != null;
    }

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
    //  搬出ロジック
    // =========================================================================

    private void pushFluidToNeighbors() {
        for (Direction dir : Direction.values()) {
            if (fluidTank.getFluidAmount() <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(handler -> {
                FluidStack toDrain = fluidTank.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE);
                if (!toDrain.isEmpty()) {
                    int accepted = handler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                    if (accepted > 0) {
                        fluidTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            });
        }
    }

    private void pushGasToNeighbors() {
        for (Direction dir : Direction.values()) {
            if (gasOutputBuffer.isEmpty()) break;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(Capabilities.GAS_HANDLER, dir.getOpposite()).ifPresent(handler -> {
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
    }

    private void pushInfusionToNeighbors() {
        for (Direction dir : Direction.values()) {
            if (infusionOutputBuffer.isEmpty()) break;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(Capabilities.INFUSION_HANDLER, dir.getOpposite()).ifPresent(handler -> {
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
    }

    // =========================================================================
    //  Capability
    // =========================================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidOpt.cast();
        if (cap == Capabilities.GAS_HANDLER) return gasOpt.cast();
        if (cap == Capabilities.INFUSION_HANDLER) return infusionOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidOpt.invalidate();
        gasOpt.invalidate();
        infusionOpt.invalidate();
    }

    // =========================================================================
    //  NBT
    // =========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("chemToLiquid", chemToLiquid);
        tag.put("fluidTank", fluidTank.writeToNBT(new CompoundTag()));
        if (!gasBuffer.isEmpty()) tag.put("gasBuffer", gasBuffer.write(new CompoundTag()));
        if (!gasOutputBuffer.isEmpty()) tag.put("gasOutputBuffer", gasOutputBuffer.write(new CompoundTag()));
        if (!infusionBuffer.isEmpty()) tag.put("infusionBuffer", infusionBuffer.write(new CompoundTag()));
        if (!infusionOutputBuffer.isEmpty()) tag.put("infusionOutputBuffer", infusionOutputBuffer.write(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 旧NBT互換
        chemToLiquid = tag.contains("chemToLiquid") ? tag.getBoolean("chemToLiquid") : tag.getBoolean("gasToLiquid");
        fluidTank.readFromNBT(tag.getCompound("fluidTank"));
        gasBuffer = tag.contains("gasBuffer") ? GasStack.readFromNBT(tag.getCompound("gasBuffer")) : GasStack.EMPTY;
        gasOutputBuffer = tag.contains("gasOutputBuffer") ? GasStack.readFromNBT(tag.getCompound("gasOutputBuffer")) : GasStack.EMPTY;
        infusionBuffer = tag.contains("infusionBuffer") ? InfusionStack.readFromNBT(tag.getCompound("infusionBuffer")) : InfusionStack.EMPTY;
        infusionOutputBuffer = tag.contains("infusionOutputBuffer") ? InfusionStack.readFromNBT(tag.getCompound("infusionOutputBuffer")) : InfusionStack.EMPTY;
    }

    // =========================================================================
    //  内部 Gas ハンドラ
    // =========================================================================

    private class InternalGasHandler implements IGasHandler {
        @Override public int getTanks() { return 1; }

        @Override
        public @NotNull GasStack getChemicalInTank(int tank) {
            return chemToLiquid ? gasBuffer : gasOutputBuffer;
        }

        @Override
        public void setChemicalInTank(int tank, @NotNull GasStack stack) {
            if (chemToLiquid) gasBuffer = stack; else gasOutputBuffer = stack;
            setChanged();
        }

        @Override public long getTankCapacity(int tank) { return TANK_CAPACITY; }

        @Override
        public boolean isValid(int tank, @NotNull GasStack stack) {
            if (!chemToLiquid) return false;
            ResourceLocation id = MekanismAPI.gasRegistry().getKey(stack.getType());
            return isConvertibleChemical("gas", id);
        }

        @Override
        public @NotNull GasStack insertChemical(int tank, @NotNull GasStack stack, @NotNull Action action) {
            if (!chemToLiquid || stack.isEmpty() || !isValid(tank, stack)) return stack;
            if (!gasBuffer.isEmpty() && !gasBuffer.isTypeEqual(stack)) return stack;

            long space = TANK_CAPACITY - (gasBuffer.isEmpty() ? 0 : gasBuffer.getAmount());
            long toInsert = Math.min(stack.getAmount(), space);
            if (toInsert <= 0) return stack;

            if (action == Action.EXECUTE) {
                if (gasBuffer.isEmpty()) gasBuffer = new GasStack(stack.getType(), toInsert);
                else gasBuffer.grow(toInsert);
                setChanged();
            }
            return toInsert == stack.getAmount() ? GasStack.EMPTY : new GasStack(stack.getType(), stack.getAmount() - toInsert);
        }

        @Override
        public @NotNull GasStack extractChemical(int tank, long amount, @NotNull Action action) {
            if (chemToLiquid || gasOutputBuffer.isEmpty() || amount <= 0) return GasStack.EMPTY;
            long toExtract = Math.min(amount, gasOutputBuffer.getAmount());
            GasStack result = new GasStack(gasOutputBuffer.getType(), toExtract);
            if (action == Action.EXECUTE) {
                gasOutputBuffer.shrink(toExtract);
                if (gasOutputBuffer.getAmount() <= 0) gasOutputBuffer = GasStack.EMPTY;
                setChanged();
            }
            return result;
        }

        @Override public @NotNull GasStack getEmptyStack() { return GasStack.EMPTY; }
    }

    // =========================================================================
    //  内部 Infusion ハンドラ
    // =========================================================================

    private class InternalInfusionHandler implements IInfusionHandler {
        @Override public int getTanks() { return 1; }

        @Override
        public @NotNull InfusionStack getChemicalInTank(int tank) {
            return chemToLiquid ? infusionBuffer : infusionOutputBuffer;
        }

        @Override
        public void setChemicalInTank(int tank, @NotNull InfusionStack stack) {
            if (chemToLiquid) infusionBuffer = stack; else infusionOutputBuffer = stack;
            setChanged();
        }

        @Override public long getTankCapacity(int tank) { return TANK_CAPACITY; }

        @Override
        public boolean isValid(int tank, @NotNull InfusionStack stack) {
            if (!chemToLiquid) return false;
            ResourceLocation id = MekanismAPI.infuseTypeRegistry().getKey(stack.getType());
            return isConvertibleChemical("infusion", id);
        }

        @Override
        public @NotNull InfusionStack insertChemical(int tank, @NotNull InfusionStack stack, @NotNull Action action) {
            if (!chemToLiquid || stack.isEmpty() || !isValid(tank, stack)) return stack;
            if (!infusionBuffer.isEmpty() && !infusionBuffer.isTypeEqual(stack)) return stack;

            long space = TANK_CAPACITY - (infusionBuffer.isEmpty() ? 0 : infusionBuffer.getAmount());
            long toInsert = Math.min(stack.getAmount(), space);
            if (toInsert <= 0) return stack;

            if (action == Action.EXECUTE) {
                if (infusionBuffer.isEmpty()) infusionBuffer = new InfusionStack(stack.getType(), toInsert);
                else infusionBuffer.grow(toInsert);
                setChanged();
            }
            return toInsert == stack.getAmount() ? InfusionStack.EMPTY : new InfusionStack(stack.getType(), stack.getAmount() - toInsert);
        }

        @Override
        public @NotNull InfusionStack extractChemical(int tank, long amount, @NotNull Action action) {
            if (chemToLiquid || infusionOutputBuffer.isEmpty() || amount <= 0) return InfusionStack.EMPTY;
            long toExtract = Math.min(amount, infusionOutputBuffer.getAmount());
            InfusionStack result = new InfusionStack(infusionOutputBuffer.getType(), toExtract);
            if (action == Action.EXECUTE) {
                infusionOutputBuffer.shrink(toExtract);
                if (infusionOutputBuffer.getAmount() <= 0) infusionOutputBuffer = InfusionStack.EMPTY;
                setChanged();
            }
            return result;
        }

        @Override public @NotNull InfusionStack getEmptyStack() { return InfusionStack.EMPTY; }
    }
}