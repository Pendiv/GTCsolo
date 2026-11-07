package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.ICapabilityTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.gas.IGasTank;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/** Independent gas slots shared by GT recipes and Mekanism pipes. */
public class NotifiableGasBank extends NotifiableRecipeHandlerTrait<ChemicalIngredient>
        implements ICapabilityTrait, IGasHandler {

    private static final String PERSISTENCE_KEY = "gtcsolo_gas_bank";

    private final IO io;
    private final long capacity;
    private final IGasTank[] tanks;
    private boolean restoring;

    public NotifiableGasBank(MetaMachine machine, int tankCount, long capacityPerTank, IO io) {
        super(machine);
        if (tankCount <= 0 || capacityPerTank <= 0) {
            throw new IllegalArgumentException("Gas bank size and capacity must be positive");
        }
        this.io = io;
        this.capacity = capacityPerTank;
        this.tanks = new IGasTank[tankCount];
        for (int i = 0; i < tanks.length; i++) tanks[i] = createTank(this::onContentsChanged);
    }

    private IGasTank createTank(Runnable listener) {
        return ChemicalTankBuilder.GAS.create(capacity,
                (gas, automation) -> automation == AutomationType.INTERNAL || io == IO.OUT || io == IO.BOTH,
                (gas, automation) -> automation == AutomationType.INTERNAL || io == IO.IN || io == IO.BOTH,
                gas -> true, ChemicalAttributeValidator.ALWAYS_ALLOW, listener::run);
    }

    private void onContentsChanged() {
        if (!restoring) {
            machine.markDirty();
            notifyListeners();
        }
    }

    /** Per-slot capacity, in mB. */
    public long getCapacity() { return capacity; }

    public IGasTank getMekTank(int tank) { return tanks[tank]; }

    public GasStack getStack(int tank) { return tanks[tank].getStack().copy(); }

    @Override
    public IO getHandlerIO() { return io; }

    @Override
    public IO getCapabilityIO() { return io; }

    @Override
    public RecipeCapability<ChemicalIngredient> getCapability() { return ChemicalCapabilities.GAS; }

    @Override
    public int getTanks() { return tanks.length; }

    @Override
    public GasStack getChemicalInTank(int tank) {
        return validIndex(tank) ? tanks[tank].getStack() : GasStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, GasStack stack) {
        if (validIndex(tank)) tanks[tank].setStack(stack.copy());
    }

    @Override
    public long getTankCapacity(int tank) { return validIndex(tank) ? capacity : 0; }

    @Override
    public boolean isValid(int tank, GasStack stack) {
        return validIndex(tank) && tanks[tank].isValid(stack);
    }

    @Override
    public GasStack insertChemical(int tank, GasStack stack, Action action) {
        return validIndex(tank) ? tanks[tank].insert(stack, action, AutomationType.EXTERNAL) : stack;
    }

    @Override
    public GasStack extractChemical(int tank, long amount, Action action) {
        return validIndex(tank) ? tanks[tank].extract(amount, action, AutomationType.EXTERNAL) : GasStack.EMPTY;
    }

    @Override
    public GasStack insertChemical(GasStack stack, Action action) {
        return insertAcross(tanks, stack, action, AutomationType.EXTERNAL);
    }

    private boolean validIndex(int tank) { return tank >= 0 && tank < tanks.length; }

    /** Fill already matching slots before claiming an empty slot for a new gas. */
    private static GasStack insertAcross(IGasTank[] target, GasStack stack, Action action,
                                         AutomationType automation) {
        GasStack remaining = stack;
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (IGasTank tank : target) {
                if (pass == 0 ? tank.isEmpty() || !tank.isTypeEqual(remaining) : !tank.isEmpty()) continue;
                remaining = tank.insert(remaining, action, automation);
                if (remaining.isEmpty()) break;
            }
        }
        return remaining;
    }

    @Override
    public List<ChemicalIngredient> handleRecipeInner(IO recipeIO, GTRecipe recipe,
                                                     List<ChemicalIngredient> left,
                                                     String slotName, boolean simulate) {
        if (left.isEmpty()) return null;
        if (recipeIO != io && io != IO.BOTH) return left;
        if (recipeIO != IO.IN && recipeIO != IO.OUT) return left;

        IGasTank[] transaction = tanks;
        if (simulate) {
            // Every request in this call shares a detached snapshot. A repeated request
            // must not independently spend the same content or claim the same empty slot.
            transaction = new IGasTank[tanks.length];
            for (int i = 0; i < tanks.length; i++) {
                transaction[i] = createTank(() -> {});
                transaction[i].setStackUnchecked(tanks[i].getStack().copy());
            }
        }
        for (int i = 0; i < left.size(); i++) {
            ChemicalIngredient requested = left.get(i);
            if (requested.getType() != ChemicalIngredient.Type.GAS) continue;
            if (requested.isEmpty()) { left.remove(i--); continue; }

            long remaining = requested.getAmount();
            if (recipeIO == IO.IN) {
                for (IGasTank tank : transaction) {
                    if (!requested.test(tank.getStack())) continue;
                    remaining -= tank.extract(remaining, Action.EXECUTE, AutomationType.INTERNAL).getAmount();
                    if (remaining == 0) break;
                }
            } else {
                GasStack output = (GasStack) requested.toStack();
                // A missing registry entry is an unsatisfied output, never discarded.
                if (output.isEmpty()) continue;
                remaining = insertAcross(transaction, output, Action.EXECUTE, AutomationType.INTERNAL).getAmount();
            }
            if (remaining == 0) left.remove(i--);
            else if (remaining != requested.getAmount()) {
                left.set(i, new ChemicalIngredient(requested.getType(), requested.getId(), remaining));
            }
        }
        return left.isEmpty() ? null : left;
    }

    @Override
    public List<Object> getContents() {
        List<Object> contents = new ArrayList<>(tanks.length);
        for (IGasTank tank : tanks) {
            GasStack stack = tank.getStack();
            if (!stack.isEmpty()) {
                contents.add(new ChemicalIngredient(ChemicalIngredient.Type.GAS,
                        stack.getTypeRegistryName(), stack.getAmount()));
            }
        }
        return contents;
    }

    @Override
    public double getTotalContentAmount() {
        double total = 0;
        for (IGasTank tank : tanks) total += tank.getStored();
        return total;
    }

    /** Also suitable for server-to-client UI synchronization; amounts remain long. */
    public CompoundTag serializeContents() {
        CompoundTag contents = new CompoundTag();
        for (int i = 0; i < tanks.length; i++) {
            contents.put(Integer.toString(i), tanks[i].serializeNBT());
        }
        return contents;
    }

    /** Replace all slots, including empty ones, without firing recipe or save listeners. */
    public void deserializeContents(CompoundTag contents) {
        boolean previousRestoring = restoring;
        restoring = true;
        try {
            for (int i = 0; i < tanks.length; i++) {
                tanks[i].setStackUnchecked(GasStack.EMPTY);
                tanks[i].deserializeNBT(contents.getCompound(Integer.toString(i)));
            }
        } finally {
            restoring = previousRestoring;
        }
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        tag.put(PERSISTENCE_KEY, serializeContents());
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        deserializeContents(tag.getCompound(PERSISTENCE_KEY));
    }
}
