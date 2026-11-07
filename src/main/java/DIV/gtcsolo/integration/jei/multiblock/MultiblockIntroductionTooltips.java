package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Compact only those controllers whose full introduction was registered successfully. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class MultiblockIntroductionTooltips {
    private static volatile Map<Item, List<GTRecipeType>> documented = Map.of();
    private static volatile Set<Item> singleBlocks = Set.of();

    private MultiblockIntroductionTooltips() {}

    public static void replaceCatalog(List<MultiblockIntroduction> introductions) {
        var entries = new LinkedHashMap<Item, List<GTRecipeType>>();
        var singles = new HashSet<Item>();
        for (var introduction : introductions) if (introduction.isSingleBlock()) singles.add(introduction.subject(0).getItem());
        for (var introduction : introductions) for (var variant : introduction.variants()) {
            var definition = variant.definition();
            var types = definition.getRecipeTypes();
            entries.put(definition.asStack().getItem(), types == null ? List.of() : Arrays.stream(types)
                    .filter(type -> type != null && type != GTRecipeTypes.DUMMY_RECIPES).distinct().toList());
        }
        documented = Map.copyOf(entries);
        singleBlocks = Set.copyOf(singles);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        compact(event.getItemStack(), event.getToolTip(), event.getFlags().isAdvanced());
    }

    static void compact(ItemStack stack, List<Component> lines, boolean advanced) {
        if (!stack.isEmpty() && singleBlocks.contains(stack.getItem())) {
            lines.add(Math.min(1, lines.size()), Component.translatable("gtcsolo.jei.introduction.details_hint"));
            return;
        }
        var types = documented.get(stack.getItem());
        if (types == null || stack.isEmpty() || lines.isEmpty()) return;
        var name = lines.get(0);
        var debugLines = new ArrayList<Component>();
        if (advanced) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            for (int i = 1; i < lines.size(); i++) {
                var line = lines.get(i);
                if (line.getString().equals(itemId) || line.getContents() instanceof TranslatableContents text
                        && text.getKey().equals("item.nbt_tags")) debugLines.add(line);
            }
        }
        lines.clear();
        lines.add(name);
        if (!types.isEmpty()) {
            var joined = Component.empty();
            for (int i = 0; i < types.size(); i++) {
                if (i > 0) joined.append(", ");
                joined.append(Component.translatable(types.get(i).registryName.toLanguageKey()));
            }
            lines.add(Component.translatable("gtceu.machine.available_recipe_map_1.tooltip", joined));
        }
        lines.add(Component.translatable("gtcsolo.jei.introduction.details_hint"));
        lines.addAll(debugLines);
    }
}
