package DIV.gtcsolo.machine;

import DIV.gtcsolo.machine.starforge.StarForgeOutputBuffer;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.machine.multiblock.part.DataAccessHatchMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.*;

/** Stateful circuit production. The structural aisle is supplied separately by the registration API. */
public class IndustrialCircuitAssemblyLineMachine extends WorkableElectricMultiblockMachine implements IDropSaveMachine {
    private static final String STATE = "gtcsolo_industrial_circuit_line";
    private List<ItemBusPartMachine> baseBuses = List.of(), bonusBuses = List.of();
    private List<DataAccessHatchMachine> dataHatches = List.of();
    private int repeatCount;
    private Set<BlockPos> extraFluidPositions = Set.of();
    private com.gregtechceu.gtceu.api.pattern.BlockPattern controllerPattern;
    private TickableSubscription historySubscription;
    private long lastClock = Long.MIN_VALUE, lastProductionTick = Long.MIN_VALUE;
    private final IndustrialCircuitHistory history = new IndustrialCircuitHistory();
    private final CircuitResearchMedia researchMedia = new CircuitResearchMedia();
    private final Map<String, ResidentData> resident = new LinkedHashMap<>();
    private final Deque<ItemStack> returnedMedia = new ArrayDeque<>();
    private final StarForgeOutputBuffer goods = new StarForgeOutputBuffer();
    private ItemStack analysing = ItemStack.EMPTY;
    private String analysisKey = "";
    private Set<String> analysisRecipes = Set.of();
    private long analysisTotal, analysisRemaining, analysisRate;
    private long euPerCWU = IndustrialCircuitResearch.DEFAULT_EU_PER_CWU;

    public IndustrialCircuitAssemblyLineMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        if (args.length > 0 && args[0] instanceof Number factor && factor.longValue() > 0) euPerCWU = factor.longValue();
    }

    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new CircuitLogic(this); }
    @Override public CircuitLogic getRecipeLogic() { return (CircuitLogic) super.getRecipeLogic(); }
    @Override public boolean keepSubscribing() { return true; }
    @Override public com.gregtechceu.gtceu.api.pattern.BlockPattern getPattern() {
        if (controllerPattern == null) controllerPattern =
                DIV.gtcsolo.registry.IndustrialCircuitAssemblyLines.newControllerPattern(getDefinition());
        return controllerPattern;
    }
    @Override public boolean checkPattern() {
        // A failed attempt at the next repeated slice can leave role markers in GT's
        // context. Keep the final predicates so the end cap can supersede those markers.
        return getPattern().checkPatternAt(getMultiblockState(), true);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel) historySubscription = subscribeServerTick(historySubscription, this::clockHistory);
    }

    @Override public void onUnload() {
        researchMedia.clear();
        if (historySubscription != null) { historySubscription.unsubscribe(); historySubscription = null; }
        super.onUnload();
    }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        Set<BlockPos> base = matched(IndustrialCircuitLinePattern.BASE);
        Set<BlockPos> extra = matched(IndustrialCircuitLinePattern.BONUS);
        // Definition patterns are normally memoized/shared. This controller has its own
        // pattern, so its exact validated repetition count cannot be overwritten by another core.
        repeatCount = 0;
        var pattern = getPattern();
        for (int index = 0; index < pattern.aisleRepetitions.length; index++) {
            if (pattern.aisleRepetitions[index][1] > 1) {
                if (repeatCount != 0) { onStructureInvalid(); return; }
                repeatCount = pattern.getFormedRepetitionCount()[index];
            }
        }
        if (repeatCount < 1 || repeatCount > 6 || base.isEmpty() || !Collections.disjoint(base, extra)) {
            onStructureInvalid();
            return;
        }
        var sorter = pattern.structureDir[2].getSorter(getFrontFacing(), getUpwardsFacing(), isFlipped());
        baseBuses = buses(base).stream().sorted(Comparator.comparing(bus -> sorter.apply(bus.getPos()))).toList();
        bonusBuses = buses(extra).stream().sorted(Comparator.comparing(bus -> sorter.apply(bus.getPos()))).toList();
        extraFluidPositions = new HashSet<>(matched(IndustrialCircuitLinePattern.BONUS_FLUID));
        extraFluidPositions.addAll(extra);
        dataHatches = getParts().stream().map(part -> part.self()).filter(DataAccessHatchMachine.class::isInstance)
                .map(DataAccessHatchMachine.class::cast).filter(hatch -> !hatch.isCreative()).toList();
        if (baseBuses.size() != base.size() || bonusBuses.size() != extra.size() || bonusBuses.size() > repeatCount) onStructureInvalid();
        else researchMedia.bind(dataHatches);
    }

    @SuppressWarnings("unchecked")
    private Set<BlockPos> matched(String key) {
        Map<BlockPos, com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate> roles =
                getMultiblockState().getMatchContext().getOrDefault(key, Collections.emptyMap());
        Map<BlockPos, com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate> accepted =
                getMultiblockState().getMatchContext().getOrDefault("predicates", Collections.emptyMap());
        Set<BlockPos> result = new LinkedHashSet<>();
        roles.forEach((pos, predicate) -> { if (accepted.get(pos) == predicate) result.add(pos); });
        return result;
    }

    private List<ItemBusPartMachine> buses(Set<BlockPos> positions) {
        List<ItemBusPartMachine> result = new ArrayList<>();
        for (BlockPos pos : positions) if (MetaMachine.getMachine(getLevel(), pos) instanceof ItemBusPartMachine bus) result.add(bus);
        return result;
    }

    @Override public void onStructureInvalid() {
        researchMedia.clear();
        super.onStructureInvalid();
        baseBuses = bonusBuses = List.of();
        dataHatches = List.of();
        repeatCount = 0;
        extraFluidPositions = Set.of();
    }

    /** Strip only the native research-hatch check; the core's physically held media remain authoritative. */
    @Override public GTRecipe fullModifyRecipe(GTRecipe recipe) {
        if (!isFormed() || repeatCount < 1) return null;
        if (!isLine(recipe) && recipe.recipeType != com.gregtechceu.gtceu.common.data.GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES) return null;
        if (!supported(recipe)) return null;
        GTRecipe detached = recipe.copy();
        detached.data = recipe.data.copy();
        if (isLine(recipe)) {
            if (!unlocked(recipe)) return null;
            detached.conditions.removeIf(ResearchCondition.class::isInstance);
            GTRecipe result = super.fullModifyRecipe(detached);
            if (result == null || !supported(result) || !CircuitLogic.outputNumbersFit(result, 1)) return null;
            if (result.conditions.stream().anyMatch(ResearchCondition.class::isInstance) && !unlocked(result)) return null;
            if (RecipeHelper.getInputEUt(result) > getOverclockVoltage()) return null;
            result.ocLevel = 0;
            result.parallels = 1;
            return result;
        }
        // UHV and higher circuits must be authored in the researched circuit-line type.
        for (Content content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                for (String tier : List.of("uhv", "uev", "uiv", "uxv", "opv", "max")) {
                    if (stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                            new net.minecraft.resources.ResourceLocation("gtceu", "circuits/" + tier)))) return null;
                }
            }
        }
        GTRecipe result = super.fullModifyRecipe(detached);
        return result != null && supported(result) ? ordinaryModify(result) : null;
    }

    private GTRecipe ordinaryModify(GTRecipe recipe) {
        var oc = IndustrialCircuitRules.overclock(RecipeHelper.getInputEUt(recipe), recipe.duration, getOverclockVoltage());
        if (oc == null) return null;
        int bonus = IndustrialCircuitRules.ordinaryBonus(repeatCount, oc.stages());
        if (!CircuitLogic.outputNumbersFit(recipe, bonus) || !inputNumbersFit(recipe, bonus)) return null;
        GTRecipe overclocked = recipe.copy();
        RecipeHelper.setInputEUt(overclocked, oc.eut());
        overclocked.ocLevel = oc.stages();
        overclocked.duration = historyDuration(recipe, oc.duration());
        GTRecipe boosted = com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(bonus)).parallels(bonus).build().apply(overclocked);
        int parallel = com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic.getParallelAmount(this, boosted,
                getParallelHatch().map(hatch -> Math.max(1, hatch.getCurrentParallel())).orElse(1));
        if (parallel <= 0 || (long) bonus * parallel > Integer.MAX_VALUE
                || !CircuitLogic.outputNumbersFit(recipe, bonus * parallel) || !inputNumbersFit(recipe, bonus * parallel)) return null;
        try {
            GTRecipe result = com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction.builder()
                    .modifyAllContents(ContentModifier.multiplier(parallel)).parallels(parallel).build().apply(boosted);
            RecipeHelper.setInputEUt(result, Math.multiplyExact(oc.eut(), parallel));
            result.data.putInt("gtcsolo_ical_power_parallel", parallel);
            return result;
        } catch (ArithmeticException overflow) { return null; }
    }

    private static boolean inputNumbersFit(GTRecipe recipe, int multiplier) {
        for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            if (content.chance == 0) continue;
            var ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (ingredient instanceof com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient) return false;
            var choices = ingredient.getItems();
            int amount = ingredient instanceof SizedIngredient sized ? sized.getAmount() : choices.length == 0 ? 0 : choices[0].getCount();
            if (amount <= 0 || (long) amount * multiplier > Integer.MAX_VALUE) return false;
        }
        for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
            if (content.chance != 0 && (long) FluidRecipeCapability.CAP.of(content.content).getAmount() * multiplier > Integer.MAX_VALUE) return false;
        }
        return true;
    }

    private static boolean isLine(GTRecipe recipe) { return recipe.recipeType == ModRecipeTypes.CIRCUIT_ASSEMBLY_LINE; }

    private boolean unlocked(GTRecipe recipe) {
        if (recipe.conditions.stream().anyMatch(condition -> condition instanceof ResearchCondition research && research.isReverse())) return false;
        String recipeKey = IndustrialCircuitResearch.recipeKey(recipe);
        if (IndustrialCircuitResearch.canAnalyse(recipe)
                && resident.values().stream().anyMatch(data -> data.recipes.contains(recipeKey))) return true;
        Set<String> required = IndustrialCircuitResearch.requirements(recipe);
        if (required.isEmpty()) return false;
        // The index is invalidated by hatch changes, structure changes and data reloads.
        return researchMedia.unlocks(recipe);
    }

    private long historyCount(GTRecipe candidate) {
        long count = history.maximumCount();
        for (var entry : history.entries()) count = Math.max(count, researchMedia.count(entry.recipe()));
        if (candidate != null) count = Math.max(count, researchMedia.count(IndustrialCircuitResearch.recipeKey(candidate)));
        return count;
    }

    private int historyDuration(GTRecipe recipe, int duration) {
        return IndustrialCircuitHistory.processingDuration(duration, historyCount(recipe));
    }

    private static boolean supported(GTRecipe recipe) {
        return recipe.inputs.keySet().stream().allMatch(cap -> cap == ItemRecipeCapability.CAP || cap == FluidRecipeCapability.CAP)
                && recipe.outputs.keySet().stream().allMatch(cap -> cap == ItemRecipeCapability.CAP || cap == FluidRecipeCapability.CAP)
                && recipe.tickInputs.keySet().stream().allMatch(cap -> cap == EURecipeCapability.CAP)
                && recipe.tickOutputs.isEmpty();
    }

    private IndustrialCircuitInputs.Bus inputBus(ItemBusPartMachine bus) {
        return new IndustrialCircuitInputs.Bus(bus.getInventory().storage, bus.getCircuitInventory().storage);
    }

    private List<com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank> inputFluids() { return inputFluids(false); }
    private List<com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank> inputFluids(boolean extra) {
        List<com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank> result = new ArrayList<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IO io : List.of(IO.IN, IO.BOTH)) {
            var handlers = getCapabilitiesProxy().get(io, FluidRecipeCapability.CAP);
            if (handlers != null) for (var handler : handlers) if (handler instanceof NotifiableFluidTank tank) {
                if (extraFluidPositions.contains(tank.getMachine().getPos()) != extra) continue;
                for (var storage : tank.getStorages()) if (seen.add(storage)) result.add(storage);
            }
        }
        return result;
    }

    private IRecipeCapabilityHolder baseHolder() {
        Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> table = HashBasedTable.create();
        getCapabilitiesProxy().cellSet().forEach(cell -> table.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
        List<IRecipeHandler<?>> handlers = new ArrayList<>();
        for (ItemBusPartMachine bus : baseBuses) {
            handlers.add(bus.getInventory());
            if (bus.getCircuitInventory().getSlots() > 0) handlers.add(bus.getCircuitInventory());
        }
        table.put(IO.IN, ItemRecipeCapability.CAP, handlers);
        table.remove(IO.BOTH, ItemRecipeCapability.CAP);
        return () -> table;
    }

    private List<IItemHandlerModifiable> outputs() {
        List<IItemHandlerModifiable> result = new ArrayList<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IO io : List.of(IO.OUT, IO.BOTH)) {
            var handlers = getCapabilitiesProxy().get(io, ItemRecipeCapability.CAP);
            if (handlers == null) continue;
            for (var handler : handlers) {
                if (handler instanceof com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait<?> trait
                        && trait.getMachine() instanceof DataAccessHatchMachine) continue;
                IItemHandlerModifiable storage = PrototypeAssemblerMachine.itemOutputStorage(handler);
                if (storage != null && seen.add(storage)) result.add(storage);
            }
        }
        return result;
    }

    private List<NotifiableFluidTank> fluidOutputs() {
        List<NotifiableFluidTank> result = new ArrayList<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IO io : List.of(IO.OUT, IO.BOTH)) {
            var handlers = getCapabilitiesProxy().get(io, FluidRecipeCapability.CAP);
            if (handlers != null) for (var handler : handlers) if (handler instanceof NotifiableFluidTank tank && seen.add(tank)) result.add(tank);
        }
        return result;
    }

    private boolean flush() {
        if (!isFormed()) return false;
        var output = outputs();
        boolean changed = false;
        while (!returnedMedia.isEmpty()) {
            ItemStack remainder = returnedMedia.peekFirst();
            for (var inventory : output) for (int slot = 0; slot < inventory.getSlots() && !remainder.isEmpty(); slot++) {
                int before = remainder.getCount();
                remainder = inventory.insertItem(slot, remainder, false);
                changed |= before != remainder.getCount();
            }
            returnedMedia.removeFirst();
            if (!remainder.isEmpty()) { returnedMedia.addFirst(remainder); break; }
        }
        boolean complete = goods.flush(output, fluidOutputs());
        boolean goodsChanged = goods.takeChanged();
        if (changed || goodsChanged) markDirty();
        return complete && returnedMedia.isEmpty();
    }

    private void clockHistory() {
        if (!(getLevel() instanceof ServerLevel)) return;
        long now = getLevel().getGameTime();
        if (lastClock != Long.MIN_VALUE && now > lastClock) {
            long elapsed = now - lastClock;
            long idle = elapsed == 1 && lastProductionTick >= lastClock ? 0
                    : elapsed - (lastProductionTick >= lastClock && lastProductionTick < now ? 1 : 0);
            if (idle > 0 && history.maximumCount() > 0) { history.elapseIdle(idle); markDirty(); }
        }
        releaseUnusedData();
        lastClock = now;
    }

    private void releaseUnusedData() {
        boolean producing = getRecipeLogic().paidRecipe != null && getRecipeLogic().isWorking()
                && lastProductionTick >= getLevel().getGameTime() - 1;
        String active = !producing ? "" : IndustrialCircuitResearch.recipeKey(getRecipeLogic().paidRecipe);
        for (var iterator = resident.entrySet().iterator(); iterator.hasNext();) {
            ResidentData data = iterator.next().getValue();
            boolean reserved = data.fresh && history.entries().stream().anyMatch(record -> data.recipes.contains(record.recipe()));
            if (reserved || data.recipes.contains(active) || data.recipes.stream().anyMatch(history::containsPositive)) continue;
            returnedMedia.addLast(data.media.copy());
            iterator.remove();
            markDirty();
        }
    }

    private boolean beginAnalysis() {
        if (!analysing.isEmpty() || getOffsetTimer() % 20 != 0) return !analysing.isEmpty();
        for (var part : getParts()) {
          if (!(part.self() instanceof ItemBusPartMachine bus) || bus.getInventory().getHandlerIO() != IO.IN) continue;
          for (int slot = 0; slot < bus.getInventory().getSlots(); slot++) {
            ItemStack source = bus.getInventory().storage.getStackInSlot(slot);
            var identity = com.gregtechceu.gtceu.utils.ResearchManager.readResearchId(source);
            if (identity == null || identity.getFirst() != com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES
                    || resident.containsKey(identity.getFirst().registryName + "#" + identity.getSecond())) continue;
            var specification = IndustrialCircuitResearch.resolve(getLevel(), source, ModRecipeTypes.CIRCUIT_ASSEMBLY_LINE, euPerCWU);
            if (specification == null) continue;
            if (resident.values().stream().anyMatch(data -> !Collections.disjoint(data.recipes, specification.recipes()))) continue;
            ItemStack reserved = bus.getInventory().storage.extractItem(slot, 1, false);
            if (reserved.isEmpty()) continue;
            analysing = reserved;
            analysisKey = specification.key();
            analysisRecipes = specification.recipes();
            analysisRemaining = analysisTotal = specification.totalEU();
            analysisRate = specification.euPerTick();
            markDirty();
            return true;
          }
        }
        return false;
    }

    private void analyseTick() {
        long needed = Math.min(analysisRate, analysisRemaining);
        var energy = getEnergyContainer();
        if (needed <= 0 || energy.getEnergyStored() < needed) {
            getRecipeLogic().setWaiting(Component.translatable("gtcsolo.circuit_line.analysis_power", needed));
            return;
        }
        energy.changeEnergy(-needed);
        analysisRemaining -= needed;
        getRecipeLogic().setStatus(RecipeLogic.Status.WORKING);
        if (analysisRemaining == 0) {
            installAnalysedData();
            analysing = ItemStack.EMPTY;
            analysisKey = "";
            analysisRecipes = Set.of();
            getRecipeLogic().setStatus(RecipeLogic.Status.IDLE);
        }
        markDirty();
    }

    private void installAnalysedData() {
        // Recheck persisted/in-flight analysis after a recipe reload or an upgrade from the old unrestricted mode.
        Map<String, Integer> eligible = new LinkedHashMap<>();
        for (var recipe : getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.CIRCUIT_ASSEMBLY_LINE)) {
            String key = IndustrialCircuitResearch.recipeKey(recipe);
            if (analysisRecipes.contains(key) && IndustrialCircuitResearch.canAnalyse(recipe)) eligible.put(key, recipe.duration);
        }
        if (eligible.isEmpty()) { returnedMedia.addLast(analysing.copy()); return; }
        analysisRecipes = Set.copyOf(eligible.keySet());
        while (resident.size() >= 2) {
            String oldest = resident.keySet().iterator().next();
            ResidentData retired = resident.remove(oldest);
            history.forget(retired.recipes);
            returnedMedia.addLast(retired.media.copy());
        }
        String initial = analysisRecipes.stream().sorted().findFirst().orElseThrow();
        int initialDuration = eligible.get(initial);
        history.reserve(initial, initialDuration);
        resident.put(analysisKey, new ResidentData(analysing, analysisRecipes, true));
        releaseUnusedData();
    }

    private void recordDataUse(String recipe) {
        for (String key : new ArrayList<>(resident.keySet())) {
            ResidentData data = resident.get(key);
            if (data.recipes.contains(recipe)) { data.fresh = false; resident.remove(key); resident.put(key, data); }
        }
    }

    @Override public void addDisplayText(List<Component> text) {
        super.addDisplayText(text);
        var logic = getRecipeLogic();
        boolean ordinary = getRecipeType() == com.gregtechceu.gtceu.common.data.GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES;
        text.removeIf(component -> component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                && (contents.getKey().equals("gtceu.multiblock.parallel") || contents.getKey().equals("gtceu.multiblock.parallel.exact")));
        if (ordinary && !logic.paidSpecial) {
            int parallel = logic.paidRecipe == null ? getParallelHatch().map(hatch -> hatch.getCurrentParallel()).orElse(1)
                    : Math.max(1, logic.paidRecipe.data.getInt("gtcsolo_ical_power_parallel"));
            com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText.builder(text, isFormed())
                    .addParallelsLine(parallel, logic.paidRecipe != null);
            if (logic.paidRecipe != null) text.add(Component.translatable("gtcsolo.ical.ordinary_progress",
                    logic.paidRecipe.ocLevel, logic.paidRecipe.parallels));
        }
        if (logic.paidSpecial && logic.paidRecipe != null) {
            text.removeIf(component -> component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                    && contents.getKey().startsWith("gtceu.multiblock.output_line."));
            com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText.builder(text, isFormed())
                    .setWorkingStatus(logic.isWorkingEnabled(), logic.isActive()).addOutputLines(logic.outputRecipe());
        }
        text.add(Component.translatable("gtcsolo.circuit_line.repeats", repeatCount, repeatCount * 2));
        long learnedCount = historyCount(logic.paidRecipe);
        text.add(Component.translatable("gtcsolo.circuit_line.history", learnedCount, IndustrialCircuitHistory.discountPercent(learnedCount)));
        for (var entry : history.entries()) text.add(Component.translatable("gtcsolo.circuit_line.record", entry.recipe(),
                Math.max(entry.count(), researchMedia.count(entry.recipe())), entry.duration()));
        if (!analysing.isEmpty()) text.add(Component.translatable("gtcsolo.circuit_line.analysis", analysisTotal - analysisRemaining, analysisTotal, analysisRate));
        text.add(Component.translatable("gtcsolo.circuit_line.data", resident.size(), returnedMedia.size()));
        if (getRecipeLogic().paidSpecial) text.add(Component.translatable("gtcsolo.ical.extra_progress",
                getRecipeLogic().paidExtras, getRecipeLogic().paidExtraLimit));
    }

    @Override public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        CompoundTag state = new CompoundTag();
        ListTag records = new ListTag();
        for (var entry : history.entries()) {
            CompoundTag record = new CompoundTag();
            record.putString("recipe", entry.recipe()); record.putLong("count", entry.count());
            record.putInt("duration", entry.duration()); record.putLong("idle", entry.idleTicks()); records.add(record);
        }
        state.put("history", records);
        state.putInt("mode", getActiveRecipeType());
        state.putBoolean("enabled", getRecipeLogic().isWorkingEnabled());
        state.putLong("clock", lastClock); state.putLong("productive", lastProductionTick);
        ListTag media = new ListTag();
        resident.forEach((key, data) -> {
            CompoundTag item = new CompoundTag(); item.putString("key", key); item.put("media", data.media.save(new CompoundTag()));
            item.putBoolean("fresh", data.fresh); writeRecipes(item, data.recipes); media.add(item);
        });
        state.put("resident", media);
        ListTag returning = new ListTag();
        returnedMedia.forEach(stack -> returning.add(stack.save(new CompoundTag())));
        state.put("returning", returning); state.put("goods", goods.save());
        state.put("analysing", analysing.save(new CompoundTag())); state.putString("analysisKey", analysisKey);
        state.putLong("analysisTotal", analysisTotal); state.putLong("analysisRemaining", analysisRemaining);
        state.putLong("analysisRate", analysisRate); writeRecipes(state, analysisRecipes);
        getRecipeLogic().savePaidJob(state);
        tag.put(STATE, state);
    }

    @Override public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (!tag.contains(STATE, Tag.TAG_COMPOUND)) return;
        CompoundTag state = tag.getCompound(STATE);
        List<IndustrialCircuitHistory.Entry> records = new ArrayList<>();
        for (var value : state.getList("history", Tag.TAG_COMPOUND)) {
            CompoundTag record = (CompoundTag) value;
            records.add(new IndustrialCircuitHistory.Entry(record.getString("recipe"), record.getLong("count"),
                    record.getInt("duration"), record.getLong("idle")));
        }
        history.restore(records); lastClock = state.getLong("clock"); lastProductionTick = state.getLong("productive");
        resident.clear(); returnedMedia.clear();
        for (var value : state.getList("resident", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) value;
            resident.put(item.getString("key"), new ResidentData(ItemStack.of(item.getCompound("media")), readRecipes(item), item.getBoolean("fresh")));
        }
        for (var value : state.getList("returning", Tag.TAG_COMPOUND)) returnedMedia.addLast(ItemStack.of((CompoundTag) value));
        goods.load(state.getCompound("goods")); analysing = ItemStack.of(state.getCompound("analysing"));
        analysisKey = state.getString("analysisKey"); analysisRecipes = readRecipes(state);
        analysisTotal = state.getLong("analysisTotal"); analysisRemaining = state.getLong("analysisRemaining"); analysisRate = state.getLong("analysisRate");
        getRecipeLogic().loadPaidJob(state);
        setActiveRecipeType(Math.max(0, Math.min(getRecipeTypes().length - 1, state.getInt("mode"))));
        getRecipeLogic().setWorkingEnabled(state.getBoolean("enabled"));
    }

    private static void writeRecipes(CompoundTag tag, Set<String> values) {
        ListTag list = new ListTag(); values.forEach(value -> list.add(net.minecraft.nbt.StringTag.valueOf(value))); tag.put("recipes", list);
    }
    private static Set<String> readRecipes(CompoundTag tag) {
        Set<String> result = new LinkedHashSet<>();
        var list = tag.getList("recipes", Tag.TAG_STRING); for (int index = 0; index < list.size(); index++) result.add(list.getString(index));
        return Set.copyOf(result);
    }
    private static final class ResidentData {
        final ItemStack media; final Set<String> recipes; boolean fresh;
        ResidentData(ItemStack media, Set<String> recipes, boolean fresh) { this.media = media.copy(); this.recipes = Set.copyOf(recipes); this.fresh = fresh; }
    }

    public static class CircuitLogic extends RecipeLogic {
        private GTRecipe paidRecipe;
        private int paidProgress, paidDuration;
        private boolean paidSpecial;
        private int paidExtras, paidExtraLimit;
        private String previousRollRecipe = "";

        public CircuitLogic(IndustrialCircuitAssemblyLineMachine machine) { super(machine); }
        @Override public IndustrialCircuitAssemblyLineMachine getMachine() { return (IndustrialCircuitAssemblyLineMachine) super.getMachine(); }
        @Override public double getProgressPercent() {
            var core = getMachine();
            return !core.analysing.isEmpty() && core.analysisTotal > 0
                    ? (core.analysisTotal - core.analysisRemaining) / (double) core.analysisTotal : super.getProgressPercent();
        }

        @Override public Iterator<GTRecipe> searchRecipe() {
            if (machine.getRecipeType() == ModRecipeTypes.CIRCUIT_DATA_ANALYSIS) return Collections.emptyIterator();
            return machine.getRecipeType().searchRecipe(machine.getRecipeType() == ModRecipeTypes.CIRCUIT_ASSEMBLY_LINE
                    ? getMachine().baseHolder() : machine);
        }

        @Override public boolean checkMatchedRecipeAvailable(GTRecipe match) {
            var core = getMachine();
            if (paidRecipe != null || !core.goods.isEmpty() || !core.analysing.isEmpty()) return false;
            if (!isLine(match)) return super.checkMatchedRecipeAvailable(match);
            GTRecipe modified = machine.fullModifyRecipe(match.copy());
            if (modified == null || !modified.checkConditions(this).isSuccess() || !modified.matchTickRecipe(machine).isSuccess()) return false;
            var plan = IndustrialCircuitInputs.plan(modified, core.baseBuses.stream().map(core::inputBus).toList(),
                    List.of(), core.inputFluids(), 0);
            if (plan == null) return false;
            GTRecipe batch = modified.copy();
            if (!outputNumbersFit(modified, 1 + IndustrialCircuitRules.extraLimit(core.repeatCount))) return false;
            batch.parallels = 1;
            batch.ocLevel = 0;
            batch.data.putInt("gtcsolo_ical_power_parallel", 1);
            batch.duration = core.historyDuration(match, modified.duration);
            if (!supported(batch) || !batch.matchRecipeContents(IO.OUT, machine, batch.outputs, false).isSuccess()) return false;
            if (!machine.beforeWorking(batch)) return false;
            if (!plan.commit()) return false;
            if (!IndustrialCircuitResearch.recipeKey(match).equals(previousRollRecipe)) chanceCaches.clear();
            previousRollRecipe = IndustrialCircuitResearch.recipeKey(match);
            batch.preWorking(machine);
            paidRecipe = lastRecipe = batch; paidProgress = progress = 0; paidDuration = duration = batch.duration;
            paidSpecial = true; lastOriginRecipe = match; recipeDirty = false; isActive = true;
            paidExtras = 0; paidExtraLimit = IndustrialCircuitRules.extraLimit(core.repeatCount);
            core.lastProductionTick = core.getLevel().getGameTime();
            String key = IndustrialCircuitResearch.recipeKey(match);
            core.recordDataUse(key);
            setStatus(Status.WORKING); core.markDirty();
            return true;
        }

        @Override public void setupRecipe(GTRecipe recipe) {
            super.setupRecipe(recipe);
            if (lastRecipe != null && isWorking()) {
                String key = IndustrialCircuitResearch.recipeKey(recipe);
                if (!key.equals(previousRollRecipe)) chanceCaches.clear();
                previousRollRecipe = key;
                paidRecipe = lastRecipe.copy(); paidProgress = progress; paidDuration = duration; paidSpecial = isLine(recipe);
                getMachine().lastProductionTick = getMachine().getLevel().getGameTime();
                getMachine().markDirty();
            }
        }

        @Override public void serverTick() {
            var core = getMachine(); core.clockHistory();
            if (isSuspend() || !machine.isRecipeLogicAvailable()) return;
            if (paidRecipe != null) {
                if (lastRecipe == null) { lastRecipe = paidRecipe; progress = paidProgress; duration = paidDuration; setStatus(Status.WORKING); isActive = true; }
                int before = progress;
                if (paidSpecial && progress < duration) acceptExtraInputs();
                if (progress < duration) handleRecipeWorking();
                paidProgress = progress;
                if (progress > before) core.lastProductionTick = core.getLevel().getGameTime();
                if (lastRecipe != null && progress >= duration) onRecipeFinish();
                core.markDirty(); return;
            }
            if (!core.flush()) { setWaiting(Component.translatable("gtcsolo.circuit_line.output_wait")); return; }
            if (!core.analysing.isEmpty() || core.beginAnalysis()) {
                isActive = true;
                core.analyseTick();
                isActive = !core.analysing.isEmpty();
                return;
            }
            // No retained job: native lookup/modification remains available for the ordinary assembler mode.
            if (lastRecipe == null && !isIdle()) setStatus(Status.IDLE);
            recipeDirty = true;
            super.serverTick();
        }

        private void acceptExtraInputs() {
            var core = getMachine();
            for (var bus : core.bonusBuses) {
                while (paidExtras < paidExtraLimit) {
                    var plan = IndustrialCircuitInputs.extraPlan(paidRecipe, core.inputBus(bus), core.inputFluids(true));
                    if (plan == null || !plan.commit()) break;
                    paidExtras++;
                    core.markDirty();
                }
                if (paidExtras >= paidExtraLimit) break;
            }
        }

        /** Read-only output projection shared by the GUI and the single completion path. */
        private GTRecipe outputRecipe() {
            if (!paidSpecial || paidExtras == 0) return paidRecipe;
            GTRecipe projected = paidRecipe.copy();
            int multiplier = 1 + paidExtras;
            projected.outputs.replaceAll((capability, contents) -> contents.stream()
                    .map(content -> content.copy(capability, ContentModifier.multiplier(multiplier))).toList());
            projected.parallels = multiplier;
            return projected;
        }

        @Override public void onRecipeFinish() {
            if (paidRecipe == null) return;
            var core = getMachine(); GTRecipe finished = outputRecipe();
            for (Content content : roll(finished, ItemRecipeCapability.CAP)) {
                var ingredient = ItemRecipeCapability.CAP.copyContent(content.content); ItemStack[] choices = ingredient.getItems();
                if (choices.length > 0 && !choices[0].isEmpty()) core.goods.addItem(choices[0].copyWithCount(
                        ingredient instanceof SizedIngredient sized ? sized.getAmount() : choices[0].getCount()));
            }
            for (Content content : roll(finished, FluidRecipeCapability.CAP)) {
                var ingredient = FluidRecipeCapability.CAP.copyContent(content.content); var choices = ingredient.getStacks();
                if (choices.length > 0 && !choices[0].isEmpty()) { var stack = choices[0].copy(); stack.setAmount(ingredient.getAmount()); core.goods.addFluid(stack); }
            }
            {
                String key = IndustrialCircuitResearch.recipeKey(finished);
                core.history.complete(key, paidDuration);
                core.researchMedia.complete(key);
                core.recordDataUse(key);
                core.lastProductionTick = core.getLevel().getGameTime();
            }
            paidRecipe = null; paidProgress = paidDuration = 0; paidSpecial = false;
            paidExtras = paidExtraLimit = 0;
            machine.afterWorking(); finished.postWorking(machine);
            super.resetRecipeLogic();
            setStatus(suspendAfterFinish ? Status.SUSPEND : Status.IDLE);
            suspendAfterFinish = false;
            core.releaseUnusedData(); core.flush(); core.markDirty();
        }

        private List<Content> roll(GTRecipe recipe, RecipeCapability<?> capability) {
            List<Content> guaranteed = new ArrayList<>(), chances = new ArrayList<>();
            for (Content content : recipe.getOutputContents(capability)) {
                (content.chance >= content.maxChance ? guaranteed : chances).add(content);
            }
            if (!chances.isEmpty()) {
                GTRecipe powerRecipe = recipe.copy();
                if (recipe.data.contains("gtcsolo_ical_power_parallel"))
                    powerRecipe.parallels = Math.max(1, recipe.data.getInt("gtcsolo_ical_power_parallel"));
                int tier = RecipeHelper.getPreOCRecipeEuTier(powerRecipe);
                guaranteed.addAll(recipe.getChanceLogicForCapability(capability, IO.OUT, false).roll(chances,
                        recipe.getType().getChanceFunction(), tier, tier + recipe.ocLevel,
                        chanceCaches.computeIfAbsent(capability, cap -> cap.makeChanceCache()), recipe.parallels));
            }
            return guaranteed;
        }

        private static boolean outputNumbersFit(GTRecipe recipe, int multiplier) {
            for (Content content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
                if (content.slotName != null) return false;
                var ingredient = ItemRecipeCapability.CAP.of(content.content);
                if (ingredient instanceof com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient) return false;
                var choices = ingredient.getItems();
                if (choices.length == 0 || choices[0].isEmpty()) return false;
                int amount = ingredient instanceof SizedIngredient sized ? sized.getAmount() : choices[0].getCount();
                if (amount <= 0 || content.chance >= content.maxChance && (long) amount * multiplier > Integer.MAX_VALUE) return false;
            }
            for (Content content : recipe.getOutputContents(FluidRecipeCapability.CAP)) {
                if (content.slotName != null) return false;
                int amount = FluidRecipeCapability.CAP.of(content.content).getAmount();
                if (amount <= 0 || content.chance >= content.maxChance && (long) amount * multiplier > Integer.MAX_VALUE) return false;
            }
            return true;
        }

        @Override public void resetRecipeLogic() {
            if (paidRecipe != null) paidProgress = Math.max(paidProgress, progress);
            super.resetRecipeLogic();
        }
        @Override public void interruptRecipe() {
            // A paid job is paused rather than silently thrown away.
            if (paidRecipe != null) { setWaiting(Component.translatable("gtcsolo.circuit_line.paused")); return; }
            super.interruptRecipe();
        }

        private void savePaidJob(CompoundTag state) {
            if (paidRecipe != null) {
                state.put("paid", com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer.CODEC
                        .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, paidRecipe).getOrThrow(false, message -> {}));
                state.putString("paidId", paidRecipe.id.toString());
                state.putInt("paidParallels", paidRecipe.parallels);
                state.putInt("paidOC", paidRecipe.ocLevel);
            }
            state.putInt("paidProgress", paidProgress); state.putInt("paidDuration", paidDuration);
            state.putBoolean("paidSpecial", paidSpecial); state.putString("rollRecipe", previousRollRecipe);
            state.putInt("paidExtras", paidExtras); state.putInt("paidExtraLimit", paidExtraLimit);
        }
        private void loadPaidJob(CompoundTag state) {
            paidRecipe = state.contains("paid", Tag.TAG_COMPOUND)
                    ? com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,
                            state.getCompound("paid")).getOrThrow(false, message -> {}) : null;
            if (paidRecipe != null) {
                paidRecipe.id = new net.minecraft.resources.ResourceLocation(state.getString("paidId"));
                paidRecipe.parallels = Math.max(1, state.getInt("paidParallels"));
                paidRecipe.ocLevel = state.getInt("paidOC");
            }
            paidProgress = state.getInt("paidProgress"); paidDuration = state.getInt("paidDuration");
            paidSpecial = state.getBoolean("paidSpecial"); previousRollRecipe = state.getString("rollRecipe");
            paidExtraLimit = Math.max(0, Math.min(12, state.getInt("paidExtraLimit")));
            paidExtras = Math.max(0, Math.min(paidExtraLimit, state.getInt("paidExtras")));
        }
    }
}
