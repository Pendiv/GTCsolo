package DIV.gtcsolo.recipe;

import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * マニュアル (解説書) のクラフトレシピ: <b>本 + レンチ → マニュアル</b>。
 *
 * <p>レンチは消費されず<b>耐久 -1</b> で返却される ({@link #getRemainingItems})。 耐久が尽きる場合は壊れる (空返却)。
 * レンチ判定は {@code forge:tools/wrench} / {@code forge:tools/wrenches} の両タグを受ける (mod 差異吸収)。
 */
public class ManualCraftingRecipe extends CustomRecipe {

    private static final TagKey<Item> WRENCH = ItemTags.create(new ResourceLocation("forge", "tools/wrench"));
    private static final TagKey<Item> WRENCHES = ItemTags.create(new ResourceLocation("forge", "tools/wrenches"));

    public ManualCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private static boolean isWrench(ItemStack s) {
        return s.is(WRENCH) || s.is(WRENCHES);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        boolean book = false, wrench = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!book && s.is(Items.BOOK)) {
                book = true;
            } else if (!wrench && isWrench(s)) {
                wrench = true;
            } else {
                return false;   // 本/レンチ以外、 または 2 個目の本・レンチが混在
            }
        }
        return book && wrench;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        return new ItemStack(ModItems.MANUAL.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isWrench(s)) {
                remaining.set(i, damaged(s));   // レンチは耐久 -1 で返す
            }
        }
        return remaining;
    }

    /** レンチを耐久 -1 で複製。 耐久が尽きるなら空 (= 破壊) を返す。 非耐久アイテムはそのまま返す。 */
    private static ItemStack damaged(ItemStack wrench) {
        if (!wrench.isDamageableItem()) return wrench.copy();
        ItemStack copy = wrench.copy();
        copy.setCount(1);
        int next = copy.getDamageValue() + 1;
        if (next >= copy.getMaxDamage()) return ItemStack.EMPTY;
        copy.setDamageValue(next);
        return copy;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.MANUAL_CRAFTING.get();
    }
}
