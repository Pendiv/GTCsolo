package DIV.gtcsolo.integration.mekanism.capability;

import DIV.gtcsolo.machine.overpower.HatchRecipeTransfers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.*;

/**
 * Allocates output storage for Universal hatches and multi-slot chemical buses.
 * Search uses detached storage; execution can only consume the resulting per-tank,
 * per-substance reservation. In particular, a missing chance output cannot let the
 * next output steal a tank reserved for another substance.
 */
public final class UniversalOutputRouting {
    private UniversalOutputRouting() {}

    public static GTRecipe.ActionResult interceptMatch(GTRecipe recipe, IO io,
            IRecipeCapabilityHolder holder, Map<RecipeCapability<?>, List<Content>> contents, boolean tick) {
        if (io != IO.OUT || !affected(holder, contents)) return null;
        if (!supportedLayout(holder, contents)) return GTRecipe.ActionResult.fail(
                () -> Component.translatable("gtcsolo.recipe.universal_output_unsupported"));
        return plan(recipe, holder, contents, tick, 1) != null ? GTRecipe.ActionResult.SUCCESS : failure(holder);
    }

    public static Boolean interceptHandle(GTRecipe recipe, IO io, IRecipeCapabilityHolder holder,
            boolean tick, Map<RecipeCapability<?>, List<Content>> contents,
            Map<RecipeCapability<?>, Object2IntMap<?>> caches) {
        if (io != IO.OUT || !affected(holder, contents)) return null;
        if (!validChanceBounds(recipe, contents, tick, 1, caches)) return false;
        RoutedHolder routing = plan(recipe, holder, contents, tick, 1);
        if (routing == null) return false;
        routing.handlers.forEach(ReservedHandler::beginExecution);
        // The proxy holder is deliberately not affected(): GT rolls chance exactly once.
        return recipe.handleRecipe(io, routing, tick, contents, caches);
    }

    public static boolean canFinish(GTRecipe recipe, IRecipeCapabilityHolder holder) {
        return canFinish(recipe, holder, null);
    }

    public static boolean canFinish(GTRecipe recipe, IRecipeCapabilityHolder holder,
            Map<RecipeCapability<?>, Object2IntMap<?>> caches) {
        return !affected(holder, recipe.outputs)
                || validChanceBounds(recipe, recipe.outputs, false, 1, caches)
                && plan(recipe, holder, recipe.outputs, false, 1) != null;
    }

    public static int limitParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, int maximum) {
        boolean normal = affected(holder, recipe.outputs);
        boolean tick = affected(holder, recipe.tickOutputs);
        if (!normal && !tick) return maximum;
        int low = 0, high = maximum;
        while (low < high) {
            int mid = low + (int) (((long) high - low + 1) / 2);
            boolean fits = (!normal || plan(recipe, holder, recipe.outputs, false, mid) != null)
                    && (!tick || plan(recipe, holder, recipe.tickOutputs, true, mid) != null);
            if (fits) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    private static GTRecipe.ActionResult failure(IRecipeCapabilityHolder holder) {
        // GT 1.6.4's FAIL_NO_REASON has isSuccess=true; never use it here.
        return GTRecipe.ActionResult.fail(() -> blockedReason(holder));
    }

    public static Component blockedReason(IRecipeCapabilityHolder holder) {
        for (IO io : List.of(IO.OUT, IO.BOTH)) {
            for (RecipeCapability<?> cap : List.of(FluidRecipeCapability.CAP, ChemicalCapabilities.GAS)) {
                List<IRecipeHandler<?>> handlers = holder.getCapabilitiesProxy().get(io, cap);
                if (handlers != null && handlers.stream().anyMatch(h -> owner(h) != null)) {
                    return Component.translatable("gtcsolo.recipe.universal_output_full");
                }
            }
        }
        return Component.translatable("gtcsolo.recipe.chemical_output_full");
    }

    private static boolean affected(IRecipeCapabilityHolder holder,
                                    Map<RecipeCapability<?>, List<Content>> contents) {
        if (holder instanceof RoutedHolder || (!has(contents, FluidRecipeCapability.CAP)
                && !has(contents, ChemicalCapabilities.GAS))) return false;
        for (IO io : List.of(IO.OUT, IO.BOTH)) {
            for (RecipeCapability<?> cap : List.of(FluidRecipeCapability.CAP, ChemicalCapabilities.GAS)) {
                List<IRecipeHandler<?>> handlers = holder.getCapabilitiesProxy().get(io, cap);
                if (handlers != null && handlers.stream().anyMatch(
                        h -> owner(h) != null || h instanceof NotifiableGasBank)) return true;
            }
        }
        return false;
    }

    private static boolean has(Map<RecipeCapability<?>, List<Content>> contents, RecipeCapability<?> cap) {
        return !contents.getOrDefault(cap, List.of()).isEmpty();
    }

    private static boolean supportedLayout(IRecipeCapabilityHolder holder,
                                           Map<RecipeCapability<?>, List<Content>> contents) {
        // A custom dispatcher must explicitly bridge its own output semantics.
        if (holder instanceof com.gregtechceu.gtceu.common.machine.multiblock.electric.DistillationTowerMachine
                && has(contents, ChemicalCapabilities.GAS)) return false;
        for (RecipeCapability<?> cap : List.of(FluidRecipeCapability.CAP, ChemicalCapabilities.GAS)) {
            if (!has(contents, cap)) continue;
            if (contents.get(cap).stream().anyMatch(content -> content.slotName != null)) return false;
            for (IO io : List.of(IO.OUT, IO.BOTH)) {
                var handlers = holder.getCapabilitiesProxy().get(io, cap);
                if (handlers == null) continue;
                for (var handler : handlers) {
                    if (cap == FluidRecipeCapability.CAP) {
                        // Creative/virtual handlers must not be treated as finite physical storage.
                        if (handler.getClass() != NotifiableFluidTank.class
                                && !(handler instanceof DIV.gtcsolo.machine.overpower.OverPowerFluidHatchMachine.LargeFluidRecipeHandler)
                                && !(handler instanceof MultiChemicalBusMachine.MultiFluidTank)) return false;
                    } else if (handler.getClass() != NotifiableChemicalTank.class
                            && handler.getClass() != NotifiableGasBank.class) return false;
                }
            }
        }
        return true;
    }

    private static UniversalIOHatchMachine owner(IRecipeHandler<?> handler) {
        if (handler instanceof NotifiableFluidTank fluid
                && fluid.getMachine() instanceof UniversalIOHatchMachine universal) return universal;
        if (handler instanceof NotifiableChemicalTank gas
                && gas.getMachine() instanceof UniversalIOHatchMachine universal) return universal;
        return null;
    }

    private static RoutedHolder plan(GTRecipe recipe, IRecipeCapabilityHolder holder,
            Map<RecipeCapability<?>, List<Content>> contents, boolean tick, int multiplier) {
        if (!validChanceBounds(recipe, contents, tick, multiplier, null)) return null;
        if (!supportedLayout(holder, contents)) return null;
        Map<RecipeCapability<?>, List<Content>> envelope = envelope(recipe, contents, multiplier);
        if (envelope == null) return null;
        List<UniversalIOHatchMachine> empty = new ArrayList<>();
        Set<UniversalIOHatchMachine> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var cell : holder.getCapabilitiesProxy().cellSet()) {
            if (cell.getRowKey() == IO.IN) continue;
            for (IRecipeHandler<?> handler : cell.getValue()) {
                UniversalIOHatchMachine hatch = owner(handler);
                if (hatch == null || !seen.add(hatch)) continue;
                boolean fluid = !hatch.getFluidTank().getStorages()[0].isEmpty();
                boolean gas = !hatch.getGasTank().getMekTank().isEmpty();
                if (fluid && gas) return null; // Preserve an invalid old save rather than deleting either side.
                if (!fluid && !gas) empty.add(hatch);
            }
        }
        // Only interchange adjacent, otherwise identical handlers. A dedicated hatch between
        // two Universals can make their order significant to GT's native greedy routing.
        int[] groups = groups(holder, empty);
        RoutedHolder[] selected = new RoutedHolder[1];
        int[] roles = UniversalRoleSearch.find(groups, candidate -> {
            var roleMap = new IdentityHashMap<UniversalIOHatchMachine, Integer>();
            for (int i = 0; i < empty.size(); i++) roleMap.put(empty.get(i), candidate[i]);
            if (!fitsCapacityBound(holder, envelope, roleMap)) return false;
            // Native greedy matching is NOT an optimistic upper bound: an undecided
            // Universal may steal a fluid from a dedicated tank needed later. Only
            // reject partial assignments using the relaxed quantity bounds above.
            for (int role : candidate) if (role == 0) return true;
            RoutedHolder routed = RoutedHolder.create(holder, roleMap, contents.keySet());
            if (routed == null || !recipe.matchRecipeContents(IO.OUT, routed, envelope, tick).isSuccess()) return false;
            selected[0] = routed;
            return true;
        });
        return roles == null ? null : selected[0];
    }

    private static long addBound(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    /** Deliberately ignores species, filters and slots, so failure proves insufficient capacity. */
    private static boolean fitsCapacityBound(IRecipeCapabilityHolder holder,
            Map<RecipeCapability<?>, List<Content>> contents,
            IdentityHashMap<UniversalIOHatchMachine, Integer> roles) {
        long fluidNeeded = 0, gasNeeded = 0, fluidSpace = 0, gasSpace = 0, combinedSpace = 0;
        for (Content content : contents.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            fluidNeeded = addBound(fluidNeeded, Math.max(0, ((FluidIngredient) content.content).getAmount()));
        }
        for (Content content : contents.getOrDefault(ChemicalCapabilities.GAS, List.of())) {
            gasNeeded = addBound(gasNeeded, Math.max(0, ((ChemicalIngredient) content.content).getAmount()));
        }
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var shared = new IdentityHashMap<UniversalIOHatchMachine, long[]>();
        for (var cell : holder.getCapabilitiesProxy().cellSet()) {
            if (cell.getRowKey() == IO.IN) continue;
            for (var handler : cell.getValue()) {
                UniversalIOHatchMachine hatch = owner(handler);
                int role = roles.getOrDefault(hatch, 0);
                long space = 0;
                if (cell.getColumnKey() == FluidRecipeCapability.CAP && handler instanceof NotifiableFluidTank tank) {
                    if (role == 2 || hatch != null && !hatch.getGasTank().getMekTank().isEmpty()) continue;
                    for (CustomFluidTank storage : tank.getStorages()) {
                        if (seen.add(storage)) space = addBound(space,
                                Math.max(0L, (long) storage.getCapacity() - storage.getFluidAmount()));
                    }
                    fluidSpace = addBound(fluidSpace, space);
                    if (hatch != null) {
                        long[] spaces = shared.computeIfAbsent(hatch, ignored -> new long[2]);
                        spaces[0] = Math.max(spaces[0], space);
                    }
                } else if (cell.getColumnKey() == ChemicalCapabilities.GAS && handler instanceof NotifiableChemicalTank tank) {
                    if (role == 1 || hatch != null && !hatch.getFluidTank().getStorages()[0].isEmpty()) continue;
                    if (seen.add(tank.getMekTank())) space = Math.max(0L, tank.getCapacity() - tank.getMekTank().getStored());
                    gasSpace = addBound(gasSpace, space);
                    if (hatch != null) {
                        long[] spaces = shared.computeIfAbsent(hatch, ignored -> new long[2]);
                        spaces[1] = Math.max(spaces[1], space);
                    }
                } else if (cell.getColumnKey() == ChemicalCapabilities.GAS && handler instanceof NotifiableGasBank bank) {
                    for (int i = 0; i < bank.getTanks(); i++) {
                        if (seen.add(bank.getMekTank(i))) space = addBound(space,
                                Math.max(0L, bank.getCapacity() - bank.getMekTank(i).getStored()));
                    }
                    gasSpace = addBound(gasSpace, space);
                }
                if (hatch == null) combinedSpace = addBound(combinedSpace, space);
            }
        }
        for (long[] spaces : shared.values()) combinedSpace = addBound(combinedSpace, Math.max(spaces[0], spaces[1]));
        return fluidNeeded <= fluidSpace && gasNeeded <= gasSpace
                && addBound(fluidNeeded, gasNeeded) <= combinedSpace;
    }

    /**
     * The envelope reserves at most one copy per chance entry and parallel. Only
     * GT's known chance implementations can prove that bound without rolling.
     * In particular, OR uses int multiplication and mutable per-ingredient caches.
     */
    private static boolean validChanceBounds(GTRecipe recipe,
            Map<RecipeCapability<?>, List<Content>> contents, boolean tick, int multiplier,
            Map<RecipeCapability<?>, Object2IntMap<?>> caches) {
        if (multiplier < 1) return false;
        for (RecipeCapability<?> cap : contents.keySet()) {
            ChanceLogic logic = recipe.getChanceLogicForCapability(cap, IO.OUT, tick);
            Map<Object, Integer> cacheDomains = new HashMap<>();
            Object2IntMap<?> cache = caches == null ? null : caches.get(cap);
            for (Content content : contents.getOrDefault(cap, List.of())) {
                if (content.maxChance <= 0 || content.chance < 0) return false;
                // Guaranteed contents never enter ChanceLogic.roll().
                if (content.chance >= content.maxChance) continue;
                if (recipe.parallels < 1) return false;
                if (logic == ChanceLogic.NONE) continue;
                if (logic != ChanceLogic.OR && logic != ChanceLogic.AND && logic != ChanceLogic.XOR) return false;

                ChanceBoostFunction boost = recipe.getType().getChanceFunction();
                if (boost != ChanceBoostFunction.NONE && boost != ChanceBoostFunction.OVERCLOCK) return false;
                int recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
                long chanceTier = (long) recipeTier + recipe.ocLevel;
                if (chanceTier < Integer.MIN_VALUE || chanceTier > Integer.MAX_VALUE) return false;
                int boosted = boost.getBoostedChance(content, recipeTier, (int) chanceTier);
                if (boosted < 0 || boosted > content.maxChance) return false;

                long times = (long) recipe.parallels * multiplier;
                if (times > Integer.MAX_VALUE) return false;
                long cacheMaximum = (long) content.maxChance - 1;
                if (logic == ChanceLogic.OR) {
                    long totalChance = times * boosted;
                    if (totalChance > Integer.MAX_VALUE) return false;
                    // Also protect native `newChance + cached` when maxChance is unusually large.
                    if (totalChance % content.maxChance + cacheMaximum > Integer.MAX_VALUE) return false;
                } else if ((long) boosted + cacheMaximum > Integer.MAX_VALUE) return false;

                // Equal ingredient keys share a cache. Mixing denominator domains could let
                // a later OR entry emit more than the reserved number of copies.
                Integer previousDomain = cacheDomains.putIfAbsent(content.content, content.maxChance);
                if (previousDomain != null && previousDomain != content.maxChance) return false;
                if (cache != null && cache.containsKey(content.content)) {
                    int cached = cache.getInt(content.content);
                    if (cached < 0 || cached >= content.maxChance) return false;
                }
            }
        }
        return true;
    }

    private static int[] groups(IRecipeCapabilityHolder holder, List<UniversalIOHatchMachine> empty) {
        int[] result = new int[empty.size()];
        List<Object> previousKey = null;
        int[] previousPositions = null;
        for (int i = 0; i < empty.size(); i++) {
            var hatch = empty.get(i);
            List<Object> key = new ArrayList<>();
            key.add(hatch.getGasTank().getCapacity());
            key.add(hatch.getFluidTank().getFilter());
            key.add(hatch.getFluidTank().getPriority());
            key.add(hatch.getGasTank().getPriority());
            // Locked/filtered or distinct tanks are conservatively not interchangeable.
            if (hatch.getFluidTank().isLocked() || hatch.getFluidTank().isDistinct()
                    || hatch.getGasTank().isDistinct()) key.add(hatch);
            int[] positions = new int[4];
            int listIndex = 0;
            for (IO io : List.of(IO.OUT, IO.BOTH)) {
                for (RecipeCapability<?> cap : List.of(FluidRecipeCapability.CAP, ChemicalCapabilities.GAS)) {
                    var list = holder.getCapabilitiesProxy().get(io, cap);
                    int found = -1;
                    if (list != null) for (int index = 0; index < list.size(); index++) {
                        if (owner(list.get(index)) == hatch) { found = index; break; }
                    }
                    positions[listIndex++] = found;
                    key.add(found >= 0);
                }
            }
            boolean adjacent = key.equals(previousKey);
            if (adjacent) for (int index = 0; index < positions.length; index++) {
                if (positions[index] >= 0 && Math.abs(positions[index] - previousPositions[index]) != 1) {
                    adjacent = false;
                    break;
                }
            }
            result[i] = adjacent ? result[i - 1] : i;
            previousKey = key;
            previousPositions = positions;
        }
        return result;
    }

    private static Map<RecipeCapability<?>, List<Content>> envelope(GTRecipe recipe,
            Map<RecipeCapability<?>, List<Content>> contents, int multiplier) {
        Map<RecipeCapability<?>, List<Content>> result = new LinkedHashMap<>();
        try {
            for (var entry : contents.entrySet()) {
                var cap = entry.getKey();
                List<Content> copied = new ArrayList<>();
                for (Content content : entry.getValue()) {
                    long times = multiplier;
                    if (content.chance < content.maxChance) times = Math.multiplyExact(times, Math.max(1, recipe.parallels));
                    Object value = cap.copyContent(content.content);
                    if (cap == FluidRecipeCapability.CAP) {
                        // Universal and standard fluid/gas hatches have no named slots.
                        if (content.slotName != null) return null;
                        FluidIngredient fluid = (FluidIngredient) value;
                        fluid.setAmount(Math.toIntExact(Math.multiplyExact(fluid.getAmount(), times)));
                        if (fluid.getAmount() > 0 && (fluid.getStacks().length == 0 || fluid.getStacks()[0].isEmpty())) return null;
                    } else if (cap == ChemicalCapabilities.GAS) {
                        if (content.slotName != null) return null;
                        ChemicalIngredient gas = (ChemicalIngredient) value;
                        gas.setAmount(Math.multiplyExact(gas.getAmount(), times));
                        if (gas.getAmount() > 0 && gas.toStack().isEmpty()) return null;
                    } else if (times != 1) {
                        value = cap.copyContent(content.content, ContentModifier.multiplier(Math.toIntExact(times)));
                    }
                    copied.add(new Content(value, content.maxChance, content.maxChance, 0, content.slotName, content.uiName));
                }
                result.put(cap, copied);
            }
        } catch (ArithmeticException overflow) {
            return null;
        }
        return result;
    }

    private static final class RoutedHolder implements IRecipeCapabilityHolder {
        private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> proxies = HashBasedTable.create();
        private final List<ReservedHandler<?>> handlers = new ArrayList<>();

        static RoutedHolder create(IRecipeCapabilityHolder original,
                                   IdentityHashMap<UniversalIOHatchMachine, Integer> roles,
                                   Set<RecipeCapability<?>> capabilities) {
            RoutedHolder result = new RoutedHolder();
            Map<IRecipeHandler<?>, ReservedHandler<?>> wrapped = new IdentityHashMap<>();
            Set<Object> storageIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var cell : original.getCapabilitiesProxy().cellSet()) {
                List<IRecipeHandler<?>> list = new ArrayList<>();
                for (IRecipeHandler<?> handler : cell.getValue()) {
                    boolean fluid = cell.getColumnKey() == FluidRecipeCapability.CAP;
                    boolean gas = cell.getColumnKey() == ChemicalCapabilities.GAS;
                    if (cell.getRowKey() == IO.IN || !fluid && !gas || !capabilities.contains(cell.getColumnKey())) {
                        list.add(handler);
                        continue;
                    }
                    ReservedHandler<?> proxy = wrapped.get(handler);
                    if (proxy == null) {
                        int role = roles.getOrDefault(owner(handler), 0);
                        if (fluid && handler instanceof NotifiableFluidTank tank) {
                            for (CustomFluidTank storage : tank.getStorages()) {
                                if (!storageIdentities.add(storage)) return null;
                            }
                            proxy = new FluidReservation(tank, role != 2);
                        } else if (gas && handler instanceof NotifiableChemicalTank tank) {
                            if (!storageIdentities.add(tank.getMekTank())) return null;
                            proxy = new GasReservation(tank, role != 1);
                        } else if (gas && handler instanceof NotifiableGasBank bank) {
                            for (int i = 0; i < bank.getTanks(); i++) {
                                if (!storageIdentities.add(bank.getMekTank(i))) return null;
                            }
                            proxy = new GasBankReservation(bank);
                        } else return null; // No speculative writes into unknown/virtual output handlers.
                        wrapped.put(handler, proxy);
                        result.handlers.add(proxy);
                    }
                    list.add(proxy);
                }
                result.proxies.put(cell.getRowKey(), cell.getColumnKey(), list);
            }
            return result;
        }

        @Override public Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> getCapabilitiesProxy() { return proxies; }
    }

    private abstract static class ReservedHandler<T> implements IRecipeHandler<T> {
        final IRecipeHandler<T> original;
        final boolean allowed;
        boolean executing;

        ReservedHandler(IRecipeHandler<T> original, boolean allowed) { this.original = original; this.allowed = allowed; }
        abstract void beginExecution();
        @Override public RecipeCapability<T> getCapability() { return original.getCapability(); }
        @Override public int getPriority() { return original.getPriority(); }
        @Override public List<Object> getContents() { return original.getContents(); }
        @Override public double getTotalContentAmount() { return original.getTotalContentAmount(); }
        @Override public boolean isDistinct() { return !executing && original.isDistinct(); }
    }

    private static final class FluidReservation extends ReservedHandler<FluidIngredient> {
        final NotifiableFluidTank tank;
        final CustomFluidTank[] shadow;
        final FluidStack[] initial;
        final int[] remaining;

        FluidReservation(NotifiableFluidTank tank, boolean allowed) {
            super(tank, allowed);
            this.tank = tank;
            CustomFluidTank[] live = tank.getStorages();
            shadow = new CustomFluidTank[live.length];
            initial = new FluidStack[live.length];
            remaining = new int[live.length];
            for (int i = 0; i < live.length; i++) {
                CustomFluidTank source = live[i];
                shadow[i] = new CustomFluidTank(source.getCapacity(), source::isFluidValid);
                initial[i] = source.getFluid().copy();
                shadow[i].setFluid(initial[i].copy());
            }
        }

        @Override void beginExecution() {
            executing = true;
            for (int i = 0; i < shadow.length; i++) remaining[i] = shadow[i].getFluidAmount() - initial[i].getAmount();
        }

        @Override public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe,
                List<FluidIngredient> left, String slot, boolean simulate) {
            if (!allowed || io != IO.OUT || slot != null) return left.isEmpty() ? null : left;
            if (!executing) {
                FluidStack[] before = null;
                if (isDistinct()) before = Arrays.stream(shadow).map(s -> s.getFluid().copy()).toArray(FluidStack[]::new);
                var result = HatchRecipeTransfers.largeFluids(left, shadow, false, tank.isAllowSameFluids(), false);
                if (result != null && before != null) for (int i = 0; i < shadow.length; i++) shadow[i].setFluid(before[i]);
                return result;
            }
            for (var iterator = left.iterator(); iterator.hasNext();) {
                FluidIngredient ingredient = iterator.next();
                for (int i = 0; i < shadow.length && ingredient.getAmount() > 0; i++) {
                    if (remaining[i] <= 0 || !ingredient.test(shadow[i].getFluid())) continue;
                    FluidStack offered = shadow[i].getFluid().copy();
                    offered.setAmount(Math.min(remaining[i], ingredient.getAmount()));
                    int inserted = tank.getStorages()[i].fill(offered, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    ingredient.shrink(inserted);
                    if (!simulate) remaining[i] -= inserted;
                }
                if (ingredient.getAmount() <= 0) iterator.remove();
            }
            return left.isEmpty() ? null : left;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class GasReservation extends ReservedHandler<ChemicalIngredient> {
        final NotifiableChemicalTank tank;
        ChemicalIngredient stored;
        final long initial;
        long remaining;

        GasReservation(NotifiableChemicalTank tank, boolean allowed) {
            super(tank, allowed);
            this.tank = tank;
            initial = tank.getMekTank().getStored();
            stored = initial == 0 ? null : new ChemicalIngredient(ChemicalIngredient.Type.GAS,
                    tank.getStack().getTypeRegistryName(), initial);
        }

        @Override void beginExecution() { executing = true; remaining = stored == null ? 0 : stored.getAmount() - initial; }

        @Override public List<ChemicalIngredient> handleRecipeInner(IO io, GTRecipe recipe,
                List<ChemicalIngredient> left, String slot, boolean simulate) {
            if (!allowed || io != IO.OUT || slot != null) return left.isEmpty() ? null : left;
            ChemicalIngredient before = stored == null ? null : stored.copy();
            for (var iterator = left.iterator(); iterator.hasNext();) {
                ChemicalIngredient ingredient = iterator.next();
                if (ingredient.isEmpty()) { iterator.remove(); continue; }
                if (ingredient.getType() != ChemicalIngredient.Type.GAS
                        || stored != null && !stored.getId().equals(ingredient.getId())) continue;
                long accepted;
                if (!executing) {
                    long available = tank.getCapacity() - (stored == null ? 0 : stored.getAmount());
                    long wanted = Math.min(ingredient.getAmount(), available);
                    var stack = ingredient.toStack();
                    stack.setAmount(wanted);
                    long possible = wanted - tank.getMekTank().insert(stack, Action.SIMULATE, AutomationType.INTERNAL).getAmount();
                    accepted = Math.max(0, possible);
                    if (accepted > 0) stored = new ChemicalIngredient(ingredient.getType(), ingredient.getId(),
                            (stored == null ? 0 : stored.getAmount()) + accepted);
                } else {
                    long wanted = Math.min(ingredient.getAmount(), remaining);
                    var stack = ingredient.toStack();
                    stack.setAmount(wanted);
                    accepted = wanted - tank.getMekTank().insert(stack,
                            simulate ? Action.SIMULATE : Action.EXECUTE, AutomationType.INTERNAL).getAmount();
                    if (!simulate) remaining -= accepted;
                }
                ingredient.setAmount(ingredient.getAmount() - accepted);
                if (ingredient.isEmpty()) iterator.remove();
            }
            if (!executing && isDistinct() && !left.isEmpty()) stored = before;
            return left.isEmpty() ? null : left;
        }
    }

    /** A bank has independent slots; each still needs a fixed species reservation. */
    private static final class GasBankReservation extends ReservedHandler<ChemicalIngredient> {
        final NotifiableGasBank bank;
        final ChemicalIngredient[] stored;
        final long[] initial;
        final long[] remaining;

        GasBankReservation(NotifiableGasBank bank) {
            super(bank, true);
            this.bank = bank;
            stored = new ChemicalIngredient[bank.getTanks()];
            initial = new long[stored.length];
            remaining = new long[stored.length];
            for (int i = 0; i < stored.length; i++) {
                initial[i] = bank.getMekTank(i).getStored();
                if (initial[i] > 0) stored[i] = new ChemicalIngredient(ChemicalIngredient.Type.GAS,
                        bank.getStack(i).getTypeRegistryName(), initial[i]);
            }
        }

        @Override void beginExecution() {
            executing = true;
            for (int i = 0; i < stored.length; i++) {
                remaining[i] = stored[i] == null ? 0 : stored[i].getAmount() - initial[i];
            }
        }

        @Override public List<ChemicalIngredient> handleRecipeInner(IO io, GTRecipe recipe,
                List<ChemicalIngredient> left, String slot, boolean simulate) {
            if (io != IO.OUT || slot != null) return left.isEmpty() ? null : left;
            ChemicalIngredient[] before = null;
            if (!executing && isDistinct()) {
                before = Arrays.stream(stored).map(s -> s == null ? null : s.copy())
                        .toArray(ChemicalIngredient[]::new);
            }
            for (var iterator = left.iterator(); iterator.hasNext();) {
                ChemicalIngredient ingredient = iterator.next();
                if (ingredient.isEmpty()) { iterator.remove(); continue; }
                if (ingredient.getType() != ChemicalIngredient.Type.GAS) continue;
                // Match the bank's normal routing: matching gas first, then empty slots.
                for (int pass = 0; pass < 2 && !ingredient.isEmpty(); pass++) {
                    for (int i = 0; i < stored.length && !ingredient.isEmpty(); i++) {
                        boolean empty = stored[i] == null;
                        if (pass == 0 ? empty || !stored[i].getId().equals(ingredient.getId()) : !empty) continue;
                        long space = executing ? remaining[i]
                                : bank.getCapacity() - (empty ? 0 : stored[i].getAmount());
                        long wanted = Math.min(ingredient.getAmount(), space);
                        if (wanted <= 0) continue;
                        var stack = (mekanism.api.chemical.gas.GasStack) ingredient.toStack();
                        if (stack.isEmpty()) continue;
                        stack.setAmount(wanted);
                        long accepted = wanted - bank.getMekTank(i).insert(stack,
                                executing && !simulate ? Action.EXECUTE : Action.SIMULATE,
                                AutomationType.INTERNAL).getAmount();
                        if (accepted <= 0) continue;
                        if (!executing) {
                            stored[i] = new ChemicalIngredient(ingredient.getType(), ingredient.getId(),
                                    (empty ? 0 : stored[i].getAmount()) + accepted);
                        } else if (!simulate) remaining[i] -= accepted;
                        ingredient.setAmount(ingredient.getAmount() - accepted);
                    }
                }
                if (ingredient.isEmpty()) iterator.remove();
            }
            if (before != null && !left.isEmpty()) System.arraycopy(before, 0, stored, 0, stored.length);
            return left.isEmpty() ? null : left;
        }
    }
}
