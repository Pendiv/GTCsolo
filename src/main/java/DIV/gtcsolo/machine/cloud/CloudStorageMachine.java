package DIV.gtcsolo.machine.cloud;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudStorageMachine extends MultiblockControllerMachine implements IMachineLife {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(CloudStorageMachine.class,
            MultiblockControllerMachine.MANAGED_FIELD_HOLDER);
    public static final String CAPACITY_CELLS = "gtcsolo.cloud.capacity_cells";
    private static final int ITEM_SLOTS_PER_CELL = 27;
    private static final int FLUID_MB_PER_CELL = 256_000;
    private static final int TRANSFER_INTERVAL = 5;
    private static final int ITEM_EXPORT_ATTEMPTS = 32;

    private final CloudStorageData.Kind kind;
    private final List<ItemBusPartMachine> itemInputs = new ArrayList<>();
    private final List<ItemBusPartMachine> itemOutputs = new ArrayList<>();
    private final List<FluidHatchPartMachine> fluidInputs = new ArrayList<>();
    private final List<FluidHatchPartMachine> fluidOutputs = new ArrayList<>();
    @Persisted @DescSynced private int color = CloudStorageData.UNCOLORED;
    @Persisted private boolean capacityRegistered;
    @Persisted private long[] capacityChunks = new long[0];
    @DescSynced private long localCapacity;
    @DescSynced private boolean canImport;
    @DescSynced private boolean canExport;
    private int nextExportSlot;
    private @Nullable TickableSubscription transferSubscription;
    private @Nullable TickableSubscription reconciliationSubscription;

    public CloudStorageMachine(IMachineBlockEntity holder, CloudStorageData.Kind kind) {
        super(holder);
        this.kind = kind;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return FIELDS;
    }

    /** Records the matched storage cell for the final predicate check. */
    public static TraceabilityPredicate capacityCell(Block block) {
        return Predicates.custom(state -> {
            if (!state.getBlockState().is(block)) return false;
            Map<BlockPos, TraceabilityPredicate> cells = state.getMatchContext().getOrCreate(CAPACITY_CELLS,
                    HashMap::new);
            cells.put(state.getPos(), state.predicate);
            return true;
        }, () -> new BlockInfo[]{BlockInfo.fromBlockState(block.defaultBlockState())});
    }

    public CloudStorageData.Kind getKind() { return kind; }

    public int getColor() { return color; }

    public boolean canImport() { return canImport; }

    public boolean canExport() { return canExport; }

    public long getLocalCapacity() { return localCapacity; }

    public boolean isOperational() {
        return isFormed() && (canImport || canExport) && !isInValid() && !getMultiblockState().hasError();
    }

    @Override
    public boolean checkPattern() {
        var pattern = getPattern();
        return pattern != null && pattern.checkPatternAt(getMultiblockState(), true);
    }

    public @Nullable CloudStorageData getCloudData() {
        return getLevel() instanceof ServerLevel level ? CloudStorageData.get(level) : null;
    }

    public boolean setColor(int newColor) {
        if (!(getLevel() instanceof ServerLevel level) || !isOperational() ||
                (newColor != CloudStorageData.UNCOLORED && !CloudStorageData.validColor(newColor))) return false;
        if (color == newColor) return true;
        if (color != CloudStorageData.UNCOLORED && hasBufferedContents()) return false;
        color = newColor;
        nextExportSlot = 0;
        updateCapacity(level);
        updateTransferSubscription();
        markDirty();
        return true;
    }

    @Override
    public void onLoad() {
        if (getLevel() instanceof ServerLevel) isFormed = false;
        super.onLoad();
        reconcileCapacityOnLoad();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (!(getLevel() instanceof ServerLevel level)) return;
        stopReconciliation();
        clearPorts();
        for (var part : getParts()) {
            if (kind == CloudStorageData.Kind.ITEMS && part.self() instanceof ItemBusPartMachine bus) {
                if (bus.getInventory().handlerIO == IO.IN) itemInputs.add(bus);
                if (bus.getInventory().handlerIO == IO.OUT) itemOutputs.add(bus);
            } else if (kind == CloudStorageData.Kind.FLUIDS && part.self() instanceof FluidHatchPartMachine hatch) {
                if (hatch.tank.handlerIO == IO.IN) fluidInputs.add(hatch);
                if (hatch.tank.handlerIO == IO.OUT) fluidOutputs.add(hatch);
            }
        }
        canImport = !itemInputs.isEmpty() || !fluidInputs.isEmpty();
        canExport = !itemOutputs.isEmpty() || !fluidOutputs.isEmpty();
        localCapacity = canImport ? matchedCapacityCells() * capacityPerCell() : 0;
        if (kind == CloudStorageData.Kind.ITEMS) {
            localCapacity = Math.min(localCapacity, CloudStorageData.MAX_ITEM_SLOTS);
        }
        if (localCapacity > 0) {
            long[] nextChunks = getMultiblockState().getCache().stream().mapToLong(ChunkPos::asLong)
                    .distinct().sorted().toArray();
            if (!Arrays.equals(capacityChunks, nextChunks)) {
                capacityChunks = nextChunks;
                markDirty();
            }
        }
        updateCapacity(level);
        updateTransferSubscription();
    }

    @Override
    public void onStructureInvalid() {
        boolean unloaded = getMultiblockState().error == MultiblockState.UNLOAD_ERROR ||
                (getLevel() instanceof ServerLevel level && !capacityChunksLoaded(level));
        super.onStructureInvalid();
        clearPorts();
        stopTransfer();
        if (unloaded) reconcileCapacityOnLoad();
        else clearCapacity();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        clearPorts();
        stopTransfer();
        reconcileCapacityOnLoad();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        clearPorts();
        stopTransfer();
        stopReconciliation();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        if (oldFacing != newFacing) clearCapacity();
        super.onRotated(oldFacing, newFacing);
    }

    @Override
    public void onMachineRemoved() {
        clearCapacity();
        stopTransfer();
    }

    private long capacityPerCell() {
        return kind == CloudStorageData.Kind.ITEMS ? ITEM_SLOTS_PER_CELL : FLUID_MB_PER_CELL;
    }

    private long matchedCapacityCells() {
        var context = getMultiblockState().getMatchContext();
        Map<BlockPos, TraceabilityPredicate> cells = context.getOrDefault(CAPACITY_CELLS, Map.of());
        Map<BlockPos, TraceabilityPredicate> accepted = context.getOrDefault("predicates", Map.of());
        return cells.entrySet().stream().filter(entry -> accepted.get(entry.getKey()) == entry.getValue()).count();
    }

    private void updateCapacity(ServerLevel level) {
        if (localCapacity > 0 && CloudStorageData.validColor(color)) {
            CloudStorageData.get(level).setCapacity(kind, color, level.dimension().location(), getPos(), localCapacity);
            if (!capacityRegistered) {
                capacityRegistered = true;
                markDirty();
            }
        } else {
            clearCapacity();
        }
    }

    private void clearCapacity() {
        if (!(getLevel() instanceof ServerLevel level)) return;
        CloudStorageData.get(level).removeCapacity(kind, level.dimension().location(), getPos());
        if (capacityRegistered) {
            capacityRegistered = false;
            markDirty();
        }
        stopReconciliation();
    }

    private void reconcileCapacityOnLoad() {
        if (capacityRegistered && reconciliationSubscription == null && getLevel() instanceof ServerLevel) {
            reconciliationSubscription = subscribeServerTick(this::checkSavedCapacity);
        }
    }

    private void checkSavedCapacity() {
        if (getOffsetTimer() % 20 != 0 || !(getLevel() instanceof ServerLevel level) ||
                !capacityChunksLoaded(level) || !getPatternLock().tryLock()) return;
        try {
            // Native reformation republishes capacity. An unloaded section is not evidence of dismantling.
            if (!checkPattern() && getMultiblockState().error != MultiblockState.UNLOAD_ERROR) clearCapacity();
        } finally {
            getPatternLock().unlock();
        }
    }

    private boolean capacityChunksLoaded(ServerLevel level) {
        for (long chunk : capacityChunks) {
            if (!level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) return false;
        }
        return true;
    }

    private void stopReconciliation() {
        unsubscribe(reconciliationSubscription);
        reconciliationSubscription = null;
    }

    private void clearPorts() {
        itemInputs.clear();
        itemOutputs.clear();
        fluidInputs.clear();
        fluidOutputs.clear();
        canImport = false;
        canExport = false;
        localCapacity = 0;
    }

    private boolean hasBufferedContents() {
        return itemInputs.stream().anyMatch(bus -> !bus.getInventory().isEmpty()) ||
                itemOutputs.stream().anyMatch(bus -> !bus.getInventory().isEmpty()) ||
                fluidInputs.stream().anyMatch(hatch -> !hatch.tank.isEmpty()) ||
                fluidOutputs.stream().anyMatch(hatch -> !hatch.tank.isEmpty());
    }

    private void updateTransferSubscription() {
        if (!isOperational() || !CloudStorageData.validColor(color)) stopTransfer();
        else if (transferSubscription == null) transferSubscription = subscribeServerTick(this::transfer);
    }

    private void stopTransfer() {
        unsubscribe(transferSubscription);
        transferSubscription = null;
    }

    private void transfer() {
        if (getOffsetTimer() % TRANSFER_INTERVAL != 0 || !isOperational()) return;
        CloudStorageData data = getCloudData();
        if (data == null) return;
        for (var bus : itemOutputs) if (bus.isWorkingEnabled() && !bus.isInValid()) exportItems(data, bus.getInventory());
        for (var bus : itemInputs) if (bus.isWorkingEnabled() && !bus.isInValid()) importItems(data, bus.getInventory());
        for (var hatch : fluidOutputs) if (hatch.isWorkingEnabled() && !hatch.isInValid()) exportFluid(data, hatch.tank);
        for (var hatch : fluidInputs) if (hatch.isWorkingEnabled() && !hatch.isInValid()) importFluids(data, hatch.tank);
    }

    private void importItems(CloudStorageData data, NotifiableItemStackHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var offered = inventory.getStackInSlot(slot);
            if (offered.isEmpty()) continue;
            int accepted = offered.getCount() - data.insertItem(color, offered, true).getCount();
            if (accepted <= 0) continue;
            var extracted = inventory.extractItemInternal(slot, accepted, false);
            data.insertItem(color, extracted, false);
        }
    }

    private void exportItems(CloudStorageData data, NotifiableItemStackHandler inventory) {
        for (int i = 0; i < ITEM_EXPORT_ATTEMPTS; i++) {
            int slot = data.nextItemSlot(color, nextExportSlot);
            if (slot < 0) {
                nextExportSlot = 0;
                return;
            }
            nextExportSlot = slot + 1;
            var offered = data.itemInSlot(color, slot);
            int accepted = offered.getCount() - ItemHandlerHelper.insertItemStacked(inventory.storage, offered, false)
                    .getCount();
            if (accepted > 0) data.extractItem(color, slot, accepted, false);
        }
    }

    private void importFluids(CloudStorageData data, NotifiableFluidTank tank) {
        for (int slot = 0; slot < tank.getTanks(); slot++) {
            FluidStack offered = tank.getFluidInTank(slot);
            if (offered.isEmpty()) continue;
            int accepted = data.fill(color, offered, FluidAction.SIMULATE);
            if (accepted > 0) {
                var extracted = tank.drainInternal(new FluidStack(offered, accepted), FluidAction.EXECUTE);
                data.fill(color, extracted, FluidAction.EXECUTE);
            }
        }
    }

    private void exportFluid(CloudStorageData data, NotifiableFluidTank tank) {
        FluidStack offered = data.fluid(color);
        if (offered.isEmpty()) return;
        int accepted = tank.fillInternal(offered, FluidAction.EXECUTE);
        if (accepted > 0) data.drain(color, new FluidStack(offered, accepted), FluidAction.EXECUTE);
    }

    public static final class Chest extends CloudStorageMachine implements IUIMachine {
        public Chest(IMachineBlockEntity holder) { super(holder, CloudStorageData.Kind.ITEMS); }

        @Override
        public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
            return CloudStorageData.validColor(getColor()) && (isRemote() ? isFormed() : isOperational());
        }

        @Override
        public ModularUI createUI(Player player) {
            return CloudStorageUi.createChestUI(this, player);
        }
    }

    public static final class Tank extends CloudStorageMachine {
        public Tank(IMachineBlockEntity holder) { super(holder, CloudStorageData.Kind.FLUIDS); }
    }
}
