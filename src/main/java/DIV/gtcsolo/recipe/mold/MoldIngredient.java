package DIV.gtcsolo.recipe.mold;

import DIV.gtcsolo.item.IronCastingMoldItem;
import DIV.gtcsolo.registry.IronCastingMolds;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.Nullable;
import java.util.stream.Stream;

/** One shape, with the original reusable mold and its iron alternative in the same recipe slot. */
public final class MoldIngredient extends AbstractIngredient {
    public static final ResourceLocation ID = new ResourceLocation("gtcsolo", "casting_mold");
    public final String shape;
    public final int toolsPerRecipe;
    final boolean pending;
    public final Item original;
    public final Item iron;

    public MoldIngredient(String shape, int toolsPerRecipe) { this(shape, toolsPerRecipe, false); }
    private MoldIngredient(String shape, int toolsPerRecipe, boolean pending) {
        super(values(shape));
        if (toolsPerRecipe < 1) throw new IllegalArgumentException("Mold count must be positive");
        this.shape = shape;
        this.toolsPerRecipe = toolsPerRecipe;
        this.pending = pending;
        this.original = IronCastingMolds.original(shape);
        this.iron = IronCastingMolds.MOLDS.get(shape).get();
    }
    private static Stream<Ingredient.Value> values(String shape) {
        if (!IronCastingMolds.MOLDS.containsKey(shape)) throw new IllegalArgumentException("Unknown mold shape: " + shape);
        return Stream.of(new Ingredient.ItemValue(new ItemStack(IronCastingMolds.original(shape))),
                new Ingredient.ItemValue(new ItemStack(IronCastingMolds.MOLDS.get(shape).get())));
    }
    MoldIngredient pending() { return pending ? this : new MoldIngredient(shape, toolsPerRecipe, true); }
    @Override public boolean test(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.is(original) || stack.is(iron) && IronCastingMoldItem.remaining(stack) > 0);
    }
    @Override public boolean isSimple() { return false; }
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
    @Override public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.addProperty("shape", shape);
        json.addProperty("tools", toolsPerRecipe);
        return json;
    }
    public static final IIngredientSerializer<MoldIngredient> SERIALIZER = new IIngredientSerializer<>() {
        @Override public MoldIngredient parse(JsonObject json) {
            return new MoldIngredient(GsonHelper.getAsString(json, "shape"), GsonHelper.getAsInt(json, "tools", 1));
        }
        @Override public MoldIngredient parse(FriendlyByteBuf buffer) {
            return new MoldIngredient(buffer.readUtf(), buffer.readVarInt());
        }
        @Override public void write(FriendlyByteBuf buffer, MoldIngredient ingredient) {
            buffer.writeUtf(ingredient.shape);
            buffer.writeVarInt(ingredient.toolsPerRecipe);
        }
    };
}
