package DIV.gtcsolo.machine;

import DIV.gtcsolo.integration.mekanism.ChemicalBridge;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 化学変換液体搬入ハッチ。
 *
 * GTマルチブロックの液体搬入ハッチ (PartAbility.IMPORT_FLUIDS) として機能しつつ、
 * Mekanism の Gas/Infusion を受け入れて対応するGT液体に変換し、内部タンクに格納する。
 *
 * 動作: Liquid→Chemical 方向のみ（Chemical受入 → GT液体として内部保持）。
 * Mekanism 加圧チューブ/注入管が接続可能。
 */
public class ConversionFluidHatchMachine extends FluidHatchPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ConversionFluidHatchMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);

    private static final int TRANSFER_RATE = 256;

    // 気体バッファ（チューブから受け入れ、次tickで液体に変換）
    // LDLib は GasStack/InfusionStack のシリアライザを持たないため手動NBT
    private GasStack gasBuffer = GasStack.EMPTY;
    private InfusionStack infusionBuffer = InfusionStack.EMPTY;

    private TickableSubscription conversionSub;

    // Mek Capability
    private LazyOptional<IGasHandler> gasOpt;
    private LazyOptional<IInfusionHandler> infusionOpt;

    public ConversionFluidHatchMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.IN, getTierCapacity(tier), 1);
    }

    /**
     * Tier ごとの容量 (mB)。
     * EV(4)=128000, IV(5)=256000, ... 基本 8000 * 2^tier
     */
    public static int getTierCapacity(int tier) {
        return 8000 * (1 << Math.min(9, tier));
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
    //  変換ティック: バッファ内の Chemical → GT液体に変換して tank に fill
    // =========================================================================

    private void onConversionTick() {
        if (getLevel() == null || getLevel().isClientSide) return;

        boolean changed = false;

        // Gas → Fluid
        if (!gasBuffer.isEmpty()) {
            ResourceLocation gasId = MekanismAPI.gasRegistry().getKey(gasBuffer.getType());
            net.minecraftforge.fluids.FluidStack converted = resolveChemicalToFluid("gas", gasId);
            if (!converted.isEmpty()) {
                int amount = (int) Math.min(gasBuffer.getAmount(), TRANSFER_RATE);
                converted = new net.minecraftforge.fluids.FluidStack(converted.getFluid(), amount);
                int filled = tank.fill(converted, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    gasBuffer.shrink(filled);
                    if (gasBuffer.getAmount() <= 0) gasBuffer = GasStack.EMPTY;
                    changed = true;
                }
            }
        }

        // Infusion → Fluid
        if (!infusionBuffer.isEmpty()) {
            ResourceLocation infId = MekanismAPI.infuseTypeRegistry().getKey(infusionBuffer.getType());
            net.minecraftforge.fluids.FluidStack converted = resolveChemicalToFluid("infusion", infId);
            if (!converted.isEmpty()) {
                int amount = (int) Math.min(infusionBuffer.getAmount(), TRANSFER_RATE);
                converted = new net.minecraftforge.fluids.FluidStack(converted.getFluid(), amount);
                int filled = tank.fill(converted, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    infusionBuffer.shrink(filled);
                    if (infusionBuffer.getAmount() <= 0) infusionBuffer = InfusionStack.EMPTY;
                    changed = true;
                }
            }
        }

        // Gas 搬出を積極的に行う（隣接チューブから吸い込み）
        pullChemicalsFromNeighbors();
    }

    // =========================================================================
    //  隣接チューブから積極的にChemicalを吸い込む
    // =========================================================================

    private void pullChemicalsFromNeighbors() {
        if (getLevel() == null) return;
        Direction front = getFrontFacing();

        BlockEntity neighbor = getLevel().getBlockEntity(getPos().relative(front));
        if (neighbor == null) return;

        // Gas
        long gasSpace = getTierCapacity(getTier()) - (gasBuffer.isEmpty() ? 0 : gasBuffer.getAmount());
        if (gasSpace > 0) {
            neighbor.getCapability(Capabilities.GAS_HANDLER, front.getOpposite()).ifPresent(handler -> {
                long toExtract = Math.min(gasSpace, TRANSFER_RATE);
                GasStack extracted = handler.extractChemical(toExtract, Action.SIMULATE);
                if (!extracted.isEmpty() && isConvertibleChemical("gas", MekanismAPI.gasRegistry().getKey(extracted.getType()))) {
                    // バッファと種類が一致するか空
                    if (gasBuffer.isEmpty() || gasBuffer.isTypeEqual(extracted)) {
                        GasStack actual = handler.extractChemical(toExtract, Action.EXECUTE);
                        if (!actual.isEmpty()) {
                            if (gasBuffer.isEmpty()) gasBuffer = actual.copy();
                            else gasBuffer.grow(actual.getAmount());
                        }
                    }
                }
            });
        }

        // Infusion
        long infSpace = getTierCapacity(getTier()) - (infusionBuffer.isEmpty() ? 0 : infusionBuffer.getAmount());
        if (infSpace > 0) {
            neighbor.getCapability(Capabilities.INFUSION_HANDLER, front.getOpposite()).ifPresent(handler -> {
                long toExtract = Math.min(infSpace, TRANSFER_RATE);
                InfusionStack extracted = handler.extractChemical(toExtract, Action.SIMULATE);
                if (!extracted.isEmpty() && isConvertibleChemical("infusion", MekanismAPI.infuseTypeRegistry().getKey(extracted.getType()))) {
                    if (infusionBuffer.isEmpty() || infusionBuffer.isTypeEqual(extracted)) {
                        InfusionStack actual = handler.extractChemical(toExtract, Action.EXECUTE);
                        if (!actual.isEmpty()) {
                            if (infusionBuffer.isEmpty()) infusionBuffer = actual.copy();
                            else infusionBuffer.grow(actual.getAmount());
                        }
                    }
                }
            });
        }
    }

    // =========================================================================
    //  変換解決
    // =========================================================================

    private net.minecraftforge.fluids.FluidStack resolveChemicalToFluid(String type, @Nullable ResourceLocation chemicalId) {
        if (chemicalId == null) return net.minecraftforge.fluids.FluidStack.EMPTY;
        String chemKey = type + ":" + chemicalId.getNamespace() + ":" + chemicalId.getPath();
        String materialName = ChemicalBridge.getMaterialName(chemKey);
        if (materialName == null) return net.minecraftforge.fluids.FluidStack.EMPTY;

        Material mat = GTCEuAPI.materialManager.getMaterial("gtcsolo:" + materialName);
        if (mat == null || !mat.hasFluid()) return net.minecraftforge.fluids.FluidStack.EMPTY;

        net.minecraft.world.level.material.Fluid fluid = mat.getFluid();
        if (fluid == null) return net.minecraftforge.fluids.FluidStack.EMPTY;
        return new net.minecraftforge.fluids.FluidStack(fluid, 1);
    }

    private boolean isConvertibleChemical(String type, @Nullable ResourceLocation chemicalId) {
        if (chemicalId == null) return false;
        return ChemicalBridge.getMaterialName(type + ":" + chemicalId.getNamespace() + ":" + chemicalId.getPath()) != null;
    }

    // =========================================================================
    //  Capability — 外部から取得用（AttachCapabilitiesEvent 経由で公開）
    // =========================================================================

    public LazyOptional<IGasHandler> getGasCapability() {
        return gasOpt != null ? gasOpt : LazyOptional.empty();
    }

    public LazyOptional<IInfusionHandler> getInfusionCapability() {
        return infusionOpt != null ? infusionOpt : LazyOptional.empty();
    }

    // =========================================================================
    //  Gas ハンドラ（チューブ接続受入用）
    // =========================================================================

    private class InternalGasHandler implements IGasHandler {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull GasStack getChemicalInTank(int tank) { return gasBuffer; }
        @Override public void setChemicalInTank(int tank, @NotNull GasStack stack) { gasBuffer = stack; }
        @Override public long getTankCapacity(int tank) { return ConversionFluidHatchMachine.getTierCapacity(getTier()); }
        @Override public @NotNull GasStack getEmptyStack() { return GasStack.EMPTY; }

        @Override
        public boolean isValid(int tank, @NotNull GasStack stack) {
            ResourceLocation id = MekanismAPI.gasRegistry().getKey(stack.getType());
            return isConvertibleChemical("gas", id);
        }

        @Override
        public @NotNull GasStack insertChemical(int tank, @NotNull GasStack stack, @NotNull Action action) {
            if (stack.isEmpty() || !isValid(tank, stack)) return stack;
            if (!gasBuffer.isEmpty() && !gasBuffer.isTypeEqual(stack)) return stack;
            long capacity = ConversionFluidHatchMachine.getTierCapacity(getTier());
            long space = capacity - (gasBuffer.isEmpty() ? 0 : gasBuffer.getAmount());
            long toInsert = Math.min(stack.getAmount(), space);
            if (toInsert <= 0) return stack;
            if (action == Action.EXECUTE) {
                if (gasBuffer.isEmpty()) gasBuffer = new GasStack(stack.getType(), toInsert);
                else gasBuffer.grow(toInsert);
            }
            return toInsert == stack.getAmount() ? GasStack.EMPTY : new GasStack(stack.getType(), stack.getAmount() - toInsert);
        }

        @Override
        public @NotNull GasStack extractChemical(int tank, long amount, @NotNull Action action) {
            return GasStack.EMPTY; // 搬入専用
        }
    }

    // =========================================================================
    //  Infusion ハンドラ
    // =========================================================================

    private class InternalInfusionHandler implements IInfusionHandler {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull InfusionStack getChemicalInTank(int tank) { return infusionBuffer; }
        @Override public void setChemicalInTank(int tank, @NotNull InfusionStack stack) { infusionBuffer = stack; }
        @Override public long getTankCapacity(int tank) { return ConversionFluidHatchMachine.getTierCapacity(getTier()); }
        @Override public @NotNull InfusionStack getEmptyStack() { return InfusionStack.EMPTY; }

        @Override
        public boolean isValid(int tank, @NotNull InfusionStack stack) {
            ResourceLocation id = MekanismAPI.infuseTypeRegistry().getKey(stack.getType());
            return isConvertibleChemical("infusion", id);
        }

        @Override
        public @NotNull InfusionStack insertChemical(int tank, @NotNull InfusionStack stack, @NotNull Action action) {
            if (stack.isEmpty() || !isValid(tank, stack)) return stack;
            if (!infusionBuffer.isEmpty() && !infusionBuffer.isTypeEqual(stack)) return stack;
            long capacity = ConversionFluidHatchMachine.getTierCapacity(getTier());
            long space = capacity - (infusionBuffer.isEmpty() ? 0 : infusionBuffer.getAmount());
            long toInsert = Math.min(stack.getAmount(), space);
            if (toInsert <= 0) return stack;
            if (action == Action.EXECUTE) {
                if (infusionBuffer.isEmpty()) infusionBuffer = new InfusionStack(stack.getType(), toInsert);
                else infusionBuffer.grow(toInsert);
            }
            return toInsert == stack.getAmount() ? InfusionStack.EMPTY : new InfusionStack(stack.getType(), stack.getAmount() - toInsert);
        }

        @Override
        public @NotNull InfusionStack extractChemical(int tank, long amount, @NotNull Action action) {
            return InfusionStack.EMPTY; // 搬入専用
        }
    }

    public static ConversionFluidHatchMachine create(IMachineBlockEntity holder, int tier) {
        return new ConversionFluidHatchMachine(holder, tier);
    }
}