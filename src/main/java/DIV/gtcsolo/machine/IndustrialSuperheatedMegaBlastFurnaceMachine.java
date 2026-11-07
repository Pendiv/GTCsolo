package DIV.gtcsolo.machine;

import DIV.gtcsolo.registry.ModRecipeTypes;
import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GCYMRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A heated main furnace and an independent, mutually serialized smelting lane. */
public class IndustrialSuperheatedMegaBlastFurnaceMachine extends CoilWorkableElectricMultiblockMachine
        implements IDropSaveMachine {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(
            IndustrialSuperheatedMegaBlastFurnaceMachine.class, MANAGED_FIELD_HOLDER);
    private static final String STATE = "gtcsolo_ismbf";
    private static final String LAST_TICK_EU = "gtcsolo_ismbf_last_tick_eu";
    @Persisted @DescSynced private double heat;
    @Persisted @DescSynced private double health = IsmbfRules.MAX_HEALTH;
    @Persisted @DescSynced private boolean overheated;
    @Persisted private double coolantCredit;
    @Persisted private int thermalTicks;
    @Persisted private int pendingCoolingTicks;
    @Persisted @DescSynced private final RecipeLogic smeltingLogic;
    private TickableSubscription thermalSubscription;
    private boolean activeBlocksLit;

    public IndustrialSuperheatedMegaBlastFurnaceMachine(IMachineBlockEntity holder) {
        super(holder);
        smeltingLogic = new FurnaceLogic(this, true);
    }

    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }
    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new FurnaceLogic(this, false); }
    @Override public boolean keepSubscribing() { return true; }

    @Override public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel) thermalSubscription = subscribeServerTick(thermalSubscription, this::thermalTick);
    }

    @Override public void onUnload() {
        if (thermalSubscription != null) { thermalSubscription.unsubscribe(); thermalSubscription = null; }
        smeltingLogic.inValid();
        super.onUnload();
    }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        // Paid superheat survives reloads and re-formation. Normal-mode bonuses remain coil-limited.
        activeBlocksLit = false;
        smeltingLogic.updateTickSubscription();
    }

    @Override public void onStructureInvalid() {
        super.onStructureInvalid();
        smeltingLogic.resetRecipeLogic();
        smeltingLogic.updateTickSubscription();
        // Heat and damage survive disassembly; rebuilding cannot bypass an overheat lock.
    }

    @Override public boolean isActive() {
        return super.isActive() || (smeltingLogic != null && smeltingLogic.isActive());
    }

    @Override public void notifyStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        // One lane becoming idle must not turn off a still-working lane's fireboxes.
        scheduleRenderUpdate();
    }

    @Override public void setWorkingEnabled(boolean enabled) {
        super.setWorkingEnabled(enabled);
    }

    private boolean mainJobPresent() {
        return recipeLogic.getLastRecipe() != null && recipeLogic.getMaxProgress() > 0;
    }

    private boolean extremeJobPresent() {
        return mainJobPresent() && isExtreme(recipeLogic.getLastRecipe());
    }

    private static boolean isExtreme(GTRecipe recipe) {
        return recipe.recipeType == ModRecipeTypes.EXTREME_TEMPERATURE_BLAST_FURNACE;
    }

    private boolean canRun(GTRecipe recipe) {
        if (!isRecipeLogicAvailable() || overheated || heat <= IsmbfRules.AMBIENT) return false;
        if (isExtreme(recipe)) return recipe.data.getInt("ebf_temp") > 0
                && IsmbfRules.canStartExtreme(getCoilType().getCoilTemperature(),
                        recipe.data.getInt(IsmbfRules.START_TEMPERATURE), heat);
        return normalHeat() + 1e-7 >= recipe.data.getInt("ebf_temp");
    }

    private double normalHeat() { return Math.min(heat, getCoilType().getCoilTemperature()); }

    private int heatingTarget() {
        return extremeJobPresent() ? Math.max(getCoilType().getCoilTemperature(),
                recipeLogic.getLastRecipe().data.getInt("ebf_temp")) : getCoilType().getCoilTemperature();
    }

    private void thermalTick() {
        boolean productive = isRecipeLogicAvailable() && isWorkingEnabled() && (recipeLogic.isWorking()
                || (smeltingLogic != null && smeltingLogic.isWorking() && !extremeJobPresent()));
        var energy = energyContainer;
        boolean powered = false;
        if (isFormed() && !overheated) {
            if (energy != null) {
                powered = energy.getInputPerSec() > 0;
            }
        }
        if (heat > IsmbfRules.AMBIENT && (overheated || (!productive && !powered))) {
            pendingCoolingTicks++;
            markDirty();
        }
        if (++thermalTicks >= 40) {
            double oldHeat = heat;
            boolean oldOverheated = overheated;
            heat = IsmbfRules.cool(heat, pendingCoolingTicks);
            pendingCoolingTicks = 0;
            thermalTicks = 0;
            if (isFormed() && !overheated && isWorkingEnabled() && energy != null) {
                int target = heatingTarget();
                long wanted = IsmbfRules.heatingEU(heat, target);
                long used = energy.removeEnergy(Math.min(wanted, energy.getEnergyStored()));
                if (used > 0) heat = IsmbfRules.heatAfterPayment(heat, target, used);
            }
            if (overheated && heat <= IsmbfRules.AMBIENT) overheated = false;
            if (heat != oldHeat || overheated != oldOverheated) markDirty();
        }
        double oldHealth = health;
        if (!overheated && heat <= IsmbfRules.AMBIENT && health < IsmbfRules.MAX_HEALTH)
            health = Math.min(IsmbfRules.MAX_HEALTH, health + IsmbfRules.REPAIR_PER_SECOND / 20);
        // Status callbacks occur before the originating logic updates its status.
        boolean lit = productive && !overheated;
        if (lit != activeBlocksLit) { updateActiveBlocks(lit); activeBlocksLit = lit; }
        if (health != oldHealth) markDirty();
    }

    /** Called only on a successful powered processing tick, including a one-tick recipe. */
    private boolean coolExtreme(GTRecipe recipe) {
        double operatingHeat = recipe.data.getInt("ebf_temp");
        double needed = IsmbfRules.coolingPerTick(operatingHeat);
        double supplied = Math.min(needed, coolantCredit);
        coolantCredit -= supplied;
        needed -= supplied;
        // Use the most effective coolant first. Only accepted fluid input handlers are consulted.
        var coolants = List.of(GTMaterials.Europium.getFluid(1), GTMaterials.PCBCoolant.getFluid(1),
                GTMaterials.DistilledWater.getFluid(1));
        int[] efficiencies = {2000, 10, 1};
        var handlers = capabilitiesProxy.get(IO.IN, FluidRecipeCapability.CAP);
        if (handlers != null) for (int index = 0; index < coolants.size() && needed > 0; index++) {
            int efficiency = efficiencies[index];
            for (var handler : handlers) {
                if (!(handler instanceof NotifiableFluidTank tank)) continue;
                int amount = (int) Math.min(Integer.MAX_VALUE, Math.ceil(needed / efficiency));
                FluidStack request = coolants.get(index).copy();
                request.setAmount(amount);
                int drained = tank.drainInternal(request, IFluidHandler.FluidAction.EXECUTE).getAmount();
                double provided = (double) drained * efficiency;
                double used = Math.min(needed, provided);
                needed -= used;
                coolantCredit += provided - used;
                if (needed <= 0) break;
            }
        }
        health = IsmbfRules.healthAfterCooling(health, needed);
        markDirty();
        if (health > 0) return true;
        overheated = true;
        recipeLogic.interruptRecipe();
        recipeLogic.resetRecipeLogic();
        setWorkingEnabled(false);
        markDirty();
        return false;
    }

    @Override public GTRecipe fullModifyRecipe(GTRecipe recipe) {
        if (!canRun(recipe)) return null;
        boolean smelting = recipe.recipeType == GTRecipeTypes.FURNACE_RECIPES;
        boolean alloy = recipe.recipeType == GCYMRecipeTypes.ALLOY_BLAST_RECIPES;
        boolean extreme = isExtreme(recipe);
        if (!smelting && !alloy && !extreme && recipe.recipeType != GTRecipeTypes.BLAST_RECIPES) return null;
        // GT's identity copy shares Content instances. Detach before native part modifiers
        // and EU replacement, otherwise an OC can mutate the globally registered recipe.
        GTRecipe detached = detachedCopy(recipe);
        detached.data = recipe.data.copy();
        GTRecipe base = super.fullModifyRecipe(detached);
        if (base == null) return null;
        base.data = base.data.copy();
        double convertedPartDiscount = 1;
        if (!smelting && !extreme) {
            long originalPower = RecipeHelper.getInputEUt(recipe);
            long modifiedPower = RecipeHelper.getInputEUt(base);
            if (modifiedPower > 0 && modifiedPower < originalPower) {
                convertedPartDiscount = modifiedPower / (double) originalPower;
                if (alloy) convertedPartDiscount *= convertedPartDiscount;
                EURecipeCapability.putEUContent(base.tickInputs, originalPower);
            }
        }
        if (extreme) {
            // Native aggregation converts amperage into higher effective voltage. Extreme
            // operation is explicitly limited by the highest individual input hatch instead.
            long voltageLimit = energyContainer == null ? 0 : energyContainer.getHighestInputVoltage();
            var oc = IsmbfRules.overclock(RecipeHelper.getInputEUt(base), base.duration,
                    voltageLimit, 0, false, 1);
            if (oc == null) return null;
            base.duration = oc.duration(); base.ocLevel = oc.stages(); base.parallels = 1;
            EURecipeCapability.putEUContent(base.tickInputs, oc.eut());
            return base;
        }
        double effectiveHeat = normalHeat();
        int maximumBonus = smelting ? IsmbfRules.smeltingBonus(effectiveHeat, !mainJobPresent()) : 64;
        int safeBase = safeMultiplier(base);
        if (safeBase < 1) return null;
        GTRecipe capacity = detachedCopy(base);
        capacity.tickInputs.remove(EURecipeCapability.CAP);
        reserveChanceRolls(capacity);
        int bonus = availableBatches(capacity, Math.min(maximumBonus, safeBase));
        if (bonus < 1) return null;
        GTRecipe boosted = scale(base, bonus, false);
        GTRecipe boostedCapacity = scale(capacity, bonus, false);
        // Smelting does not use sub-tick parallelism, so the already checked batch needs no second search.
        int resourceLimit = smelting ? 1 : availableBatches(boostedCapacity, safeMultiplier(boostedCapacity));
        if (resourceLimit < 1) return null;
        double ticks = base.duration;
        int required = base.data.getInt("ebf_temp");
        int perfect = smelting ? 0 : Math.max(0, (int) ((effectiveHeat - required) / 1800));
        if (!smelting) ticks *= convertedPartDiscount * IsmbfRules.convertedDiscount(required, effectiveHeat, alloy)
                * IsmbfRules.durationBonus(effectiveHeat, alloy);
        boolean instantSmelt = smelting && effectiveHeat > 10000;
        if (instantSmelt) ticks = 1;
        var oc = IsmbfRules.overclock(RecipeHelper.getInputEUt(base), ticks, getOverclockVoltage(), perfect,
                !smelting, resourceLimit);
        if (oc == null) return null;
        GTRecipe clocked = scale(boosted, oc.parallels(), false);
        GTRecipe clockedCapacity = scale(boostedCapacity, oc.parallels(), false);
        clocked.duration = oc.duration(); clocked.ocLevel = oc.stages();
        EURecipeCapability.putEUContent(clocked.tickInputs, oc.eut());
        clocked.data.putLong(LAST_TICK_EU, oc.lastTickEU());
        int maxParallel = getParallelHatch().map(hatch -> Math.max(1, hatch.getCurrentParallel())).orElse(1);
        maxParallel = Math.min(maxParallel, safeMultiplier(clockedCapacity));
        maxParallel = (int) Math.min(maxParallel, getOverclockVoltage() / oc.eut());
        int parallel = availableBatches(clockedCapacity, maxParallel);
        if (parallel < 1 || oc.eut() > Long.MAX_VALUE / parallel) return null;
        return scale(clocked, parallel, true);
    }

    private static GTRecipe scale(GTRecipe recipe, int amount, boolean scalePower) {
        if (amount == 1) return detachedCopy(recipe);
        GTRecipe result = ModifierFunction.builder().modifyAllContents(ContentModifier.multiplier(amount))
                .build().apply(recipe);
        // The IO bonus does not multiply power. External hatch parallelism does.
        if (scalePower) EURecipeCapability.putEUContent(result.tickInputs, Math.multiplyExact(RecipeHelper.getInputEUt(recipe), amount));
        if (scalePower && recipe.data.contains(LAST_TICK_EU))
            result.data.putLong(LAST_TICK_EU, Math.multiplyExact(recipe.data.getLong(LAST_TICK_EU), amount));
        result.parallels = (int) Math.min(Integer.MAX_VALUE, (long) recipe.parallels * amount);
        return result;
    }

    /** Select a feasible count without treating the free IO multiplier as extra EU/t. */
    private int availableBatches(GTRecipe recipe, int limit) {
        if (limit < 1) return 0;
        if (batchesFit(recipe, limit)) return limit;
        int low = 0, high = limit - 1;
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            if (batchesFit(recipe, middle)) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private boolean batchesFit(GTRecipe capacity, int count) {
        GTRecipe candidate = scale(capacity, count, false);
        return candidate.matchRecipe(this).isSuccess() && candidate.matchTickRecipe(this).isSuccess();
    }

    /** Copy mutable recipe contents without wrapping random providers in a modifier. */
    private static GTRecipe detachedCopy(GTRecipe recipe) {
        GTRecipe copy = recipe.copy();
        for (var contents : List.of(copy.inputs, copy.outputs, copy.tickInputs, copy.tickOutputs)) {
            for (var entry : contents.entrySet()) {
                var detached = new ArrayList<Content>(entry.getValue().size());
                for (var content : entry.getValue()) {
                    if (entry.getKey() == ItemRecipeCapability.CAP
                            && content.content instanceof SizedIngredient sized
                            && sized.getInner() instanceof IntProviderIngredient) {
                        detached.add(new Content(SizedIngredient.create(SizedIngredient.copy(sized.getInner()), sized.getAmount()),
                                content.chance, content.maxChance, content.tierChanceBoost, content.slotName, content.uiName));
                    } else detached.add(content.copy(entry.getKey()));
                }
                entry.setValue(detached);
            }
        }
        return copy;
    }

    /** Reserve capacity for every possible chance roll without changing the executable recipe. */
    private static void reserveChanceRolls(GTRecipe capacity) {
        for (var contents : List.of(capacity.inputs, capacity.outputs, capacity.tickInputs, capacity.tickOutputs)) {
            for (RecipeCapability<?> capability : List.of(ItemRecipeCapability.CAP, FluidRecipeCapability.CAP)) {
                var old = contents.get(capability);
                if (old == null) continue;
                var reserved = new ArrayList<Content>(old.size());
                for (var content : old) {
                    boolean chanced = content.chance > 0 && content.chance < content.maxChance;
                    var ingredient = capability == ItemRecipeCapability.CAP
                            ? ItemRecipeCapability.CAP.of(content.content) : null;
                    int sizedAmount = 0;
                    if (ingredient instanceof SizedIngredient sized && sized.getInner() instanceof IntProviderIngredient) {
                        sizedAmount = sized.getAmount();
                        ingredient = sized.getInner();
                    }
                    if (ingredient instanceof IntProviderIngredient ranged) {
                        int rolls = chanced ? Math.max(1, capacity.parallels) : 1;
                        int maximum = Math.multiplyExact(Math.max(sizedAmount, ranged.getCountProvider().getMaxValue()), rolls);
                        reserved.add(new Content(SizedIngredient.create(ranged.getInner(), maximum),
                                content.chance == 0 ? 0 : content.maxChance,
                                content.maxChance, 0, content.slotName, content.uiName));
                    } else if (chanced) {
                        reserved.add(new Content(capability.copyContent(content.content,
                                ContentModifier.multiplier(Math.max(1, capacity.parallels))),
                                content.maxChance, content.maxChance, 0, content.slotName, content.uiName));
                    } else reserved.add(content);
                }
                contents.put(capability, reserved);
            }
        }
    }

    private static int safeMultiplier(GTRecipe recipe) {
        int limit = MegaMixerRecipeModifiers.safeMultiplier(recipe);
        for (var contents : List.of(recipe.inputs, recipe.outputs, recipe.tickInputs, recipe.tickOutputs)) {
            for (var content : contents.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
                if (content.chance == 0) continue;
                var ingredient = ItemRecipeCapability.CAP.of(content.content);
                if (!(ingredient instanceof SizedIngredient sized)
                        || !(sized.getInner() instanceof IntProviderIngredient ranged)) continue;
                long maximum = Math.max(sized.getAmount(), ranged.getCountProvider().getMaxValue());
                if (content.chance < content.maxChance) maximum *= Math.max(1, recipe.parallels);
                if (maximum > 0) limit = (int) Math.min(limit, Integer.MAX_VALUE / maximum);
            }
        }
        return limit;
    }

    @Override public void addDisplayText(List<Component> text) {
        super.addDisplayText(text);
        text.add(Component.translatable("gtcsolo.ismbf.display.heat", (long) heat, getCoilType().getCoilTemperature()));
        if (extremeJobPresent()) text.add(Component.translatable("gtcsolo.ismbf.display.heating_target", heatingTarget()));
        text.add(Component.translatable("gtcsolo.ismbf.display.health", (long) health, (long) IsmbfRules.MAX_HEALTH));
        if (overheated) text.add(Component.translatable("gtcsolo.ismbf.display.overheated"));
        text.add(Component.translatable("gtcsolo.ismbf.display.smelting", smeltingLogic.getProgress(), smeltingLogic.getMaxProgress()));
    }

    @Override public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        CompoundTag state = new CompoundTag();
        state.putDouble("heat", heat); state.putDouble("health", health);
        state.putBoolean("overheated", overheated); state.putDouble("coolantCredit", coolantCredit);
        state.putInt("thermalTicks", thermalTicks); state.putInt("pendingCoolingTicks", pendingCoolingTicks);
        tag.put(STATE, state);
    }

    @Override public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (!tag.contains(STATE)) return;
        var state = tag.getCompound(STATE);
        heat = finite(state.getDouble("heat"), 0, Integer.MAX_VALUE);
        health = finite(state.getDouble("health"), 0, IsmbfRules.MAX_HEALTH);
        overheated = state.getBoolean("overheated");
        coolantCredit = finite(state.getDouble("coolantCredit"), 0, 2000);
        thermalTicks = Math.max(0, Math.min(39, state.getInt("thermalTicks")));
        pendingCoolingTicks = Math.max(0, Math.min(thermalTicks, state.getInt("pendingCoolingTicks")));
        if (overheated) setWorkingEnabled(false);
    }

    private void explodeOverheated() {
        if (!(getLevel() instanceof ServerLevel level)) return;
        var pos = getPos();
        level.removeBlock(pos, false);
        var interaction = ConfigHolder.INSTANCE.machines.doesExplosionDamagesTerrain
                ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
        for (int i = 0; i < IsmbfRules.EXPLOSION_COUNT; i++)
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    IsmbfRules.EXPLOSION_STRENGTH, interaction);
    }

    private static double finite(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }

    private static final class FurnaceLogic extends RecipeLogic {
        private final IndustrialSuperheatedMegaBlastFurnaceMachine furnace;
        private final boolean secondary;

        FurnaceLogic(IndustrialSuperheatedMegaBlastFurnaceMachine machine, boolean secondary) {
            super(machine);
            furnace = machine;
            this.secondary = secondary;
        }

        @Override public void serverTick() {
            if (furnace.overheated || !furnace.isRecipeLogicAvailable()) return;
            if (secondary && (!furnace.isWorkingEnabled() || furnace.extremeJobPresent())) return;
            super.serverTick();
        }

        @Override public Iterator<GTRecipe> searchRecipe() {
            if (secondary) return GTRecipeTypes.FURNACE_RECIPES.searchRecipe(furnace);
            if (furnace.getRecipeType() == GTRecipeTypes.FURNACE_RECIPES) return Collections.emptyIterator();
            return super.searchRecipe();
        }

        @Override public void setupRecipe(GTRecipe recipe) {
            if (!furnace.canRun(recipe) || (secondary && furnace.extremeJobPresent())
                    || (!secondary && recipe.recipeType != furnace.getRecipeType())) {
                // Native onRecipeFinish expects setup to reset the completed job. Keeping a
                // completed job here would produce its outputs again on the following tick.
                lastRecipe = null; lastOriginRecipe = null;
                progress = duration = 0; isActive = false;
                markLastRecipeDirty(); setStatus(Status.IDLE);
                return;
            }
            super.setupRecipe(recipe);
        }

        @Override public void handleRecipeWorking() {
            if (lastRecipe == null) return;
            if (!furnace.canRun(lastRecipe)) {
                setWaiting(Component.translatable("gtcsolo.ismbf.display.insufficient_heat")); return;
            }
            if (!secondary && isExtreme(lastRecipe) && !IsmbfRules.canProcessExtreme(
                    furnace.getCoilType().getCoilTemperature(), lastRecipe.data.getInt(IsmbfRules.START_TEMPERATURE),
                    furnace.heat, lastRecipe.data.getInt("ebf_temp"))) {
                setWaiting(Component.translatable("gtcsolo.ismbf.display.insufficient_heat")); return;
            }
            if (!secondary && isExtreme(lastRecipe) && lastRecipe.checkConditions(this).isSuccess()
                    && lastRecipe.matchTickRecipe(furnace).isSuccess() && !furnace.coolExtreme(lastRecipe)) return;
            super.handleRecipeWorking();
        }

        @Override public GTRecipe.ActionResult handleTickRecipe(GTRecipe recipe) {
            // Time is represented in whole ticks. Charge only the remaining fraction of EU
            // on the last tick so converting an EU discount to time does not add a full tick's cost.
            if (progress + 1 >= duration && recipe.data.contains(LAST_TICK_EU)) {
                GTRecipe finalTick = recipe.copy();
                EURecipeCapability.putEUContent(finalTick.tickInputs, recipe.data.getLong(LAST_TICK_EU));
                return super.handleTickRecipe(finalTick);
            }
            return super.handleTickRecipe(recipe);
        }

        @Override public void onRecipeFinish() {
            if (lastRecipe == null) return;
            // Both lanes share the output buses. Recheck immediately before producing either output.
            if (lastRecipe != null && !lastRecipe.matchRecipeContents(IO.OUT, furnace, lastRecipe.outputs, false).isSuccess()) {
                setWaiting(Component.translatable("gtceu.recipe_logic.insufficient_out")); return;
            }
            super.onRecipeFinish();
        }

        @Override public void setWorkingEnabled(boolean enabled) {
            if (!secondary && enabled && furnace.overheated) {
                furnace.explodeOverheated();
                return;
            }
            super.setWorkingEnabled(enabled);
        }

        @Override public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
            CompoundTag cache = new CompoundTag();
            super.saveCustomPersistedData(cache, forDrop);
            tag.put(secondary ? "gtcsolo_ismbf_smelting_cache" : "gtcsolo_ismbf_main_cache", cache);
        }

        @Override public void loadCustomPersistedData(CompoundTag tag) {
            super.loadCustomPersistedData(tag.getCompound(
                    secondary ? "gtcsolo_ismbf_smelting_cache" : "gtcsolo_ismbf_main_cache"));
        }
    }
}
