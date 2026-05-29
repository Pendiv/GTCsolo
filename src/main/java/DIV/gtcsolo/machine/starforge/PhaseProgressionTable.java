package DIV.gtcsolo.machine.starforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

        /**
         * ResourceLocation で item を解決して default に追加する。
         * 未解決 (= 当該 mod 未導入 / item 削除済) なら silent skip。
         * KubeJS から「開発環境にない item を要求アイテムに含めたい」 用途で使う。
         */
        public Builder addDefaultIfPresent(ResourceLocation id) {
            if (id == null) return this;
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(defaultItems::add);
            return this;
        }

        /** {@code "modid:path"} 文字列を ResourceLocation に parse してから {@link #addDefaultIfPresent(ResourceLocation)}。 */
        public Builder addDefaultIfPresent(String id) {
            if (id == null) return this;
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? this : addDefaultIfPresent(rl);
        }

        /**
         * Predicate 付きで Effective を登録する。 displayId が未解決なら silent skip。
         * matcher は KubeJS から SAM lambda で渡せる (= {@code (stack) => stack.is(...)} 等)。
         */
        public Builder addEffectiveIfPresent(ResourceLocation displayId, Predicate<ItemStack> matcher, long value) {
            if (displayId == null || matcher == null) return this;
            BuiltInRegistries.ITEM.getOptional(displayId)
                    .ifPresent(item -> effectiveEntries.add(
                            new EffectiveEntry(matcher, value, new ItemStack(item))));
            return this;
        }

        public Builder addEffectiveIfPresent(String displayId, Predicate<ItemStack> matcher, long value) {
            if (displayId == null) return this;
            ResourceLocation rl = ResourceLocation.tryParse(displayId);
            return rl == null ? this : addEffectiveIfPresent(rl, matcher, value);
        }

        /**
         * 単一 item match の Effective を短く登録する (= predicate を書く必要がない場合)。
         * matcher は内部で {@code stack.is(item)} = ID 一致 (NBT 無視) のみ。
         * NBT 条件等が必要なら {@link #addEffectiveIfPresent(ResourceLocation, Predicate, long)} を使う。
         */
        public Builder addEffectiveItem(ResourceLocation id, long value) {
            if (id == null) return this;
            BuiltInRegistries.ITEM.getOptional(id)
                    .ifPresent(item -> effectiveEntries.add(new EffectiveEntry(
                            stack -> stack.is(item), value, new ItemStack(item))));
            return this;
        }

        public Builder addEffectiveItem(String id, long value) {
            if (id == null) return this;
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? this : addEffectiveItem(rl, value);
        }

        public PhaseProgressionTable build() {
            return new PhaseProgressionTable(defaultItems, effectiveEntries);
        }
    }
}
