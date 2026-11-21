package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.MachineVoltage;
import DIV.gtcsolo.integration.mekanism.RecipePower;
import DIV.gtcsolo.integration.mekanism.RecipeVoltage;
import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToItemStackCachedRecipe.ChemicalUsageMultiplier;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import mekanism.common.recipe.lookup.monitor.RecipeCacheLookupMonitor;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.tile.factory.TileEntityItemStackGasToItemStackFactory;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.util.MekanismUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecipeCacheLookupMonitor.class, remap = false)
public abstract class RecipeMachineVoltageMixin {
    @Shadow @Final private IRecipeLookupHandler<?> handler;

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/MekanismRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"))
    private void gtcsolo$applyVoltageCondition(MekanismRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<?>> result) {
        CachedRecipe<?> cached = result.getReturnValue();
        if (cached == null || !(handler instanceof TileEntityMekanism machine)) return;
        RecipePower power = ((RecipeVoltage) recipe).gtcsolo$getPower();
        CachedRecipeConditionsAccess access = (CachedRecipeConditionsAccess) cached;
        boolean factory = machine instanceof TileEntityFactory<?>;
        if (power == null && !factory) return;
        long rating = MachineVoltage.rating(machine);
        BooleanSupplier existing = access.gtcsolo$getHolderCondition();
        cached.setCanHolderFunction(() -> (!factory || index < SlotUpgrade.processes((TileEntityFactory<?>) machine))
                && (power == null || power.voltage() <= rating) && existing.getAsBoolean());
        if (power == null || power.voltage() > rating) return;

        IntSupplier nativeTicks = access.gtcsolo$getRequiredTicks();
        IntSupplier baseTicks = power.duration() == 0 ? nativeTicks : () -> MekanismUtils.getTicks(machine, power.duration());
        IntSupplier effectiveTicks = () -> {
            int ticks = baseTicks.getAsInt();
            return power.operatingTicks(ticks, power.overclocks(rating, ticks));
        };
        cached.setRequiredTicks(effectiveTicks);
        if (cached instanceof CachedChemicalUsageAccess chemical
                && (machine instanceof TileEntityItemStackGasToItemStackFactory || machine instanceof TileEntityAdvancedElectricMachine)) {
            // Mekanism distributes remaining gas over (machine ticks - elapsed ticks).
            // Each parallel recipe now has its own duration. Keep its last tick responsible for the remaining gas.
            ChemicalUsageMultiplier usage = chemical.gtcsolo$getChemicalUsage();
            chemical.gtcsolo$setChemicalUsage((used, elapsed) -> usage.getToUse(used,
                    nativeTicks.getAsInt() - Math.max(1, effectiveTicks.getAsInt() - elapsed)));
        }
        access.gtcsolo$setPerTickEnergy(() -> {
            int stages = power.overclocks(rating, baseTicks.getAsInt());
            return MekanismUtils.getEnergyPerTick(machine, MachineVoltage.joules(power.voltage(), power.amperage()))
                    .multiply(1L << (2 * stages));
        });
    }
}
