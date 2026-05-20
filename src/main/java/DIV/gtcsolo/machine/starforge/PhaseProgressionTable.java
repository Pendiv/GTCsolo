package DIV.gtcsolo.machine.starforge;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * StarForge の構築/崩壊フェイズで投入アイテムから count を加算するテーブル。
 *
 * <ul>
 *   <li>{@code defaultItems}:   count を {@value #DEFAULT_VALUE} 加算する標準アイテム群 (ItemID で判定)</li>
 *   <li>{@code effectiveEntries}: count を任意値だけ進める例外群 (Predicate で判定、 JEI で「Effective」ラベル表示)</li>
 * </ul>
 *
 * {@link #evaluate(ItemStack)} は Effective → default の順で判定する。
 * いずれにも該当しないアイテムは 0 (= 投入対象外) を返す。
 */
public final class PhaseProgressionTable {

    public static final long DEFAULT_VALUE = 1L;

    private final List<Item> defaultItems;
    private final List<EffectiveEntry> effectiveEntries;

    private PhaseProgressionTable(List<Item> defaultItems, List<EffectiveEntry> effectiveEntries) {
        this.defaultItems = List.copyOf(defaultItems);
        this.effectiveEntries = List.copyOf(effectiveEntries);
    }

    public List<Item> getDefaultItems() {
        return defaultItems;
    }

    public List<EffectiveEntry> getEffectiveEntries() {
        return effectiveEntries;
    }

    public long evaluate(ItemStack stack) {
        if (stack.isEmpty()) return 0L;
        for (EffectiveEntry e : effectiveEntries) {
            if (e.matcher.test(stack)) return e.value;
        }
        for (Item item : defaultItems) {
            if (stack.is(item)) return DEFAULT_VALUE;
        }
        return 0L;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class EffectiveEntry {
        public final Predicate<ItemStack> matcher;
        public final long value;
        /** JEI スロット表示用の代表 ItemStack (= 例えば 空 star_locus なら NBT 無し版) */
        public final ItemStack displayStack;

        public EffectiveEntry(Predicate<ItemStack> matcher, long value, ItemStack displayStack) {
            this.matcher = matcher;
            this.value = value;
            this.displayStack = displayStack;
        }
    }

    public static final class Builder {
        private final List<Item> defaultItems = new ArrayList<>();
        private final List<EffectiveEntry> effectiveEntries = new ArrayList<>();

        private Builder() {}

        public Builder addDefault(Item... items) {
            defaultItems.addAll(Arrays.asList(items));
            return this;
        }

        public Builder addEffective(Predicate<ItemStack> matcher, long value, ItemStack displayStack) {
            effectiveEntries.add(new EffectiveEntry(matcher, value, displayStack));
            return this;
        }

        public PhaseProgressionTable build() {
            return new PhaseProgressionTable(defaultItems, effectiveEntries);
        }
    }
}
