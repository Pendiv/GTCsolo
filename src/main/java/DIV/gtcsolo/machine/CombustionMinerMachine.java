package DIV.gtcsolo.machine;

import DIV.gtcsolo.registry.CombustionMinerMachines;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CombustionMinerMachine extends WorkableMultiblockMachine implements IDisplayUIMachine {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(CombustionMinerMachine.class,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);
    private static final String TOTAL_EU = "gtcsolo_mining_eu";
    private static final String VEIN_NAME = "gtcsolo_vein_name";
    private static final String TOGGLE = "gtcsolo.miner.toggle";

    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> fuelHandlers = HashBasedTable.create();
    private final IRecipeCapabilityHolder fuelInput = () -> fuelHandlers;

    public CombustionMinerMachine(IMachineBlockEntity holder, Object... args) { super(holder, args); }

    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }
    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new MiningLogic(this); }
    @Override public boolean keepSubscribing() { return true; }

    private IEnergyContainer powerInput() {
        var handlers = getCapabilitiesProxy().get(IO.IN, EURecipeCapability.CAP);
        return handlers != null && handlers.size() == 1 && handlers.get(0) instanceof IEnergyContainer input
                ? input : null;
    }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        fuelHandlers.clear();
        BlockPos fuelPos = getMultiblockState().getMatchContext().get(CombustionMinerMachines.FUEL_HATCH_POSITION);
        for (var part : getParts()) {
            if (!part.self().getPos().equals(fuelPos)) continue;
            List<IRecipeHandler<?>> handlers = new ArrayList<>();
            for (var handler : part.getRecipeHandlers()) {
                if (handler.getCapability() == FluidRecipeCapability.CAP
                        && (handler.getHandlerIO() == IO.IN || handler.getHandlerIO() == IO.BOTH)) handlers.add(handler);
            }
            if (!handlers.isEmpty()) fuelHandlers.put(IO.IN, FluidRecipeCapability.CAP, handlers);
            break;
        }
    }

    @Override public void onStructureInvalid() {
        fuelHandlers.clear();
        super.onStructureInvalid();
    }

    @Override public void onPartUnload() {
        fuelHandlers.clear();
        super.onPartUnload();
    }

    @Override public void onUnload() {
        fuelHandlers.clear();
        super.onUnload();
    }

    private List<GTRecipe> candidateRecipes() {
        var level = getLevel();
        if (level == null || level.isClientSide()) return List.of();
        return level.getRecipeManager().getAllRecipesFor(getRecipeType()).stream()
                .filter(this::accepts).sorted(Comparator.comparing(GTRecipe::getId)).toList();
    }

    private GTRecipe circuitRecipe() {
        for (var recipe : candidateRecipes()) {
            var items = recipe.getInputContents(ItemRecipeCapability.CAP);
            if (recipe.matchRecipeContents(IO.IN, this, Map.of(ItemRecipeCapability.CAP, items), false).isSuccess()) {
                return recipe;
            }
        }
        return null;
    }

    private boolean accepts(GTRecipe recipe) {
        long eut = RecipeHelper.getInputEUt(recipe);
        var outputs = recipe.getOutputContents(ItemRecipeCapability.CAP);
        var oil = singleFluidInput(recipe);
        var circuits = recipe.getInputContents(ItemRecipeCapability.CAP);
        if (circuits.size() != 1 || circuits.get(0).chance != 0 || circuits.get(0).slotName != null) return false;
        var ingredient = ItemRecipeCapability.CAP.of(circuits.get(0).content);
        if (ingredient instanceof SizedIngredient sized) {
            if (sized.getAmount() != 1) return false;
            ingredient = sized.getInner();
        }
        return recipe.getType() == getRecipeType() && eut > 0 && GTUtil.getTierByVoltage(eut) == getDefinition().getTier()
                && recipe.duration > 0 && eut <= Long.MAX_VALUE / 2 / recipe.duration
                && recipe.inputs.size() == 2 && ingredient instanceof IntCircuitIngredient
                && oil != null && oil.getStacks().length == 1 && oil.test(GTMaterials.DrillingFluid.getFluid(1))
                && recipe.tickOutputs.isEmpty()
                && recipe.tickInputs.size() == 1 && recipe.outputs.size() == 1
                && !outputs.isEmpty() && outputs.size() <= 12;
    }

    private static FluidIngredient singleFluidInput(GTRecipe recipe) {
        var fluids = recipe.getInputContents(FluidRecipeCapability.CAP);
        if (fluids.size() != 1) return null;
        var content = fluids.get(0);
        if (content.content instanceof FluidIngredient ingredient && ingredient.getAmount() > 0
                && content.chance > 0 && content.chance == content.maxChance && content.slotName == null) return ingredient;
        return null;
    }

    @Override public GTRecipe fullModifyRecipe(GTRecipe source) {
        if (!accepts(source)) return null;
        var recipe = source.copy();
        recipe.data = source.data.copy();
        recipe.data.putLong(TOTAL_EU, RecipeHelper.getInputEUt(source) * source.duration);
        recipe.tickInputs.remove(EURecipeCapability.CAP);
        return recipe;
    }

    @Override public void addDisplayText(List<Component> lines) {
        var logic = (MiningLogic) recipeLogic;
        MultiblockDisplayText.builder(lines, isFormed())
                .setWorkingStatus(logic.isWorkingEnabled(), logic.isWorking())
                .addWorkingStatusLine().addProgressLineOnlyPercent(logic.getProgressPercent());
        if (!isFormed()) return;
        var recipe = logic.getLastRecipe();
        if (recipe == null) recipe = circuitRecipe();
        Component name = Component.translatable("gtcsolo.miner.select_vein");
        if (recipe != null) {
            String key = recipe.data.getString(VEIN_NAME);
            String id = recipe.getId().toString();
            name = key.isEmpty() ? Component.literal(id) : Component.translatableWithFallback(key, id);
        }
        lines.add(Component.translatable("gtcsolo.miner.vein", name));
        lines.add(ComponentPanelWidget.withButton(Component.translatable(logic.isWorkingEnabled()
                ? "gtcsolo.miner.pause" : "gtcsolo.miner.resume"), TOGGLE));
        if (logic.waitReason != null && logic.isWaiting()) lines.add(logic.waitReason);
    }

    @Override public void handleDisplayClick(String action, ClickData click) {
        var level = getLevel();
        if (click.isRemote || level == null || level.isClientSide() || !isFormed()) return;
        if (TOGGLE.equals(action)) {
            recipeLogic.setWorkingEnabled(!recipeLogic.isWorkingEnabled());
        }
    }

    private static final class MiningLogic extends RecipeLogic {
        private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(MiningLogic.class,
                RecipeLogic.MANAGED_FIELD_HOLDER);
        private final CombustionMinerMachine miner;
        @Persisted private long energy;
        @Persisted private int fuelVolume;
        @Persisted private long fuelEnergy;
        @Persisted private int fuelTicks;
        @Persisted private long fuelEUt;
        private GTRecipe preferredFuel;
        private Component waitReason;

        MiningLogic(CombustionMinerMachine miner) { super(miner); this.miner = miner; }
        @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }

        @Override public void findAndHandleRecipe() {
            lastRecipe = null;
            lastOriginRecipe = null;
            lastFailedMatches = null;
            recipeDirty = false;
            for (var recipe : miner.candidateRecipes()) {
                if (checkMatchedRecipeAvailable(recipe)) break;
            }
        }

        @Override public void setupRecipe(GTRecipe recipe) {
            var power = miner.powerInput();
            if (energy < recipe.data.getLong(TOTAL_EU) && (power == null || power.getEnergyStored() <= 0)) {
                waitFor(Component.translatable("gtceu.multiblock.not_enough_energy"));
                return;
            }
            // The normal recipe input table contains the drilling hatch only.
            super.setupRecipe(recipe);
            if (lastRecipe == recipe && isWorking()) miner.markDirty();
        }

        @Override public void handleRecipeWorking() {
            if (lastRecipe == null) return;
            long cost = lastRecipe.data.getLong(TOTAL_EU);
            if (cost <= 0) { resetRecipeLogic(); return; }
            var conditions = lastRecipe.checkConditions(this);
            if (!conditions.isSuccess()) { waitFor(conditions.reason().get()); return; }
            long drawn = drawPower(cost - energy);
            if (drawn > 0) {
                energy += drawn;
                burnFuel(cost);
                miner.markDirty();
            }
            progress = energy >= cost ? duration : (int) (energy / (cost / duration));
            if (energy < cost && drawn <= 0) {
                waitFor(Component.translatable("gtceu.multiblock.not_enough_energy"));
                return;
            }
            if (!isWorking()) lastRecipe.preWorking(miner);
            setStatus(Status.WORKING);
            if (!miner.onWorking()) { interruptRecipe(); return; }
            totalContinuousRunningTime++;
        }

        private long drawPower(long remaining) {
            if (remaining <= 0) return 0;
            var power = miner.powerInput();
            if (power == null) return 0;
            long cap = 2L * GTValues.V[miner.getDefinition().getTier()];
            long requested = Math.min(remaining, Math.min(cap, power.getEnergyStored()));
            return requested > 0 ? power.removeEnergy(requested) : 0;
        }

        private void burnFuel(long cost) {
            if (energy >= cost) return;
            long voltage = GTValues.V[miner.getDefinition().getTier()];
            // A previously saved batch may contain already-consumed fuel from the old implementation.
            if (fuelEnergy > 0) {
                long generated = Math.min(voltage, fuelEnergy);
                fuelEnergy -= generated;
                energy += generated;
                if (fuelEnergy == 0) fuelVolume = 0;
                return;
            }
            fuelVolume = 0;
            if (fuelTicks <= 0 && !loadFuel()) return;
            energy += fuelEUt;
            if (--fuelTicks == 0) fuelEUt = 0;
        }

        private boolean loadFuel() {
            var fuel = findFuel();
            if (fuel == null) return false;
            long eut = RecipeHelper.getOutputEUt(fuel);
            long voltage = GTValues.V[miner.getDefinition().getTier()];
            int parallel = (int) Math.min(voltage / eut,
                    Integer.MAX_VALUE / singleFluidInput(fuel).getAmount());
            // Match the standard simple generator's fast parallel selection.
            while (parallel > 0) {
                GTRecipe batch = fuel.copy(ContentModifier.multiplier(parallel), false);
                if (batch.matchRecipe(miner.fuelInput).isSuccess()) {
                    if (!batch.handleRecipeIO(IO.IN, miner.fuelInput, chanceCaches)) return false;
                    fuelTicks = fuel.duration;
                    fuelEUt = eut * parallel;
                    return true;
                }
                parallel /= 2;
            }
            return false;
        }

        private GTRecipe findFuel() {
            if (!miner.fuelInput.hasProxies()) return null;
            var manager = getRecipeManager();
            if (preferredFuel != null && manager.byKey(preferredFuel.getId()).orElse(null) == preferredFuel
                    && matchesFuel(preferredFuel)) return preferredFuel;
            for (var recipe : manager.getAllRecipesFor(GTRecipeTypes.COMBUSTION_GENERATOR_FUELS)) {
                if (matchesFuel(recipe)) {
                    preferredFuel = recipe;
                    return recipe;
                }
            }
            return null;
        }

        private boolean matchesFuel(GTRecipe fuel) {
            long eut = RecipeHelper.getOutputEUt(fuel);
            if (fuel.duration <= 0 || eut <= 0 || eut > Long.MAX_VALUE / 2 / fuel.duration
                    || eut > GTValues.V[miner.getDefinition().getTier()]
                    || fuel.inputs.size() != 1 || singleFluidInput(fuel) == null
                    || !fuel.outputs.isEmpty() || !fuel.tickInputs.isEmpty()
                    || fuel.tickOutputs.size() != 1) return false;
            return fuel.checkConditions(this).isSuccess() && fuel.matchRecipe(miner.fuelInput).isSuccess();
        }

        private void waitFor(Component reason) {
            if (isWorking() && lastRecipe != null) lastRecipe.postWorking(miner);
            waitReason = reason;
            setWaiting(reason);
        }

        @Override public void onRecipeFinish() {
            var recipe = lastRecipe;
            if (recipe == null || duration <= 0 || isIdle() || isSuspend()) return;
            long cost = recipe.data.getLong(TOTAL_EU);
            if (cost <= 0 || energy < cost) { resetRecipeLogic(); return; }
            var conditions = recipe.checkConditions(this);
            if (!conditions.isSuccess()) { waitFor(conditions.reason().get()); return; }
            var output = recipe.matchRecipeContents(IO.OUT, miner, recipe.outputs, false);
            if (!output.isSuccess()) { waitFor(output.reason().get()); return; }
            if (!handleRecipeIO(recipe, IO.OUT)) {
                waitFor(Component.translatable("gtceu.recipe_logic.insufficient_out"));
                return;
            }
            energy -= cost;
            miner.afterWorking();
            if (isWorking()) recipe.postWorking(miner);
            lastRecipe = null;
            lastOriginRecipe = null;
            progress = duration = 0;
            isActive = false;
            consecutiveRecipes++;
            setStatus(suspendAfterFinish ? Status.SUSPEND : Status.IDLE);
            suspendAfterFinish = false;
            miner.markDirty();
        }
    }
}
