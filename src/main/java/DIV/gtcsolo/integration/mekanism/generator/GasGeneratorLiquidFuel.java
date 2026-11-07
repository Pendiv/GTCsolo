package DIV.gtcsolo.integration.mekanism.generator;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.fluid.VariableCapacityFluidTank;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.ITileComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** Native-style fuel budgeting with an independent, persistent fractional liquid reserve. */
public final class GasGeneratorLiquidFuel implements ITileComponent {
    public static final String SAVE_KEY = "gtcsolo_liquid_fuel";
    private final TileEntityMekanism tile;
    private final IContentsListener listener;
    private final IExtendedFluidTank tank;
    private @Nullable LiquidFuelRecipe cachedRecipe;
    private @Nullable LiquidFuelRecipe remainderFuel;
    private int remainderTicks;

    public GasGeneratorLiquidFuel(TileEntityMekanism tile, IContentsListener listener) {
        this.tile = tile;
        this.listener = listener;
        // Forge's fluid API uses int amounts, unlike Mekanism's long-valued gas API.
        tank = VariableCapacityFluidTank.input(() -> (int) Math.min(Integer.MAX_VALUE, tile.getGasTanks(null).get(0).getCapacity()),
                stack -> findFuel(stack.getFluid()) != null || isSupport(stack.getFluid()), listener);
        tile.addComponent(this);
    }

    public IExtendedFluidTank tank() { return tank; }
    public boolean hasFuel() { return remainderTicks > 0 || !tank.isEmpty() && findFuel(tank.getFluid().getFluid()) != null; }

    private boolean isSupport(Fluid fluid) {
        var support = GasGeneratorSupportGas.of(tile);
        return support != null && support.acceptsFluid(fluid);
    }

    private @Nullable LiquidFuelRecipe findFuel(Fluid fluid) {
        var level = tile.getLevel();
        if (level == null || isSupport(fluid)) return null;
        var manager = level.getRecipeManager();
        // byKey also detects replacements/removal on /reload without a global lifecycle cache.
        if (cachedRecipe != null && cachedRecipe.fluid() == fluid
                && manager.byKey(cachedRecipe.getId()).orElse(null) == cachedRecipe) return cachedRecipe;
        cachedRecipe = manager.getAllRecipesFor(LiquidFuelRecipe.TYPE.get()).stream()
                .filter(recipe -> recipe.fluid() == fluid)
                .min(java.util.Comparator.comparing(LiquidFuelRecipe::getId)).orElse(null);
        return cachedRecipe;
    }

    public BurnResult burn(BasicEnergyContainer energy) {
        var rotor = GasGeneratorRotor.of(tile);
        if (rotor == null || !rotor.isReady()) return BurnResult.IDLE;
        LiquidFuelRecipe current = tank.isEmpty() ? null : findFuel(tank.getFluid().getFluid());
        LiquidFuelRecipe fuel = remainderTicks > 0 ? remainderFuel : current;
        if (fuel == null) return BurnResult.IDLE;
        FloatingLong rate = rotor.generationRate(fuel.energyPerTick());
        if (rate.isZero()) return BurnResult.IDLE;
        boolean sameFuel = current != null && current.fluid() == fuel.fluid()
                && current.burnTime() == fuel.burnTime() && current.energyDensity().equals(fuel.energyDensity());
        int availableFluid = sameFuel ? tank.getFluidAmount() : 0;
        long availableTicks = (long) availableFluid * fuel.burnTime() + remainderTicks;
        long limit = (long) GasGeneratorUpgrades.burnLimit(tile,
                256 * (Math.max(1, availableFluid) / (double) tank.getCapacity()), rate);
        long used = Math.min(availableTicks, Math.min(limit, energy.getNeeded().divide(rate).intValue()));
        if (used <= 0) return BurnResult.IDLE;
        FloatingLong generated = rate.multiply(used);
        if (!energy.insert(generated, Action.SIMULATE, AutomationType.INTERNAL).isZero()) return BurnResult.IDLE;
        var support = GasGeneratorSupportGas.of(tile);
        if (support != null) used = support.consume(used, rate);
        generated = rate.multiply(used);
        energy.insert(generated, Action.EXECUTE, AutomationType.INTERNAL);
        long remaining = availableTicks - used;
        if (sameFuel) tank.setStackSize((int) (remaining / fuel.burnTime()), Action.EXECUTE);
        remainderTicks = (int) (remaining % fuel.burnTime());
        remainderFuel = remainderTicks == 0 ? null : fuel;
        listener.onContentsChanged();
        rotor.onBurn();
        return new BurnResult(rate, fuel.burnTime(), used, generated);
    }

    @Override public void read(CompoundTag data) {
        remainderTicks = 0;
        remainderFuel = null;
        CompoundTag saved = data.getCompound(SAVE_KEY);
        if (!saved.contains("fluid")) return;
        try {
            var id = ResourceLocation.tryParse(saved.getString("fluid"));
            if (id == null) return;
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
            if (fluid == null) return;
            var fuel = new LiquidFuelRecipe(id, fluid, saved.getInt("burn_time"),
                    FloatingLong.parseFloatingLong(saved.getString("energy_density"), true));
            int ticks = saved.getInt("burn_ticks");
            if (ticks > 0 && ticks < fuel.burnTime()) {
                remainderFuel = fuel;
                remainderTicks = ticks;
            }
        } catch (IllegalArgumentException ignored) {
            // A removed fluid or malformed old reserve cannot be burned; the actual tank is persisted by Mekanism.
        }
    }

    @Override public void write(CompoundTag data) {
        if (remainderFuel == null || remainderTicks == 0) { data.remove(SAVE_KEY); return; }
        CompoundTag saved = new CompoundTag();
        saved.putString("fluid", ForgeRegistries.FLUIDS.getKey(remainderFuel.fluid()).toString());
        saved.putInt("burn_time", remainderFuel.burnTime());
        saved.putString("energy_density", remainderFuel.energyDensity().toString());
        saved.putInt("burn_ticks", remainderTicks);
        data.put(SAVE_KEY, saved);
    }

    public record BurnResult(FloatingLong rate, int burnTime, long usedTicks, FloatingLong generated) {
        public static final BurnResult IDLE = new BurnResult(FloatingLong.ZERO, 0, 0, FloatingLong.ZERO);
        public double usedFluid() { return burnTime == 0 ? 0 : usedTicks / (double) burnTime; }
    }
}
