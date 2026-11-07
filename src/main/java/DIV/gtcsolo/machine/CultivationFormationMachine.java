package DIV.gtcsolo.machine;

import DIV.gtcsolo.api.tier.RoomStructureTier;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

public final class CultivationFormationMachine extends ResourceRoomMachine
        implements IMachineModifyDrops {
    private static final ManagedFieldHolder FIELDS =
            new ManagedFieldHolder(CultivationFormationMachine.class, ResourceRoomMachine.FIELDS);
    @Persisted private CompoundTag essenceTransfer = new CompoundTag();
    private ElementalEssenceCycle cycle;

    public CultivationFormationMachine(IMachineBlockEntity holder, RoomStructureTier tiers) {
        super(holder, tiers, false);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return FIELDS;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new FormationLogic(this);
    }

    private ElementalEssenceCycle cycle() {
        if (cycle == null) cycle = ElementalEssenceCycle.load(essenceTransfer);
        return cycle;
    }

    private void syncCycle() {
        if (cycle().takeChanged()) {
            essenceTransfer = cycle.save();
            markDirty();
        }
    }

    public List<IItemHandler> essenceInventories(IO direction) {
        var inventories = new ArrayList<IItemHandler>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        for (var io : List.of(direction, IO.BOTH)) {
            var handlers = getCapabilitiesProxy().get(io, ItemRecipeCapability.CAP);
            if (handlers != null)
                for (var handler : handlers) {
                    if (handler instanceof IItemHandler items && seen.add(handler))
                        inventories.add(items);
                }
        }
        return inventories;
    }

    private boolean hasEssences() {
        return ElementalEssenceCycle.hasRequiredEssences(essenceInventories(IO.IN));
    }

    private void flushEssences() {
        if (!isFormed()) return;
        cycle().flush(essenceInventories(cycle.isReturning() ? IO.IN : IO.OUT));
        syncCycle();
    }

    private boolean ensurePlan() {
        boolean ready = cycle().ensurePlan(essenceInventories(IO.IN), getLevel().random);
        syncCycle();
        return ready;
    }

    private GTRecipe withTransferOutputs(GTRecipe source) {
        var recipe = source.copy();
        var items = new ArrayList<>(recipe.getOutputContents(ItemRecipeCapability.CAP));
        for (var stack : cycle().plannedOutputs())
            items.add(new Content(SizedIngredient.create(stack), 10000, 10000, 0, null, null));
        recipe.outputs.put(ItemRecipeCapability.CAP, items);
        return recipe;
    }

    private boolean fitsOutputs(GTRecipe recipe) {
        var combined = withTransferOutputs(recipe);
        return combined.matchRecipeContents(IO.OUT, this, combined.outputs, false).isSuccess();
    }

    public static ModifierFunction modify(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof CultivationFormationMachine room)
                || !room.isFormed()
                || !room.ensurePlan()) {
            return ModifierFunction.NULL;
        }
        return RoomRecipeModifiers.modify(machine, recipe);
    }

    @Override
    public int limitExtraOutputs(GTRecipe capacity, int parallel) {
        int low = 0, high = parallel;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (fitsOutputs(capacity.copy(ContentModifier.multiplier(mid), false))) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    @Override
    public boolean beforeWorking(GTRecipe recipe) {
        return !cycle().isActive()
                && hasEssences()
                && ensurePlan()
                && fitsOutputs(recipe)
                && super.beforeWorking(recipe);
    }

    @Override
    public void onDrops(List<ItemStack> drops) {
        drops.addAll(cycle().storedItems());
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        cycle = null;
    }

    @Override
    public void addDisplayText(List<Component> lines) {
        super.addDisplayText(lines);
        if (isFormed() && cycle().hasDelivery()) {
            lines.add(Component.translatable("gtcsolo.cultivation.output_wait"));
        }
        if (isFormed() && !hasEssences())
            lines.add(Component.translatable("gtcsolo.cultivation.essences_required"));
    }

    public static final class FormationLogic extends RecipeLogic {
        private final CultivationFormationMachine room;

        public FormationLogic(CultivationFormationMachine room) {
            super(room);
            this.room = room;
        }

        @Override
        public void resetRecipeLogic() {
            if (room != null && room.essenceTransfer != null) {
                room.cycle().abort();
                room.syncCycle();
            }
            super.resetRecipeLogic();
        }

        @Override
        public void setupRecipe(GTRecipe recipe) {
            super.setupRecipe(recipe);
            if (isWorking() && lastRecipe == recipe && !room.cycle().isActive()) {
                room.cycle().begin();
                room.syncCycle();
            }
        }

        @Override
        public void serverTick() {
            if (room.getOffsetTimer() % 5 == 0) room.flushEssences();
            super.serverTick();
        }

        @Override
        public void handleRecipeWorking() {
            if (!room.hasEssences()) {
                setWaiting(Component.translatable("gtcsolo.cultivation.essences_required"));
                return;
            }
            super.handleRecipeWorking();
        }

        @Override
        public void onRecipeFinish() {
            if (lastRecipe == null) return;
            if (!room.fitsOutputs(lastRecipe)) {
                setWaiting(Component.translatable("gtceu.recipe_logic.insufficient_out"));
                return;
            }
            boolean ready = room.cycle().collect(room.essenceInventories(IO.IN));
            room.syncCycle();
            if (!ready) {
                setWaiting(Component.translatable("gtcsolo.cultivation.essences_required"));
                return;
            }
            super.onRecipeFinish();
            room.flushEssences();
        }

        @Override
        public void interruptRecipe() {
            room.cycle().abort();
            room.syncCycle();
            super.interruptRecipe();
            room.flushEssences();
        }
    }
}
