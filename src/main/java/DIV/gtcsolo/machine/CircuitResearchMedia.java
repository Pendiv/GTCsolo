package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.machine.multiblock.part.DataAccessHatchMachine;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.gregtechceu.gtceu.utils.ResearchManager;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/** Index only physical hatch contents and native research links; no per-tick recipe registry scans. */
@Mod.EventBusSubscriber(modid = "gtcsolo", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CircuitResearchMedia {
    private record ResearchKey(ResourceLocation type, String research, Item item) {}
    private record Medium(DataAccessHatchMachine hatch, int slot, ItemStack stack, ResearchKey key) {
        boolean present() {
            if (hatch.isInValid() || hatch.importItems.storage.getStackInSlot(slot) != stack || stack.isEmpty()) return false;
            var research = ResearchManager.readResearchId(stack);
            return research != null && research.getFirst() != null
                    && key.type().equals(research.getFirst().registryName) && key.research().equals(research.getSecond());
        }
    }

    private static volatile long dataRevision;
    private long indexedRevision = -1;
    private boolean dirty = true;
    private List<DataAccessHatchMachine> hatches = List.of();
    private final List<ISubscription> subscriptions = new ArrayList<>();
    private final Map<ResearchKey, List<Medium>> research = new HashMap<>();
    private final Map<String, List<Medium>> experience = new HashMap<>();

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) dataRevision++;
    }

    public void bind(List<DataAccessHatchMachine> next) {
        clear();
        hatches = List.copyOf(next);
        for (var hatch : hatches) subscriptions.add(hatch.importItems.addChangedListener(() -> dirty = true));
    }

    public void clear() {
        subscriptions.forEach(ISubscription::unsubscribe);
        subscriptions.clear();
        hatches = List.of();
        research.clear();
        experience.clear();
        dirty = true;
    }

    private void refresh() {
        if (!dirty && indexedRevision == dataRevision) return;
        research.clear();
        experience.clear();
        for (var hatch : hatches) for (int slot = 0; slot < hatch.importItems.getSlots(); slot++) {
            var stack = hatch.importItems.storage.getStackInSlot(slot);
            var identity = ResearchManager.readResearchId(stack);
            if (identity == null || identity.getFirst() == null || !ResearchManager.isStackDataItem(stack, true)) continue;
            var key = new ResearchKey(identity.getFirst().registryName, identity.getSecond(), stack.getItem());
            var medium = new Medium(hatch, slot, stack, key);
            research.computeIfAbsent(key, ignored -> new ArrayList<>()).add(medium);
            if (!CircuitResearchStorage.canRecord(stack)) continue;
            var linked = identity.getFirst().getDataStickEntry(identity.getSecond());
            if (linked == null) continue;
            for (var recipe : linked) {
                // Match both native research identity and its expected medium, just like the controller's unlock check.
                if (accepts(recipe, stack, identity.getSecond()))
                    experience.computeIfAbsent(IndustrialCircuitResearch.recipeKey(recipe), ignored -> new ArrayList<>()).add(medium);
            }
        }
        indexedRevision = dataRevision;
        dirty = false;
    }

    private static boolean accepts(GTRecipe recipe, ItemStack stack, String researchId) {
        for (var condition : recipe.conditions) if (condition instanceof ResearchCondition requirement && !requirement.isReverse()) {
            for (var entry : requirement.data)
                if (researchId.equals(entry.getResearchId()) && ItemStack.isSameItem(stack, entry.getDataItem())) return true;
        }
        return false;
    }

    public boolean unlocks(GTRecipe recipe) {
        refresh();
        boolean required = false;
        for (var condition : recipe.conditions) if (condition instanceof ResearchCondition requirement) {
            if (requirement.isReverse()) return false;
            for (var entry : requirement.data) {
                required = true;
                var key = new ResearchKey(recipe.recipeType.registryName, entry.getResearchId(), entry.getDataItem().getItem());
                boolean found = false;
                for (var medium : research.getOrDefault(key, List.of())) {
                    if (medium.present()) found = true;
                    else dirty = true;
                }
                if (!found) return false;
            }
        }
        return required;
    }

    public long count(String recipe) {
        refresh();
        long result = 0;
        for (var medium : experience.getOrDefault(recipe, List.of())) {
            if (medium.present()) result = Math.max(result, CircuitResearchStorage.count(medium.stack(), recipe));
            else dirty = true;
        }
        return result;
    }

    public void complete(String recipe) {
        refresh();
        Medium selected = null;
        long best = -1;
        for (var medium : experience.getOrDefault(recipe, List.of())) {
            if (!medium.present()) { dirty = true; continue; }
            if (!CircuitResearchStorage.canRecord(medium.stack())) continue;
            long count = CircuitResearchStorage.count(medium.stack(), recipe);
            if (count > best) { best = count; selected = medium; }
        }
        if (selected != null && CircuitResearchStorage.complete(selected.stack(), recipe)) {
            // Research identity and inventory occupancy did not change. Avoid rebuilding native research indexes.
            // Native slot synchronization observes the changed ItemStack NBT; mark the owning block for saving.
            selected.hatch().markDirty();
        }
    }
}
