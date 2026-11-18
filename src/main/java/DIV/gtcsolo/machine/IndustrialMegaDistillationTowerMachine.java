package DIV.gtcsolo.machine;

import DIV.gtcsolo.util.GtcsoloRecipeModifiers;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.DistillationTowerMachine;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fluids.capability.templates.VoidFluidHandler;

import java.util.List;

public final class IndustrialMegaDistillationTowerMachine extends DistillationTowerMachine {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(
            IndustrialMegaDistillationTowerMachine.class, MANAGED_FIELD_HOLDER);
    private static final int MAX_COMPLETIONS = 300;
    private static final int BASE_PARALLELS = 8;

    @Persisted private String recordedRecipe = "";
    @Persisted @DescSynced private int completions;
    @Persisted private int recordedDuration;
    @Persisted private int idleTicks;
    private TickableSubscription decaySubscription;

    public IndustrialMegaDistillationTowerMachine(IMachineBlockEntity holder, int firstOutputOffset) {
        super(holder, firstOutputOffset);
    }

    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }
    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new TowerLogic(this); }
    @Override public boolean alwaysTryModifyRecipe() { return true; }

    @Override public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel && completions > 0) subscribeDecay();
    }

    @Override public void onUnload() {
        stopDecay();
        super.onUnload();
    }

    @Override public GTRecipe fullModifyRecipe(GTRecipe recipe) {
        if (!isFormed() || recipe.recipeType != GTRecipeTypes.DISTILLATION_RECIPES) return null;
        var outputs = getFluidOutputs();
        int fluidCount = recipe.getOutputContents(FluidRecipeCapability.CAP).size();
        // A missing stage becomes a void handler in the native tower.
        if (outputs == null || fluidCount > outputs.size()) return null;
        for (int stage = 0; stage < fluidCount; stage++) {
            if (outputs.get(stage) instanceof VoidFluidHandler) return null;
        }

        int stages = getTier() - RecipeHelper.getRecipeEUtTier(recipe);
        if (stages < 0 || !StoneGeneratorRules.canScale(recipe, Math.pow(2, stages))) return null;
        var overclocked = GtcsoloRecipeModifiers.industrialOverclock(this, recipe).apply(recipe);
        if (overclocked == null) return null;

        int parallel = recordedRecipe.equals(recipe.id.toString()) ? recordedParallelLimit() : BASE_PARALLELS;
        long eut = RecipeHelper.getInputEUt(overclocked);
        if (eut > 0) {
            long availableEUt = energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
            parallel = (int) Math.min(parallel, availableEUt / eut);
        }
        if (parallel < 1) return null;
        // Use the total input amperage above and keep the usual ingredient and output simulations.
        for (var inputs : List.of(overclocked.inputs, overclocked.tickInputs)) {
            for (var capability : inputs.keySet()) {
                if (capability != EURecipeCapability.CAP && capability.doMatchInRecipe()) {
                    parallel = Math.min(parallel, capability.getMaxParallelRatio(this, overclocked, parallel));
                }
            }
        }
        while (parallel > 0 && !StoneGeneratorRules.canScale(overclocked, parallel)) parallel--;
        if (parallel < 1) return null;
        parallel = ParallelLogic.limitByOutputMerging(this, overclocked, parallel, capability -> false);
        if (parallel < 1) return null;
        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallel))
                .eutMultiplier(parallel).parallels(parallel).build().apply(overclocked);
    }

    private int recordedParallelLimit() {
        return BASE_PARALLELS + 4 * (completions / 50);
    }

    private void recordCompletion(GTRecipe recipe, int duration) {
        idleTicks = 0;
        String id = recipe.id.toString();
        if (completions == 0 || recordedRecipe.equals(id)) {
            recordedRecipe = id;
            recordedDuration = Math.max(1, duration);
            completions = Math.min(MAX_COMPLETIONS, completions + 1);
            subscribeDecay();
        } else {
            reduceRecord();
        }
    }

    private void subscribeDecay() {
        decaySubscription = subscribeServerTick(decaySubscription, this::tickDecay);
    }

    private void stopDecay() {
        if (decaySubscription != null) {
            decaySubscription.unsubscribe();
            decaySubscription = null;
        }
    }

    private void tickDecay() {
        if (getRecipeLogic().isWorking()) return;
        if (++idleTicks < Math.max(1, recordedDuration)) return;
        idleTicks = 0;
        reduceRecord();
        // The native logic can reuse a previously modified batch when work resumes.
        if (getRecipeLogic().isIdle()) getRecipeLogic().markLastRecipeDirty();
    }

    private void reduceRecord() {
        if (--completions == 0) {
            recordedRecipe = "";
            recordedDuration = 0;
            stopDecay();
        }
    }

    @Override public void addDisplayText(List<Component> text) {
        super.addDisplayText(text);
        text.add(Component.translatable("gtcsolo.industrial_mega_distillation_tower.completions", completions, MAX_COMPLETIONS));
        text.add(Component.translatable("gtcsolo.industrial_mega_distillation_tower.parallels", recordedParallelLimit()));
    }

    private static final class TowerLogic extends DistillationTowerLogic {
        private TowerLogic(IndustrialMegaDistillationTowerMachine tower) { super(tower); }

        @Override protected boolean handleRecipeIO(GTRecipe recipe, IO io) {
            boolean handled = super.handleRecipeIO(recipe, io);
            if (handled && io == IO.OUT) {
                ((IndustrialMegaDistillationTowerMachine) getMachine()).recordCompletion(recipe, duration);
            }
            return handled;
        }
    }
}
