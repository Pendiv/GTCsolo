package DIV.gtcsolo.machine.water;

import DIV.gtcsolo.api.pattern.FlexibleCoreMultiblockMachine;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/** A passive, bounded reservoir. Walls share this single tank; they never generate or store water. */
public final class WaterServerMachine extends FlexibleCoreMultiblockMachine implements IDisplayUIMachine, IDropSaveMachine {
    public static final int CAPACITY = 200_000;
    public static final int PRODUCTION = 500;
    private static final Direction[] OUTPUT_FACES = Direction.values();
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(WaterServerMachine.class, MANAGED_FIELD_HOLDER);
    @Persisted private final NotifiableFluidTank water;
    @Persisted @DropSaved private int carriedWater;
    private TickableSubscription waterSubscription;
    private ISubscription waterChanges;

    public WaterServerMachine(IMachineBlockEntity holder) {
        super(holder);
        water = new NotifiableFluidTank(this, 1, CAPACITY, IO.OUT).setFilter(stack -> stack.getFluid() == Fluids.WATER);
    }

    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }
    public NotifiableFluidTank getWaterTank() { return water; }
    public int getStoredWater() { return water.getFluidInTank(0).getAmount(); }
    public boolean canServeWalls() { return isFormed() && (isRemote() || !getMultiblockState().hasError()); }
    public boolean containsWoodWall(BlockPos position) {
        return WaterServerStructure.containsWall(getPos(), getFrontFacing(), getCorePosition(), position);
    }

    @Override protected BlockPattern[] createCorePatterns() {
        BlockPattern[] result = new BlockPattern[WaterServerStructure.CORE_POSITIONS.size()];
        for (int i = 0; i < result.length; i++) result[i] = WaterServerStructure.pattern(getDefinition(), i);
        return result;
    }

    @Override public void onLoad() {
        // Native isFormed is persisted; wait for this load's successful check before producing/serving.
        if (getLevel() instanceof ServerLevel) isFormed = false;
        super.onLoad();
        waterChanges = water.addChangedListener(() -> { onChanged(); updateSubscription(); });
        updateSubscription();
    }

    @Override public void onUnload() {
        if (getLevel() instanceof ServerLevel) isFormed = false;
        if (waterChanges != null) waterChanges.unsubscribe();
        waterChanges = null;
        if (waterSubscription != null) waterSubscription.unsubscribe();
        waterSubscription = null;
        super.onUnload();
    }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        updateSubscription();
    }

    @Override public void onStructureInvalid() { super.onStructureInvalid(); updateSubscription(); }
    @Override public void onPartUnload() {
        // Native async probing temporarily clears the match error; a stale formed flag must not enable supply.
        if (getLevel() instanceof ServerLevel) isFormed = false;
        super.onPartUnload();
        updateSubscription();
    }

    private void updateSubscription() {
        if (!(getLevel() instanceof ServerLevel) || isInValid()) return;
        boolean generate = canServeWalls() && getStoredWater() < CAPACITY;
        // Pipes can expose their connection after placement without notifying this core again.
        // Keep retrying while water remains, including when the reservoir is full.
        boolean export = getStoredWater() > 0;
        if (generate || export) waterSubscription = subscribeServerTick(waterSubscription, this::waterTick);
        else if (waterSubscription != null) {
            waterSubscription.unsubscribe();
            waterSubscription = null;
        }
    }

    public static int produce(IFluidHandler storage, boolean formed) {
        return formed ? storage.fill(new FluidStack(Fluids.WATER, PRODUCTION), IFluidHandler.FluidAction.EXECUTE) : 0;
    }

    private void waterTick() {
        produce(water.getStorages()[0], canServeWalls());
        if (getOffsetTimer() % 5 == 0 && getLevel() instanceof ServerLevel level) {
            for (Direction face : OUTPUT_FACES)
                if (level.isLoaded(getPos().relative(face))) water.exportToNearby(face);
        }
        updateSubscription();
    }

    @Override public void saveToItem(CompoundTag tag) {
        carriedWater = getStoredWater();
        IDropSaveMachine.super.saveToItem(tag);
    }

    @Override public void loadFromItem(CompoundTag tag) {
        IDropSaveMachine.super.loadFromItem(tag);
        int amount = Math.max(0, Math.min(CAPACITY, carriedWater));
        water.getStorages()[0].setFluid(amount == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, amount));
    }

    @Override public boolean savePickClone() { return false; }

    @Override public void addDisplayText(List<Component> lines) {
        lines.add(Component.translatable(canServeWalls() ? "gtcsolo.water_server.ready" : "gtceu.multiblock.invalid_structure")
                .withStyle(canServeWalls() ? ChatFormatting.GREEN : ChatFormatting.RED));
        lines.add(Component.translatable("gtcsolo.water_server.stored", FormattingUtil.formatNumbers(getStoredWater()),
                FormattingUtil.formatNumbers(CAPACITY)));
        lines.add(Component.translatable("gtcsolo.water_server.rate", FormattingUtil.formatNumbers(PRODUCTION)));
    }
}
