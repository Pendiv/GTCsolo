package DIV.gtcsolo.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TooltipDisplayHelper {
    public static final String TOOLTIP_DISPLAY_KEY = "tooltip_display";

    private TooltipDisplayHelper() {}

    public static List<String> readLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        if (stack.isEmpty() || !stack.hasTag()) return lines;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TOOLTIP_DISPLAY_KEY)) return lines;

        if (tag.contains(TOOLTIP_DISPLAY_KEY, Tag.TAG_STRING)) {
            String line = tag.getString(TOOLTIP_DISPLAY_KEY);
            if (!line.isEmpty()) lines.add(line);
            return lines;
        }

        if (tag.contains(TOOLTIP_DISPLAY_KEY, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(TOOLTIP_DISPLAY_KEY, Tag.TAG_STRING);
            for (int i = 0; i < listTag.size(); i++) {
                String line = listTag.getString(i);
                if (!line.isEmpty()) lines.add(line);
            }
        }

        return lines;
    }

    public static boolean writeLines(ItemStack stack, Collection<String> inputLines) {
        if (stack.isEmpty()) return false;

        List<String> lines = new ArrayList<>();
        for (String line : inputLines) {
            if (line != null && !line.isEmpty()) lines.add(line);
        }
        if (lines.isEmpty()) return false;

        CompoundTag tag = stack.getOrCreateTag();

        if (lines.size() == 1) {
            tag.putString(TOOLTIP_DISPLAY_KEY, lines.get(0));
            return true;
        }

        ListTag listTag = new ListTag();
        for (String line : lines) {
            listTag.add(StringTag.valueOf(line));
        }
        tag.put(TOOLTIP_DISPLAY_KEY, listTag);
        return true;
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        for (String line : readLines(stack)) {
            tooltip.add(Component.literal(line));
        }
    }

    public static String getSubtypeKey(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return "";

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TOOLTIP_DISPLAY_KEY)) return "";

        return "tooltip_display=" + tag.get(TOOLTIP_DISPLAY_KEY);
    }
}