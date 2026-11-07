package DIV.gtcsolo.integration.mekanism.generator;

import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.gas.attribute.GasAttributes.Fuel;
import mekanism.api.math.FloatingLong;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.ITileComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** The existing opposite-phase tank supplies support material; normal speed remains available. */
public final class GasGeneratorSupportGas implements ITileComponent {
    public static final String SAVE_KEY = "gtcsolo_support_gas";
    private final TileEntityMekanism tile;
    private final IGasTank tank;
    private final IContentsListener listener;
    private FloatingLong credit = FloatingLong.ZERO;
    private FloatingLong creditUnit = FloatingLong.ZERO;
    private @Nullable CombustionSupportRecipe cachedGas;
    private @Nullable CombustionSupportRecipe cachedFluid;
    private long baselineBudget;

    public GasGeneratorSupportGas(TileEntityMekanism tile, IGasTank fuelTank, IContentsListener listener) {
        this.tile = tile;
        this.listener = listener;
        tank = fuelTank;
        tile.addComponent(this);
    }

    public static @Nullable GasGeneratorSupportGas of(TileEntityMekanism tile) {
        return tile instanceof SupportGasGenerator supported ? supported.gtcsolo$getSupportGas() : null;
    }

    private @Nullable CombustionSupportRecipe find(CombustionSupportRecipe.Kind kind, ResourceLocation material) {
        var level = tile.getLevel();
        if (level == null || material == null) return null;
        var manager = level.getRecipeManager();
        var cached = kind == CombustionSupportRecipe.Kind.GAS ? cachedGas : cachedFluid;
        if (cached != null && cached.material().equals(material) && manager.byKey(cached.getId()).orElse(null) == cached) return cached;
        var found = manager.getAllRecipesFor(CombustionSupportRecipe.TYPE.get()).stream()
                .filter(recipe -> recipe.kind() == kind && recipe.material().equals(material))
                .min(java.util.Comparator.comparing(CombustionSupportRecipe::getId)).orElse(null);
        if (kind == CombustionSupportRecipe.Kind.GAS) cachedGas = found; else cachedFluid = found;
        return found;
    }

    public boolean acceptsGas(Gas gas) { return find(CombustionSupportRecipe.Kind.GAS, gas.getRegistryName()) != null; }
    public boolean acceptsFluid(Fluid fluid) { return find(CombustionSupportRecipe.Kind.FLUID, ForgeRegistries.FLUIDS.getKey(fluid)) != null; }
    public boolean hasGasFuel() { return !tank.isEmpty() && tank.getType().has(Fuel.class) && !acceptsGas(tank.getType()); }

    private @Nullable Supply supply() {
        if (!tank.isEmpty()) {
            var definition = find(CombustionSupportRecipe.Kind.GAS, tank.getType().getRegistryName());
            if (definition != null) return new Supply(definition, tank.getStored(), true);
        }
        var liquids = tile.getFluidTanks(null);
        if (!liquids.isEmpty() && !liquids.get(0).isEmpty()) {
            var fluid = liquids.get(0);
            var definition = find(CombustionSupportRecipe.Kind.FLUID, ForgeRegistries.FLUIDS.getKey(fluid.getFluid().getFluid()));
            if (definition != null) return new Supply(definition, fluid.getFluidAmount(), false);
        }
        return null;
    }

    private FloatingLong unitEnergy(Supply supply) {
        return MekanismConfig.general.ic2ConversionRate.get().multiply(supply.definition.euPerMb());
    }

    private FloatingLong availableEnergy() {
        var source = supply();
        return source == null ? credit : unitEnergy(source).multiply(source.amount).plusEqual(credit);
    }

    public long limit(long baseline, long requested, FloatingLong rate) {
        baselineBudget = baseline;
        if (requested <= baseline || rate.isZero()) return baseline;
        long supported = availableEnergy().divide(rate).longValue();
        return supported <= baseline ? baseline : Math.min(requested, supported);
    }

    /** Charge only a productive boosted tick, at its total generated energy. Returns a safe fallback budget. */
    public long consume(long used, FloatingLong rate) {
        if (used <= baselineBudget || rate.isZero()) return used;
        FloatingLong cost = rate.multiply(used);
        if (availableEnergy().smallerThan(cost)) return Math.min(used, baselineBudget);
        FloatingLong needed = cost.subtract(credit);
        if (needed.isZero()) {
            credit = credit.subtract(cost).copyAsConst();
        } else {
            var source = supply();
            if (source == null) return Math.min(used, baselineBudget);
            FloatingLong unit = unitEnergy(source);
            if (unit.isZero()) return Math.min(used, baselineBudget);
            long amount = needed.divide(unit).longValue();
            if (unit.multiply(amount).smallerThan(needed)) amount++;
            if (amount > source.amount) return Math.min(used, baselineBudget);
            if (source.gas) tank.shrinkStack(amount, Action.EXECUTE);
            else tile.getFluidTanks(null).get(0).shrinkStack((int) amount, Action.EXECUTE);
            credit = unit.multiply(amount).plusEqual(credit).subtract(cost).copyAsConst();
            creditUnit = unit.copyAsConst();
        }
        listener.onContentsChanged();
        return used;
    }

    @Override public void read(CompoundTag data) {
        credit = FloatingLong.ZERO;
        creditUnit = FloatingLong.ZERO;
        var savedData = data.getCompound(SAVE_KEY);
        String saved = savedData.getString("credit_joules");
        if (saved.isEmpty()) return;
        try {
            FloatingLong parsed = FloatingLong.parseFloatingLong(saved, true);
            FloatingLong unit = FloatingLong.parseFloatingLong(savedData.getString("credit_unit"), true);
            if (!unit.isZero() && parsed.smallerThan(unit)) { credit = parsed; creditUnit = unit; }
        } catch (IllegalArgumentException ignored) {
            // Discard invalid fractional credit; the native tank restores its own contents separately.
        }
    }

    @Override public void write(CompoundTag data) {
        if (credit.isZero()) { data.remove(SAVE_KEY); return; }
        CompoundTag saved = new CompoundTag();
        saved.putString("credit_joules", credit.toString());
        saved.putString("credit_unit", creditUnit.toString());
        data.put(SAVE_KEY, saved);
    }

    private record Supply(CombustionSupportRecipe definition, long amount, boolean gas) {}
}
