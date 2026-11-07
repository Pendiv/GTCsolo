package DIV.gtcsolo.machine;

import DIV.gtcsolo.util.GtcsoloRecipeModifiers;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.recipe.condition.RockBreakerCondition;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** Standard rock-breaking recipes with tank-based surroundings and an animated production channel. */
public class IndustrialRockBreakerMachine extends WorkableElectricMultiblockMachine {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(
            IndustrialRockBreakerMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);
    private List<NotifiableFluidTank> environmentTanks = List.of();
    private GTRecipe displayRecipe;
    @DescSynced private FluidStack displayFluidA = FluidStack.EMPTY;
    @DescSynced private FluidStack displayFluidB = FluidStack.EMPTY;
    @DescSynced private ItemStack displayOutput = ItemStack.EMPTY;

    public IndustrialRockBreakerMachine(IMachineBlockEntity holder) { super(holder); }
    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }
    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new StoneLogic(this); }
    public FluidStack getDisplayFluidA() { return displayFluidA; }
    public FluidStack getDisplayFluidB() { return displayFluidB; }
    public ItemStack getDisplayOutput() { return displayOutput; }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        environmentTanks = getParts().stream().map(part -> part.self())
                .filter(part -> part.getDefinition() == GTMachines.FLUID_IMPORT_HATCH[GTValues.ULV])
                .map(FluidHatchPartMachine.class::cast).map(part -> part.tank).toList();
    }

    @Override public void onStructureInvalid() {
        environmentTanks = List.of();
        displayFluidA = displayFluidB = FluidStack.EMPTY;
        displayOutput = ItemStack.EMPTY;
        displayRecipe = null;
        super.onStructureInvalid();
    }

    @Override public void onPartUnload() {
        environmentTanks = List.of();
        super.onPartUnload();
    }

    protected int environmentParallels(GTRecipe recipe) {
        var environment = StoneGeneratorRules.environment(recipe);
        return environment == null ? 0 : environment.parallels(environmentTanks);
    }

    protected boolean hasEnvironment(GTRecipe recipe) {
        return isFormed() && recipe != null && environmentParallels(recipe)
                >= Math.max(1, recipe.data.getInt(StoneGeneratorRules.PARALLELS));
    }

    @Override public GTRecipe fullModifyRecipe(GTRecipe recipe) {
        if (!isFormed() || recipe == null || recipe.recipeType != getRecipeType()) return null;
        int available = environmentParallels(recipe);
        if (available == 0 || recipe.conditions.stream().anyMatch(c -> c instanceof RockBreakerCondition && c.isReverse()))
            return null;
        int stages = getTier() - RecipeHelper.getRecipeEUtTier(recipe);
        if (stages < 0 || !StoneGeneratorRules.canScale(recipe, Math.pow(2, stages))) return null;
        var detached = recipe.copy();
        detached.data = recipe.data.copy();
        // The original condition checks only the four blocks immediately beside the controller.
        // Keep all other conditions. The runtime logic validates the same fluids in the two tanks.
        detached.conditions.removeIf(RockBreakerCondition.class::isInstance);
        var base = super.fullModifyRecipe(detached);
        if (base == null) return null;
        var industrial = GtcsoloRecipeModifiers.industrialOverclock(this, base).apply(base);
        if (industrial == null) return null;
        int parallels = available;
        // EU/t is multiplied below. Do not apply GT's one-voltage parallel cap to this tank bonus.
        for (var inputs : List.of(industrial.inputs, industrial.tickInputs)) for (var cap : inputs.keySet()) {
            if (cap != EURecipeCapability.CAP && cap.doMatchInRecipe())
                parallels = Math.min(parallels, cap.getMaxParallelRatio(this, industrial, parallels));
        }
        if (parallels < 1) return null;
        parallels = ParallelLogic.limitByOutputMerging(this, industrial, parallels, this::canVoidRecipeOutputs);
        if (parallels < 1 || !StoneGeneratorRules.canScale(industrial, parallels)
                || RecipeHelper.getInputEUt(industrial) > Long.MAX_VALUE / parallels) return null;
        var result = ModifierFunction.builder().modifyAllContents(ContentModifier.multiplier(parallels))
                .eutMultiplier(parallels).parallels(parallels).build().apply(industrial);
        result.data.putInt(StoneGeneratorRules.PARALLELS, parallels);
        return result;
    }

    @Override public boolean beforeWorking(GTRecipe recipe) {
        if (!hasEnvironment(recipe) || !super.beforeWorking(recipe)) return false;
        updateDisplay(recipe);
        return true;
    }

    @Override public boolean onWorking() {
        // Rehydrate transient visual fields when a saved, active recipe resumes.
        updateDisplay(getRecipeLogic().getLastRecipe());
        return super.onWorking();
    }

    private void updateDisplay(GTRecipe recipe) {
        if (recipe == displayRecipe) return;
        var environment = recipe == null ? null : StoneGeneratorRules.environment(recipe);
        if (environment == null) return;
        displayRecipe = recipe;
        if (displayFluidA.isEmpty() || displayFluidA.getFluid() != environment.first())
            displayFluidA = new FluidStack(environment.first(), 1000);
        if (displayFluidB.isEmpty() || displayFluidB.getFluid() != environment.second())
            displayFluidB = new FluidStack(environment.second(), 1000);
        ItemStack output = ItemStack.EMPTY;
        for (var content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            for (var stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                if (stack.getItem() instanceof BlockItem) { output = stack.copyWithCount(1); break; }
            }
            if (!output.isEmpty()) break;
        }
        if (!ItemStack.isSameItemSameTags(displayOutput, output)) displayOutput = output;
    }

    private static final class StoneLogic extends RecipeLogic {
        private final IndustrialRockBreakerMachine breaker;
        private StoneLogic(IndustrialRockBreakerMachine machine) { super(machine); breaker = machine; }

        @Override public void handleRecipeWorking() {
            if (!breaker.hasEnvironment(getLastRecipe())) {
                setWaiting(Component.translatable("gtcsolo.rock_breaker.environment_missing"));
                doDamping();
                return;
            }
            super.handleRecipeWorking();
        }
    }
}
