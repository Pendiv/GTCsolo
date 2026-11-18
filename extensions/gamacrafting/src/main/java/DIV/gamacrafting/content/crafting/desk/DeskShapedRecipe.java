package DIV.gamacrafting.content.crafting.desk;

import DIV.gamacrafting.Gamacrafting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.items.wrapper.RecipeWrapper;

import java.util.Map;
import java.util.Set;

import static DIV.gamacrafting.content.crafting.desk.DeskBlockEntity.GRID_WIDTH;

public class DeskShapedRecipe implements IDeskRecipe, IShapedRecipe<RecipeWrapper> {
    private final ResourceLocation id;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final boolean minecraft;
    private final boolean hideFromJei;

    public DeskShapedRecipe(ResourceLocation id, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean minecraft, boolean hideFromJei) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.minecraft = minecraft;
        this.hideFromJei = hideFromJei;
    }

    @Override
    public boolean isMinecraft() {
        return this.minecraft;
    }

    @Override
    public boolean showToJei() {
        return !this.hideFromJei;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public boolean matches(RecipeWrapper pContainer, Level pLevel) {
        for (int x = 0; x <= GRID_WIDTH - this.width; ++x) {
            for (int y = 0; y <= GRID_WIDTH - this.height; ++y) {
                if (this.matches(pContainer, x, y, true) || this.matches(pContainer, x, y, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public ItemStack assemble(RecipeWrapper pContainer, RegistryAccess pRegistryAccess) {
        return this.getResultItem(pRegistryAccess).copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth >= this.width && pHeight >= this.height;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return this.result;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Gamacrafting.SHAPED_DESK.get();
    }

    @Override
    public int getRecipeWidth() {
        return this.width;
    }

    @Override
    public int getRecipeHeight() {
        return this.height;
    }

    private boolean matches(RecipeWrapper wrapper, int offsetX, int offsetY, boolean mirror) {
        for (int column = 0; column < GRID_WIDTH; ++column) {
            for (int row = 0; row < GRID_WIDTH; ++row) {
                int x = column - offsetX;
                int y = row - offsetY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (x >= 0 && y >= 0 && x < this.width && y < this.height) {
                    if (mirror) {
                        ingredient = this.ingredients.get(this.width - x - 1 + y * this.width);
                    } else {
                        ingredient = this.ingredients.get(x + y * this.width);
                    }
                }

                if (!ingredient.test(wrapper.getItem(column + row * GRID_WIDTH))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<String, Ingredient> map, int width, int height) {
        NonNullList<Ingredient> dissolved = NonNullList.withSize(width * height, Ingredient.EMPTY);
        Set<String> set = Sets.newHashSet(map.keySet());
        set.remove(" ");

        for (int row = 0; row < pattern.length; ++row) {
            for (int column = 0; column < pattern[row].length(); ++column) {
                String at = pattern[row].substring(column, column + 1);
                Ingredient ingredient = map.get(at);
                if (ingredient == null) {
                    throw new JsonSyntaxException("Pattern references symbol '" + at + "' but it's not defined in the key");
                }

                set.remove(at);
                dissolved.set(column + width * row, ingredient);
            }
        }

        if (!set.isEmpty()) {
            throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set);
        } else {
            return dissolved;
        }
    }

    private static String[] shrink(String... strings) {
        int first = Integer.MAX_VALUE;
        int max = 0;
        int serial = 0;
        int spaces = 0;

        for (int index = 0; index < strings.length; ++index) {
            String string = strings[index];
            first = Math.min(first, firstNonSpace(string));
            int last = lastNonSpace(string);
            max = Math.max(max, last);
            if (last < 0) {
                if (serial == index) {
                    ++serial;
                }

                ++spaces;
            } else {
                spaces = 0;
            }
        }

        if (strings.length == spaces) {
            return new String[0];
        } else {
            String[] shrunk = new String[strings.length - spaces - serial];

            for (int index = 0; index < shrunk.length; ++index) {
                shrunk[index] = strings[index + serial].substring(first, max + 1);
            }

            return shrunk;
        }
    }

    private static int firstNonSpace(String string) {
        int index;
        for (index = 0; index < string.length() && string.charAt(index) == ' '; ++index) ;

        return index;
    }

    private static int lastNonSpace(String string) {
        int index;
        for (index = string.length() - 1; index >= 0 && string.charAt(index) == ' '; --index) ;

        return index;
    }

    private static String[] patternFromJson(JsonArray array) {
        String[] pattern = new String[array.size()];
        if (pattern.length > GRID_WIDTH) {
            throw new JsonSyntaxException("Invalid pattern: too many rows, " + GRID_WIDTH + " is maximum");
        } else if (pattern.length == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        } else {
            for (int index = 0; index < pattern.length; ++index) {
                String column = GsonHelper.convertToString(array.get(index), "pattern[" + index + "]");
                if (column.length() > GRID_WIDTH) {
                    throw new JsonSyntaxException("Invalid pattern: too many columns, " + GRID_WIDTH + " is maximum");
                }

                if (index > 0 && pattern[0].length() != column.length()) {
                    throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
                }

                pattern[index] = column;
            }

            return pattern;
        }
    }

    private static Map<String, Ingredient> keyFromJson(JsonObject pKeyEntry) {
        Map<String, Ingredient> map = Maps.newHashMap();

        for (Map.Entry<String, JsonElement> entry : pKeyEntry.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }

            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            map.put(entry.getKey(), Ingredient.fromJson(entry.getValue()));
        }

        map.put(" ", Ingredient.EMPTY);
        return map;
    }


    public static class Serializer implements RecipeSerializer<DeskShapedRecipe> {
        @Override
        public DeskShapedRecipe fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
            Map<String, Ingredient> map = keyFromJson(GsonHelper.getAsJsonObject(pJson, "key"));
            String[] shrunk = shrink(patternFromJson(GsonHelper.getAsJsonArray(pJson, "pattern")));
            if (shrunk.length == 0) {
                throw new JsonSyntaxException("Invalid pattern: at least one ingredient is required");
            }
            int width = shrunk[0].length();
            int height = shrunk.length;
            NonNullList<Ingredient> ingredients = dissolvePattern(shrunk, map, width, height);
            if (ingredients.stream().allMatch(Ingredient::isEmpty)) {
                throw new JsonSyntaxException("Invalid pattern: at least one ingredient is required");
            }
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pJson, "result"));
            if (result.isEmpty()) {
                throw new JsonSyntaxException("Gamacrafting recipe result must be a nonempty item stack");
            }
            boolean minecraft = GsonHelper.getAsBoolean(pJson, "minecraft", false);
            boolean hideFromJei = GsonHelper.getAsBoolean(pJson, "hideFromJei", false);
            return new DeskShapedRecipe(pRecipeId, width, height, ingredients, result, minecraft, hideFromJei);
        }

        @Override
        public DeskShapedRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            int width = pBuffer.readVarInt();
            int height = pBuffer.readVarInt();
            if (width < 1 || width > GRID_WIDTH || height < 1 || height > GRID_WIDTH) {
                throw new IllegalArgumentException("Invalid Gamacrafting recipe dimensions for " + pRecipeId);
            }
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(pBuffer));

            return new DeskShapedRecipe(pRecipeId, width, height, ingredients, pBuffer.readItem(), pBuffer.readBoolean(), pBuffer.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, DeskShapedRecipe pRecipe) {
            pBuffer.writeVarInt(pRecipe.width);
            pBuffer.writeVarInt(pRecipe.height);

            for (Ingredient ingredient : pRecipe.ingredients) {
                ingredient.toNetwork(pBuffer);
            }

            pBuffer.writeItem(pRecipe.result);
            pBuffer.writeBoolean(pRecipe.minecraft);
            pBuffer.writeBoolean(pRecipe.hideFromJei);
        }
    }
}
