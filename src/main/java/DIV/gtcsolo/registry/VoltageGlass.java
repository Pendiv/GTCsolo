package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.VoltageGlassBlock;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoltageGlass {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Gtcsolo.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Gtcsolo.MODID);
    public record Entry(int tier, RegistryObject<VoltageGlassBlock> block, RegistryObject<BlockItem> item) {}
    public static final List<Entry> ENTRIES = createEntries();

    private VoltageGlass() {}

    private static List<Entry> createEntries() {
        List<Entry> entries = new ArrayList<>();
        for (int i = GTValues.LV; i <= GTValues.OpV; i++) {
            final int tier = i;
            String id = "voltage_glass_" + GTValues.VN[tier].toLowerCase(Locale.ROOT);
            var block = BLOCKS.register(id, () -> new VoltageGlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS), tier));
            RegistryObject<BlockItem> item = ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
                    super.appendHoverText(stack, level, lines, flag);
                    lines.add(Component.translatable("tooltip.gtcsolo.voltage_glass.tier", GTValues.VN[tier], tier)
                            .withStyle(ChatFormatting.GRAY));
                }
            });
            entries.add(new Entry(tier, block, item));
        }
        return List.copyOf(entries);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static VoltageGlassBlock get(int tier) {
        validateRange(tier, tier);
        return ENTRIES.get(tier - GTValues.LV).block().get();
    }

    /** Call from a machine's pattern callback, after Forge block registration. */
    public static TraceabilityPredicate exact(int tier) { return between(tier, tier); }

    /** Accepts every registered grade at or above the minimum, including mixed grades. */
    public static TraceabilityPredicate atLeast(int minTier) { return between(minTier, GTValues.OpV); }

    /** Uses GT's native block predicate so preview candidates and accepted blocks agree. */
    public static TraceabilityPredicate between(int minTier, int maxTier) {
        validateRange(minTier, maxTier);
        return Predicates.blocks(ENTRIES.stream().filter(entry -> entry.tier() >= minTier && entry.tier() <= maxTier)
                .map(entry -> entry.block().get()).toArray(Block[]::new));
    }

    private static void validateRange(int minTier, int maxTier) {
        if (minTier < GTValues.LV || maxTier > GTValues.OpV || minTier > maxTier) {
            throw new IllegalArgumentException("Voltage glass range must be within LV..OpV: " + minTier + ".." + maxTier);
        }
    }

    public static void addToCreativeTab(CreativeModeTab.Output output) {
        ENTRIES.forEach(entry -> output.accept(entry.item().get()));
    }
}
