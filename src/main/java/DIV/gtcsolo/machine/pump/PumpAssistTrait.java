package DIV.gtcsolo.machine.pump;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/** Native pipe/cable capabilities attached only to an existing primitive pump core. */
public final class PumpAssistTrait extends MachineTrait implements PumpAssistLogic.Ports {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(PumpAssistTrait.class);
    private static final String SAVE_KEY = "gtcsolo_pump_assist";
    public static final long ENERGY_CAPACITY = 6_400;
    public static final int STEAM_CAPACITY = 16_000;

    private final NotifiableEnergyContainer energy;
    private final NotifiableFluidTank steam;
    private final PumpAssistLogic logic = new PumpAssistLogic();
    private NotifiableFluidTank output;
    private FluidStack consumedSteam = FluidStack.EMPTY;
    private boolean restoring;

    public PumpAssistTrait(MetaMachine machine) {
        super(machine);
        energy = NotifiableEnergyContainer.receiverContainer(machine, ENERGY_CAPACITY, GTValues.V[GTValues.LV], 1);
        steam = new NotifiableFluidTank(machine, 1, STEAM_CAPACITY, IO.IN)
                .setFilter(stack -> stack.getFluid().is(GTMaterials.Steam.getFluidTag()));
        energy.addChangedListener(this::contentsChanged);
        steam.addChangedListener(this::contentsChanged);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return FIELDS;
    }

    private void contentsChanged() {
        if (!restoring && !machine.isRemote()) machine.markDirty();
    }

    public void tick(int passiveWaterPerSecond, NotifiableFluidTank output) {
        this.output = output;
        try {
            logic.tick(passiveWaterPerSecond, this);
        } finally {
            // Do not retain a part after its controller tick (structure invalidation/chunk unload).
            this.output = null;
            consumedSteam = FluidStack.EMPTY;
        }
    }

    public void reset() {
        logic.reset();
        output = null;
    }

    @Override
    public long storedEnergy() {
        return energy.getEnergyStored();
    }

    @Override
    public int storedSteam() {
        return steam.getFluidInTank(0).getAmount();
    }

    @Override
    public int fillWater(int amount, boolean simulate) {
        // Internal fill preserves the hatch's fluid filter and permits its normal output-only capability.
        return output == null ? 0 : output.fillInternal(GTMaterials.Water.getFluid(amount),
                simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }

    @Override
    public boolean consume(PumpAssistLogic.Power power, int amount) {
        if (power == PumpAssistLogic.Power.ELECTRIC) {
            if (energy.getEnergyStored() < amount) return false;
            energy.removeEnergy(amount);
        } else {
            if (storedSteam() < amount) return false;
            consumedSteam = steam.getStorages()[0].drain(amount, FluidAction.EXECUTE);
        }
        return true;
    }

    @Override
    public void refund(PumpAssistLogic.Power power, int amount) {
        if (power == PumpAssistLogic.Power.ELECTRIC) energy.addEnergy(amount);
        else {
            FluidStack refund = consumedSteam.copy();
            refund.setAmount(amount);
            steam.fillInternal(refund, FluidAction.EXECUTE);
        }
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        // Mixin-added fields are not in the native controller's managed field holder.
        CompoundTag saved = new CompoundTag();
        saved.putLong("energy", energy.getEnergyStored());
        saved.put("steam", steam.getFluidInTank(0).writeToNBT(new CompoundTag()));
        tag.put(SAVE_KEY, saved);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        restoring = true;
        try {
            CompoundTag saved = tag.getCompound(SAVE_KEY);
            energy.setEnergyStored(Math.max(0, Math.min(ENERGY_CAPACITY, saved.getLong("energy"))));
            FluidStack loaded = FluidStack.loadFluidStackFromNBT(saved.getCompound("steam"));
            if (!loaded.isEmpty() && steam.isFluidValid(0, loaded)) {
                loaded.setAmount(Math.min(STEAM_CAPACITY, loaded.getAmount()));
                steam.setFluidInTank(0, loaded);
            } else steam.setFluidInTank(0, FluidStack.EMPTY);
            reset();
        } finally {
            restoring = false;
        }
    }
}
