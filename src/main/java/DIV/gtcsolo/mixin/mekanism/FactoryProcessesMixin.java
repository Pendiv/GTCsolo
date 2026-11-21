package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import mekanism.api.Upgrade;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityFactory.ProcessInfo;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityFactory.class, remap = false)
public abstract class FactoryProcessesMixin {
    @Shadow public FactoryTier tier;
    @Shadow private CachedRecipe<?> getCachedRecipe(int index) { throw new AssertionError(); }
    @Unique private int[] gtcsolo$durations;
    @Shadow protected ProcessInfo[] processInfoSlots;
    @Shadow private boolean sortingNeeded;
    @Unique private int gtcsolo$clientProcesses = 1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtcsolo$enableSlotUpgrade(CallbackInfo ci) {
        gtcsolo$durations = new int[processInfoSlots.length];
        TileEntityFactory<?> factory = (TileEntityFactory<?>) (Object) this;
        if (factory.supportsUpgrades()) factory.getComponent().setSupported(SlotUpgrade.TYPE);
    }

    @Redirect(method = {"<init>", "presetVariables", "load", "m_142466_"}, at = @At(value = "FIELD",
            target = "Lmekanism/common/tier/FactoryTier;processes:I"))
    private int gtcsolo$allocateProcesses(FactoryTier tier) {
        return SlotUpgrade.layout(tier).processes;
    }

    @Inject(method = "getInitialInventory", at = @At("RETURN"))
    private void gtcsolo$preserveSlotNumbers(CallbackInfoReturnable<IInventorySlotHolder> result) {
        if (processInfoSlots.length <= tier.processes) return;
        // Slot indices on disk include the energy and secondary slots. Keep every old index stable.
        Set<IInventorySlot> added = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = tier.processes; i < processInfoSlots.length; i++) {
            ProcessInfo info = processInfoSlots[i];
            added.add(info.inputSlot());
            added.add(info.outputSlot());
            if (info.secondaryOutputSlot() != null) added.add(info.secondaryOutputSlot());
        }
        List<IInventorySlot> slots = result.getReturnValue().getInventorySlots(null);
        slots.sort((left, right) -> Boolean.compare(added.contains(left), added.contains(right)));
    }

    @Inject(method = "addContainerTrackers", at = @At("TAIL"))
    private void gtcsolo$syncProcessLimit(MekanismContainer container, CallbackInfo ci) {
        TileEntityFactory<?> factory = (TileEntityFactory<?>) (Object) this;
        container.track(SyncableInt.create(() -> SlotUpgrade.processes(factory), n -> gtcsolo$clientProcesses = n));
        for (int i = 0; i < gtcsolo$durations.length; i++) {
            int index = i;
            container.track(SyncableInt.create(() -> {
                CachedRecipe<?> cached = getCachedRecipe(index);
                return cached == null ? 0 : ((CachedRecipeConditionsAccess) cached).gtcsolo$getRequiredTicks().getAsInt();
            }, ticks -> gtcsolo$durations[index] = ticks));
        }
    }

    @Inject(method = "getScaledProgress", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$showRecipeDuration(int scale, int index, CallbackInfoReturnable<Double> result) {
        int duration = gtcsolo$durations[index];
        if (duration > 0) {
            TileEntityFactory<?> factory = (TileEntityFactory<?>) (Object) this;
            result.setReturnValue((double) factory.getProgress(index) * scale / duration);
        }
    }

    @Inject(method = "inputProducesOutput", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$allowUnlockedInputs(int index, ItemStack input, IInventorySlot output,
                                              IInventorySlot secondary, boolean update, CallbackInfoReturnable<Boolean> result) {
        TileEntityFactory<?> factory = (TileEntityFactory<?>) (Object) this;
        int limit = factory.getLevel() != null && factory.isRemote() ? gtcsolo$clientProcesses : SlotUpgrade.processes(factory);
        if (index >= limit) result.setReturnValue(false);
    }

    @Redirect(method = "sortInventory", at = @At(value = "FIELD",
            target = "Lmekanism/common/tile/factory/TileEntityFactory;processInfoSlots:[Lmekanism/common/tile/factory/TileEntityFactory$ProcessInfo;"))
    private ProcessInfo[] gtcsolo$sortableProcesses(TileEntityFactory<?> factory) {
        return Arrays.copyOf(processInfoSlots, SlotUpgrade.processes(factory));
    }

    @Inject(method = "getInfo", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$showSlotCount(Upgrade upgrade, CallbackInfoReturnable<List<Component>> result) {
        if (upgrade == SlotUpgrade.TYPE) {
            int count = SlotUpgrade.processes((TileEntityFactory<?>) (Object) this);
            result.setReturnValue(List.of(Component.translatable("upgrade.gtcsolo.slot.processes", count, SlotUpgrade.MAX + 1)));
        }
    }

    @Inject(method = "recalculateUpgrades", at = @At("TAIL"))
    private void gtcsolo$resortAfterUpgrade(Upgrade upgrade, CallbackInfo ci) {
        if (upgrade == SlotUpgrade.TYPE) sortingNeeded = true;
    }
}
