package DIV.gtcsolo.integration.mekanism.capability;

import DIV.gtcsolo.machine.overpower.HatchRecipeTransfers;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

/** Item bus, five fluid tanks and five independent gas tanks in one physical part. */
public class MultiChemicalBusMachine extends ItemBusPartMachine implements IDropSaveMachine {
    public static final int FLUID_SLOTS = 5;
    public static final int GAS_SLOTS = 5;
    private static final String PACKED_CONTENTS = "gtcsolo_multi_bus_contents";
    private static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MultiChemicalBusMachine.class, ItemBusPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private final NotifiableFluidTank fluidTank;
    // Managed data stores trait settings; slot contents use the trait's custom NBT.
    @Persisted
    private final NotifiableGasBank gasBank;
    private LazyOptional<IGasHandler> gasCapability;
    private ISubscription fluidSubscription;

    public MultiChemicalBusMachine(IMachineBlockEntity holder, int tier, IO io) {
        super(holder, checkedTier(tier), io);
        if (io != IO.IN && io != IO.OUT) throw new IllegalArgumentException("A bus must be IN or OUT");
        fluidTank = new MultiFluidTank(this, FLUID_SLOTS, fluidCapacity(tier), io);
        gasBank = new NotifiableGasBank(this, GAS_SLOTS, gasCapacity(tier), io);
        gasCapability = LazyOptional.of(() -> gasBank);
    }

    private static int checkedTier(int tier) {
        if (tier < GTValues.IV || tier > GTValues.MAX) throw new IllegalArgumentException("Unsupported bus tier: " + tier);
        return tier;
    }

    public static int itemSlotCount(int tier) {
        int side = 1 + Math.min(9, checkedTier(tier));
        return side * side;
    }

    public static int fluidCapacity(int tier) {
        // Standard IV single-fluid hatch capacity; continue doubling beyond UHV.
        return Math.toIntExact(256_000L << (checkedTier(tier) - GTValues.IV));
    }

    public static long gasCapacity(int tier) {
        return 1_024_000L << (checkedTier(tier) - GTValues.IV);
    }

    @Override public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }
    public NotifiableFluidTank getFluidTank() { return fluidTank; }
    public NotifiableGasBank getGasBank() { return gasBank; }

    public <T> LazyOptional<T> getMekCapability(Capability<T> cap) {
        return cap == Capabilities.GAS_HANDLER ? gasCapability.cast() : LazyOptional.empty();
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (!forDrop) return;
        // LDLib filters managed fields by @DropSaved here. GT's parent inventory
        // and our fluid storage are only @Persisted, so pack their contents explicitly.
        // Do not save the entire BE with forDrop=false: that would also copy covers.
        CompoundTag packed = new CompoundTag();
        packed.put("items", getInventory().storage.serializeNBT());
        packed.put("circuit", circuitInventory.storage.serializeNBT());
        CompoundTag fluids = new CompoundTag();
        for (int slot = 0; slot < FLUID_SLOTS; slot++) {
            fluids.put(Integer.toString(slot), fluidTank.getStorages()[slot].writeToNBT(new CompoundTag()));
        }
        packed.put("fluids", fluids);
        packed.putBoolean("distinct", isDistinct());
        packed.putBoolean("working", isWorkingEnabled());
        tag.put(PACKED_CONTENTS, packed);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (!tag.contains(PACKED_CONTENTS, Tag.TAG_COMPOUND)) return;
        CompoundTag packed = tag.getCompound(PACKED_CONTENTS);
        getInventory().storage.deserializeNBT(packed.getCompound("items"));
        circuitInventory.storage.deserializeNBT(packed.getCompound("circuit"));
        CompoundTag fluids = packed.getCompound("fluids");
        for (int slot = 0; slot < FLUID_SLOTS; slot++) {
            fluidTank.getStorages()[slot].readFromNBT(fluids.getCompound(Integer.toString(slot)));
        }
        setDistinct(packed.getBoolean("distinct"));
        setWorkingEnabled(packed.getBoolean("working"));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!gasCapability.isPresent()) gasCapability = LazyOptional.of(() -> gasBank);
        fluidSubscription = fluidTank.addChangedListener(this::updateInventorySubscription);
    }

    @Override
    public void onUnload() {
        if (fluidSubscription != null) {
            fluidSubscription.unsubscribe();
            fluidSubscription = null;
        }
        gasCapability.invalidate();
        super.onUnload();
    }

    @Override
    public void onMachineRemoved() {
        // IDropSaveMachine invokes the custom drop serializer above. Calling the
        // parent ItemBusPartMachine here would additionally spill the same items.
    }

    @Override
    public void setDistinct(boolean distinct) {
        super.setDistinct(distinct);
        if (fluidTank != null) fluidTank.setDistinct(distinct);
        if (gasBank != null) gasBank.setDistinct(distinct);
    }

    @Override
    protected void updateInventorySubscription() {
        if (!(getLevel() instanceof ServerLevel) || fluidTank == null) return;
        boolean itemWork = (io == IO.IN || !getInventory().isEmpty())
                && GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), getFrontFacing());
        boolean fluidWork = (io == IO.IN || !fluidTank.isEmpty())
                && GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getPos(), getFrontFacing());
        if (isWorkingEnabled() && (itemWork || fluidWork)) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    @Override
    protected void autoIO() {
        if (getOffsetTimer() % 5 != 0) return;
        if (isWorkingEnabled()) {
            if (io == IO.IN) {
                getInventory().importFromNearby(getFrontFacing());
                fluidTank.importFromNearby(getFrontFacing());
            } else {
                getInventory().exportToNearby(getFrontFacing());
                fluidTank.exportToNearby(getFrontFacing());
            }
        }
        updateInventorySubscription();
    }

    @Override
    public Widget createUIWidget() {
        int columns = 1 + Math.min(9, getTier());
        int width = Math.max(176, columns * 18 + 24);
        WidgetGroup group = new WidgetGroup(0, 0, width, 166);
        // Keep all fluid/gas slots visible even at the 100-item-slot tiers.
        DraggableScrollableWidgetGroup items = new DraggableScrollableWidgetGroup(
                (width - columns * 18 - 4) / 2, 6, columns * 18 + 4, 76);
        items.setYScrollBarWidth(4);
        for (int index = 0; index < getInventory().getSlots(); index++) {
            items.addWidget(new SlotWidget(getInventory().storage, index,
                    (index % columns) * 18, (index / columns) * 18, true, io == IO.IN)
                    .setBackgroundTexture(GuiTextures.SLOT)
                    .setIngredientIO(io == IO.IN ? IngredientIO.INPUT : IngredientIO.OUTPUT));
        }
        group.addWidget(items);
        group.addWidget(new LabelWidget(8, 89, "gtcsolo.gui.multi_chemical_bus.fluids"));
        group.addWidget(new LabelWidget(8, 127, "gtcsolo.gui.multi_chemical_bus.gases"));
        int left = (width - FLUID_SLOTS * 18) / 2;
        for (int slot = 0; slot < FLUID_SLOTS; slot++) {
            group.addWidget(new TankWidget(fluidTank.getStorages()[slot], left + 18 * slot, 101,
                    true, io == IO.IN).setShowAmount(true).setDrawHoverTips(true)
                    .setBackground(GuiTextures.FLUID_SLOT));
        }
        for (int slot = 0; slot < GAS_SLOTS; slot++) {
            int index = slot;
            GasTankWidget widget = new GasTankWidget(() -> gasBank.getStack(index), gasBank.getCapacity());
            widget.setSelfPosition(new Position(left + 18 * slot, 139));
            widget.setBackground(GuiTextures.FLUID_SLOT);
            widget.setIngredientIO(io.name());
            group.addWidget(widget);
        }
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    /** Cumulative simulation is required when a recipe repeats a fluid ingredient. */
    public static final class MultiFluidTank extends NotifiableFluidTank {
        public MultiFluidTank(MetaMachine machine, int slots, int capacity, IO io) {
            super(machine, slots, capacity, io);
            allowSameFluids = true;
        }

        @Override
        public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left,
                                                      String slotName, boolean simulate) {
            if (io != handlerIO) return left;
            return HatchRecipeTransfers.largeFluids(left, storages, io == IO.IN, allowSameFluids, simulate);
        }
    }
}
