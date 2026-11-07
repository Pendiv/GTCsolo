package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.IterativeSampleInventory;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;

/** Runs the production sample counter with real Minecraft stacks and Forge inventory handlers. */
public final class IterativeSampleInventoryTest {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        try { Bootstrap.bootStrap(); }
        catch (ExceptionInInitializerError failure) {
            if (failure.getStackTrace().length == 0
                    || !failure.getStackTrace()[0].getClassName().equals("net.minecraftforge.network.NetworkHooks")) throw failure;
            System.out.println("Standalone fixture: Forge network transformation is not active.");
        }
        var ulv = new ItemStackHandler(1);
        assert !IterativeSampleInventory.qualifies(IterativeSampleInventory.count(ulv), Items.IRON_INGOT);
        ulv.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        assert IterativeSampleInventory.qualifies(IterativeSampleInventory.count(ulv), Items.IRON_INGOT);

        var split = new ItemStackHandler(4);
        split.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        split.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT));
        var counts = IterativeSampleInventory.count(split);
        assert counts.size() == 2 && !IterativeSampleInventory.qualifies(counts, Items.IRON_INGOT);
        var tagged = new ItemStack(Items.IRON_INGOT);
        tagged.getOrCreateTag().putString("sample_test", "same_item_different_nbt");
        split.setStackInSlot(2, tagged);
        counts = IterativeSampleInventory.count(split);
        assert counts.size() == 2 && counts.get(Items.IRON_INGOT) == 2L;
        assert IterativeSampleInventory.qualifies(counts, Items.IRON_INGOT);
        assert !IterativeSampleInventory.qualifies(counts, Items.GOLD_INGOT);
        assert !IterativeSampleInventory.qualifies(counts, Items.DIAMOND);
        assert split.getStackInSlot(0).getCount() == 1 && split.getStackInSlot(2).hasTag()
                : "counting must not consume or mutate samples";

        var types = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).limit(64).toList();
        assert types.size() == 64;
        var uhv = new ItemStackHandler(100);
        for (int index = 0; index < types.size(); index++) uhv.setStackInSlot(index, new ItemStack(types.get(index)));
        counts = IterativeSampleInventory.count(uhv);
        assert counts.size() == 64;
        for (var item : types) assert !IterativeSampleInventory.qualifies(counts, item);
        uhv.setStackInSlot(0, new ItemStack(types.get(0), 63));
        assert !IterativeSampleInventory.qualifies(IterativeSampleInventory.count(uhv), types.get(0));
        uhv.setStackInSlot(0, new ItemStack(types.get(0), 64));
        counts = IterativeSampleInventory.count(uhv);
        assert IterativeSampleInventory.qualifies(counts, types.get(0));
        assert !IterativeSampleInventory.qualifies(counts, types.get(1))
                : "an insufficient sample must not disable other qualifying outputs";

        split.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE));
        split.setStackInSlot(2, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE));
        assert IterativeSampleInventory.count(split).get(Items.IRON_INGOT) == 2L * Integer.MAX_VALUE;
        System.out.println("Sample inventory passed: ULV, 64 types, exact threshold, split stacks, NBT, independent eligibility, read-only counting and long totals.");
    }
}
