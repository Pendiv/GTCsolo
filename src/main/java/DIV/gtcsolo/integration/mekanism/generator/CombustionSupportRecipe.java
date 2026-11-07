package DIV.gtcsolo.integration.mekanism.generator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import mekanism.api.MekanismAPI;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Support materials occupy the existing tank opposite the active fuel's phase. */
public final class CombustionSupportRecipe implements Recipe<Container> {
    public enum Kind { GAS, FLUID }
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "gtcsolo");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "gtcsolo");
    public static final RegistryObject<RecipeType<CombustionSupportRecipe>> TYPE = TYPES.register("combustion_support",
            () -> new RecipeType<>() { @Override public String toString() { return "gtcsolo:combustion_support"; } });
    public static final RegistryObject<RecipeSerializer<CombustionSupportRecipe>> SERIALIZER = SERIALIZERS.register("combustion_support", Serializer::new);
    private final ResourceLocation id;
    private final Kind kind;
    private final ResourceLocation material;
    private final long euPerMb;

    public CombustionSupportRecipe(ResourceLocation id, Kind kind, ResourceLocation material, long euPerMb) {
        if (euPerMb <= 0) throw new IllegalArgumentException("eu_per_mb must be positive");
        boolean exists = kind == Kind.GAS ? MekanismAPI.gasRegistry().containsKey(material)
                : ForgeRegistries.FLUIDS.containsKey(material) && !material.equals(new ResourceLocation("minecraft", "empty"));
        if (!exists) throw new IllegalArgumentException("Unknown support material: " + material);
        this.id = id;
        this.kind = kind;
        this.material = material;
        this.euPerMb = euPerMb;
    }

    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
    public Kind kind() { return kind; }
    public ResourceLocation material() { return material; }
    public long euPerMb() { return euPerMb; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeType<?> getType() { return TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public boolean matches(Container inventory, Level level) { return false; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack assemble(Container inventory, RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItem(RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean isIncomplete() { return false; }

    public static final class Serializer implements RecipeSerializer<CombustionSupportRecipe> {
        @Override public CombustionSupportRecipe fromJson(ResourceLocation id, JsonObject data) {
            try {
                if (data.has("gas") == data.has("fluid")) throw new IllegalArgumentException("Specify exactly one of gas or fluid");
                Kind kind = data.has("gas") ? Kind.GAS : Kind.FLUID;
                var material = ResourceLocation.tryParse(GsonHelper.getAsString(data, kind == Kind.GAS ? "gas" : "fluid"));
                if (material == null) throw new IllegalArgumentException("Invalid material ID");
                long amount = new java.math.BigDecimal(GsonHelper.getAsString(data, "eu_per_mb")).longValueExact();
                return new CombustionSupportRecipe(id, kind, material, amount);
            } catch (IllegalArgumentException | ArithmeticException exception) {
                throw new JsonParseException("Invalid combustion support " + id + ": " + exception.getMessage(), exception);
            }
        }
        @Override public CombustionSupportRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new CombustionSupportRecipe(id, buffer.readEnum(Kind.class), buffer.readResourceLocation(), buffer.readVarLong());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, CombustionSupportRecipe recipe) {
            buffer.writeEnum(recipe.kind); buffer.writeResourceLocation(recipe.material); buffer.writeVarLong(recipe.euPerMb);
        }
    }
}
