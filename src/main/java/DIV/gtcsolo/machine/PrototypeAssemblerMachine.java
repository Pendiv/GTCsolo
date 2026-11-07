package DIV.gtcsolo.machine;

import DIV.gtcsolo.machine.starforge.StarForgeOutputBuffer;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.*;

/** A paid first attempt followed by locked, material-free retries until success. */
public class PrototypeAssemblerMachine extends WorkableElectricMultiblockMachine {
    private static final OverclockingLogic PROTOTYPE_OC =
            (parameters, voltage) -> OverclockingLogic.standardOC(parameters, voltage, 0.67, 4.0);

    public PrototypeAssemblerMachine(IMachineBlockEntity holder) { super(holder); }

    static IItemHandlerModifiable itemOutputStorage(IRecipeHandler<?> handler) {
        // insertItem on the capability intentionally rejects insertion into OUTPUT buses.
        // Recipe output must use GT's internal storage, as its own recipe handler does.
        if (handler instanceof NotifiableItemStackHandler inventory) return inventory.storage;
        return handler instanceof IItemHandlerModifiable inventory ? inventory : null;
    }

    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new PrototypeLogic(this); }
    @Override public PrototypeLogic getRecipeLogic() { return (PrototypeLogic) super.getRecipeLogic(); }

    public int getHatchTier() {
        if (energyContainer == null) energyContainer = getEnergyContainer();
        long voltage = energyContainer.getHighestInputVoltage();
        return voltage > 0 ? GTUtil.getTierByVoltage(voltage) : -1;
    }

    @Override public long getMaxVoltage() {
        int hatchTier = getHatchTier();
        return hatchTier < 0 ? 0 : GTValues.V[Math.min(GTValues.MAX, hatchTier + 2)];
    }

    @Override public long getOverclockVoltage() {
        return Math.min(super.getOverclockVoltage(), getMaxVoltage());
    }

    public static ModifierFunction prototypeOverclock(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof PrototypeAssemblerMachine prototype)
                || recipe.recipeType != GTRecipeTypes.ASSEMBLER_RECIPES
                || !PrototypeAssemblyRules.canRun(RecipeHelper.getRecipeEUtTier(recipe), prototype.getHatchTier())) {
            return ModifierFunction.NULL;
        }
        // Assembler output is normally one item. Preserve possible fluid outputs as well,
        // but never accept an unsupported/tick output that the success buffer cannot retain.
        if (!recipe.tickOutputs.isEmpty() || recipe.outputs.keySet().stream().anyMatch(cap ->
                cap != ItemRecipeCapability.CAP && cap != FluidRecipeCapability.CAP)) return ModifierFunction.NULL;
        return PROTOTYPE_OC.getModifier(machine, recipe, prototype.getOverclockVoltage(), false);
    }

    @Override public void addDisplayText(List<Component> lines) {
        super.addDisplayText(lines);
        if (!isFormed()) return;
        PrototypeLogic logic = getRecipeLogic();
        lines.add(Component.translatable("gtcsolo.prototype.success_chance",
                String.format(Locale.ROOT, "%.1f", logic.getSuccessChance() / 100.0)));
        lines.add(Component.translatable("gtcsolo.prototype.failures", logic.failures));
        GTRecipe displayed = logic.resultRecipe != null ? logic.resultRecipe
                : logic.recordedRecipe != null ? logic.recordedRecipe : logic.getLastRecipe();
        if (displayed != null) {
            for (Content content : displayed.getOutputContents(ItemRecipeCapability.CAP)) {
                var ingredient = ItemRecipeCapability.CAP.copyContent(content.content);
                ItemStack[] choices = ingredient.getItems();
                if (choices.length > 0 && !choices[0].isEmpty()) {
                    int count = ingredient instanceof SizedIngredient sized ? sized.getAmount() : choices[0].getCount();
                    lines.add(Component.translatable("gtcsolo.prototype.output", choices[0].getHoverName(), count));
                }
            }
        }
        if (logic.pendingSuccess) lines.add(Component.translatable("gtcsolo.prototype.output_wait"));
        else if (logic.recordedRecipe != null) lines.add(Component.translatable("gtcsolo.prototype.locked"));
    }

    public static class PrototypeLogic extends RecipeLogic {
        public static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
                new ManagedFieldHolder(PrototypeLogic.class, RecipeLogic.MANAGED_FIELD_HOLDER);
        @Persisted @DescSynced private GTRecipe recordedRecipe;
        @Persisted @DescSynced private GTRecipe resultRecipe;
        @Persisted @DescSynced private int originalRecipeTier = -1;
        @Persisted @DescSynced private int failures;
        @Persisted @DescSynced private int accumulatedChance;
        @Persisted @DescSynced private boolean pendingSuccess;
        @Persisted private CompoundTag pendingOutputData = new CompoundTag();
        private final StarForgeOutputBuffer pendingOutputs = new StarForgeOutputBuffer();
        private boolean outputBufferLoaded;

        public PrototypeLogic(PrototypeAssemblerMachine machine) { super(machine); }
        @Override public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }
        @Override public PrototypeAssemblerMachine getMachine() { return (PrototypeAssemblerMachine) super.getMachine(); }

        public int getSuccessChance() {
            return PrototypeAssemblyRules.successChance(accumulatedChance, originalRecipeTier, getMachine().getHatchTier());
        }

        @Override public void onMachineLoad() {
            outputBufferLoaded = false;
            super.onMachineLoad();
        }

        @Override public boolean checkMatchedRecipeAvailable(GTRecipe match) {
            if (recordedRecipe != null || pendingSuccess) return false;
            boolean started = super.checkMatchedRecipeAvailable(match);
            if (started) {
                originalRecipeTier = RecipeHelper.getRecipeEUtTier(match);
                getMachine().markDirty();
            }
            return started;
        }

        @Override public void serverTick() {
            // Own the finish dispatch: the generic Universal output-space gate must not
            // block a failed trial, which produces no outputs, or an already-buffered success.
            if (!isSuspend() && pendingSuccess) { flushSuccess(); return; }
            if (!isSuspend() && !isIdle() && lastRecipe != null) {
                if (!PrototypeAssemblyRules.canRun(originalRecipeTier, getMachine().getHatchTier())) {
                    setWaiting(Component.translatable("gtcsolo.prototype.voltage_required"));
                    return;
                }
                if (progress < duration) handleRecipeWorking();
                if (lastRecipe != null && progress >= duration) onRecipeFinish();
                return;
            }
            super.serverTick();
        }

        @Override public void findAndHandleRecipe() {
            if (pendingSuccess) { flushSuccess(); return; }
            if (recordedRecipe == null) { super.findAndHandleRecipe(); return; }
            lastFailedMatches = null;
            recipeDirty = true; // Keep polling a retained job even without input inventory notifications.
            if (!PrototypeAssemblyRules.canRun(originalRecipeTier, getMachine().getHatchTier())) {
                setWaiting(Component.translatable("gtcsolo.prototype.voltage_required"));
                return;
            }
            // Always modify the recorded ORIGINAL once. Hardware changes may alter OC,
            // but retries must never compound modifiers onto an already modified recipe.
            GTRecipe retry = machine.fullModifyRecipe(recordedRecipe.copy());
            if (retry == null) {
                setWaiting(Component.translatable("gtcsolo.prototype.voltage_required"));
                return;
            }
            retry.inputs.clear();
            retry.tickInputs.keySet().removeIf(cap -> cap != EURecipeCapability.CAP);
            var conditions = retry.checkConditions(this);
            if (!conditions.isSuccess()) { setWaiting(conditions.reason().get()); return; }
            var energy = retry.matchTickRecipe(machine);
            if (!energy.isSuccess()) { setWaiting(energy.reason().get()); return; }
            setupRecipe(retry);
            if (lastRecipe != null) lastOriginRecipe = recordedRecipe;
        }

        @Override public void onRecipeFinish() {
            if (pendingSuccess) { flushSuccess(); return; }
            if (lastRecipe == null) return;
            int chance = getSuccessChance();
            boolean success = chance >= PrototypeAssemblyRules.CERTAIN || chance > 0
                    && getMachine().getLevel().random.nextInt(PrototypeAssemblyRules.CERTAIN) < chance;
            if (success) {
                resultRecipe = lastRecipe.copy();
                prepareSuccessOutputs(resultRecipe);
                pendingSuccess = true;
                // The trial itself is complete now. Only earned output remains buffered.
                recordedRecipe = null;
                originalRecipeTier = -1;
                failures = 0;
                accumulatedChance = 0;
            } else {
                if (recordedRecipe == null) recordedRecipe =
                        (lastOriginRecipe != null ? lastOriginRecipe : lastRecipe).copy();
                failures++;
                accumulatedChance = PrototypeAssemblyRules.afterFailure(accumulatedChance,
                        originalRecipeTier, getMachine().getHatchTier());
            }
            // Exactly once per completed attempt, not once per blocked output tick.
            machine.afterWorking();
            lastRecipe.postWorking(machine);
            clearActiveAttempt();
            getMachine().markDirty();
            if (pendingSuccess && !isSuspend()) flushSuccess();
        }

        private void clearActiveAttempt() {
            boolean suspend = isSuspend() || suspendAfterFinish;
            suspendAfterFinish = false;
            super.resetRecipeLogic();
            setStatus(suspend ? Status.SUSPEND : Status.IDLE);
            recipeDirty = recordedRecipe != null || pendingSuccess;
            updateTickSubscription();
        }

        @Override public void resetRecipeLogic() {
            // Structure invalidation must not erase a paid, locked job or earned output.
            super.resetRecipeLogic();
            if (recordedRecipe == null && !pendingSuccess) originalRecipeTier = -1;
            recipeDirty = recordedRecipe != null || pendingSuccess;
            updateTickSubscription();
        }

        private List<Content> rollOutputs(GTRecipe recipe, RecipeCapability<?> capability) {
            List<Content> result = new ArrayList<>();
            List<Content> chanced = new ArrayList<>();
            for (Content content : recipe.getOutputContents(capability)) {
                if (content.chance >= content.maxChance) result.add(content);
                else chanced.add(content);
            }
            if (!chanced.isEmpty()) {
                int tier = RecipeHelper.getPreOCRecipeEuTier(recipe);
                result.addAll(recipe.getChanceLogicForCapability(capability, IO.OUT, false).roll(chanced,
                        recipe.getType().getChanceFunction(), tier, tier + recipe.ocLevel,
                        chanceCaches.get(capability), recipe.parallels));
            }
            return result;
        }

        private void prepareSuccessOutputs(GTRecipe recipe) {
            pendingOutputs.load(new CompoundTag());
            outputBufferLoaded = true;
            for (Content content : rollOutputs(recipe, ItemRecipeCapability.CAP)) {
                var ingredient = ItemRecipeCapability.CAP.copyContent(content.content);
                ItemStack[] choices = ingredient.getItems();
                if (choices.length == 0 || choices[0].isEmpty()) continue;
                ItemStack stack = choices[0].copy();
                if (ingredient instanceof SizedIngredient sized) stack.setCount(sized.getAmount());
                pendingOutputs.addItem(stack);
            }
            for (Content content : rollOutputs(recipe, FluidRecipeCapability.CAP)) {
                var ingredient = FluidRecipeCapability.CAP.copyContent(content.content);
                FluidStack[] choices = ingredient.getStacks();
                if (choices.length == 0 || choices[0].isEmpty()) continue;
                FluidStack stack = choices[0].copy();
                stack.setAmount(ingredient.getAmount());
                pendingOutputs.addFluid(stack);
            }
            pendingOutputData = pendingOutputs.save();
        }

        private void flushSuccess() {
            if (!outputBufferLoaded) { pendingOutputs.load(pendingOutputData); outputBufferLoaded = true; }
            List<IItemHandlerModifiable> items = new ArrayList<>();
            List<NotifiableFluidTank> fluids = new ArrayList<>();
            Set<IRecipeHandler<?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (IO io : List.of(IO.OUT, IO.BOTH)) {
                for (RecipeCapability<?> cap : List.of(ItemRecipeCapability.CAP, FluidRecipeCapability.CAP)) {
                    var handlers = machine.getCapabilitiesProxy().get(io, cap);
                    if (handlers == null) continue;
                    for (IRecipeHandler<?> handler : handlers) {
                        if (!seen.add(handler)) continue;
                        IItemHandlerModifiable itemHandler = itemOutputStorage(handler);
                        if (itemHandler != null) items.add(itemHandler);
                        if (handler instanceof NotifiableFluidTank fluidHandler) fluids.add(fluidHandler);
                    }
                }
            }
            boolean complete = pendingOutputs.flush(items, fluids);
            if (pendingOutputs.takeChanged()) {
                pendingOutputData = pendingOutputs.save();
                getMachine().markDirty();
            }
            if (!complete) { setWaiting(Component.translatable("gtcsolo.prototype.output_wait")); return; }
            recordedRecipe = null;
            resultRecipe = null;
            originalRecipeTier = -1;
            failures = 0;
            accumulatedChance = 0;
            pendingSuccess = false;
            pendingOutputData = new CompoundTag();
            chanceCaches.clear();
            clearActiveAttempt();
            getMachine().markDirty();
        }
    }
}
