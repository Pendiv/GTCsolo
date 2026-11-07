package DIV.gtcsolo.integration.mekanism.generator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import mekanism.api.chemical.gas.attribute.GasAttributes;
import mekanism.api.math.FloatingLong;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Reloadable fuel definitions. Energy density uses Mekanism's native J per mB. */
public final class LiquidFuelRecipe implements Recipe<Container> {
    private static final String NAME = "gas_generator_liquid_fuel";
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "gtcsolo");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "gtcsolo");
    public static final RegistryObject<RecipeType<LiquidFuelRecipe>> TYPE = TYPES.register(NAME,
            () -> new RecipeType<>() { @Override public String toString() { return "gtcsolo:" + NAME; } });
    public static final RegistryObject<RecipeSerializer<LiquidFuelRecipe>> SERIALIZER =
            SERIALIZERS.register(NAME, Serializer::new);

    private final ResourceLocation id;
    private final Fluid fluid;
    private final int burnTime;
    private final FloatingLong energyDensity;
    private final FloatingLong energyPerTick;

    public LiquidFuelRecipe(ResourceLocation id, Fluid fluid, int burnTime, FloatingLong energyDensity) {
        if (fluid == Fluids.EMPTY || burnTime < 1 || energyDensity.isZero()) {
            throw new IllegalArgumentException("Liquid fuel needs a fluid, positive burn_time and positive energy_density");
        }
        this.id = id;
        this.fluid = fluid;
        this.burnTime = burnTime;
        this.energyDensity = energyDensity.copyAsConst();
        this.energyPerTick = new GasAttributes.Fuel(burnTime, this.energyDensity).getEnergyPerTick().copyAsConst();
        if (energyPerTick.isZero()) throw new IllegalArgumentException("Liquid fuel energy per burn tick rounds to zero");
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    public Fluid fluid() { return fluid; }
    public int burnTime() { return burnTime; }
    public FloatingLong energyDensity() { return energyDensity; }
    public FloatingLong energyPerTick() { return energyPerTick; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeType<?> getType() { return TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public boolean matches(Container inventory, Level level) { return false; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack assemble(Container inventory, RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItem(RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean isIncomplete() { return false; }

    private static Fluid readFluid(ResourceLocation id) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null || fluid == Fluids.EMPTY) throw new IllegalArgumentException("Unknown liquid fuel: " + id);
        return fluid;
    }

    public static final class Serializer implements RecipeSerializer<LiquidFuelRecipe> {
        @Override public LiquidFuelRecipe fromJson(ResourceLocation id, JsonObject data) {
            try {
                String density = GsonHelper.getAsString(data, "energy_density");
                // FloatingLong otherwise truncates extra decimals and parses unsigned values.
                if (!density.matches("[0-9]+(?:\\.[0-9]{1,4})?")) {
                    throw new IllegalArgumentException("energy_density must be a decimal J/mB value with at most four decimal places");
                }
                var fluidId = ResourceLocation.tryParse(GsonHelper.getAsString(data, "fluid"));
                if (fluidId == null) throw new IllegalArgumentException("fluid must be a valid registry ID");
                return new LiquidFuelRecipe(id, readFluid(fluidId),
                        Integer.parseInt(GsonHelper.getAsString(data, "burn_time")),
                        FloatingLong.parseFloatingLong(density, true));
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Invalid liquid fuel " + id + ": " + exception.getMessage(), exception);
            }
        }

        @Override public LiquidFuelRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new LiquidFuelRecipe(id, readFluid(buffer.readResourceLocation()), buffer.readVarInt(),
                    FloatingLong.parseFloatingLong(buffer.readUtf(), true));
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, LiquidFuelRecipe recipe) {
            buffer.writeResourceLocation(ForgeRegistries.FLUIDS.getKey(recipe.fluid));
            buffer.writeVarInt(recipe.burnTime);
            buffer.writeUtf(recipe.energyDensity.toString());
        }
    }
}
