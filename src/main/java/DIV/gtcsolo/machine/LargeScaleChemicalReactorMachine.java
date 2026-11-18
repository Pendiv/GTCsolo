package DIV.gtcsolo.machine;

import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import DIV.gtcsolo.util.GtcsoloRecipeModifiers;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public final class LargeScaleChemicalReactorMachine extends WorkableElectricMultiblockMachine {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(
            LargeScaleChemicalReactorMachine.class, MANAGED_FIELD_HOLDER);
    private static final String MODE_BUTTON = "change_processing_mode";
    private static final int BULK_BATCH = 16;
    private static final int BULK_DURATION = 4;
    private static final ModifierFunction BULK = GtcsoloRecipeModifiers.inputOutputBonus(BULK_BATCH)
            .durationMultiplier(BULK_DURATION).build();
    private static final ModifierFunction FAST = ModifierFunction.builder().durationMultiplier(0.25).build();

    @Persisted @DescSynced private boolean shortestProcessing;

    public LargeScaleChemicalReactorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }

    @Override
    public Optional<IParallelHatch> getParallelHatch() {
        return shortestProcessing ? Optional.empty() : super.getParallelHatch();
    }

    public static ModifierFunction processingMode(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof LargeScaleChemicalReactorMachine reactor) || !reactor.isFormed())
            return ModifierFunction.NULL;
        var energy = reactor.energyContainer;
        if (energy == null || energy.getHighestInputVoltage() <= 0) return ModifierFunction.NULL;
        long eut = RecipeHelper.getInputEUt(recipe);
        if (eut > energy.getHighestInputVoltage()) return ModifierFunction.NULL;
        if (reactor.shortestProcessing) return FAST;
        if (recipe.duration > Integer.MAX_VALUE / BULK_DURATION ||
                MegaMixerRecipeModifiers.safeMultiplier(recipe) < BULK_BATCH) return ModifierFunction.NULL;

        int requested = reactor.getParallelHatch().map(IParallelHatch::getCurrentParallel).orElse(1);
        if (requested <= 1) return BULK;
        GTRecipe batch = BULK.apply(recipe);
        if (batch == null) return ModifierFunction.NULL;
        int limit = Math.min(requested, MegaMixerRecipeModifiers.safeMultiplier(batch));
        if (eut > 0) {
            long availableEUt = energy.getInputVoltage() * energy.getInputAmperage();
            limit = (int) Math.min(limit, availableEUt / eut);
        }
        int parallels = ParallelLogic.getParallelAmount(reactor, batch, limit);
        if (parallels <= 0) return ModifierFunction.NULL;
        if (parallels == 1) return BULK;
        return BULK.andThen(GtcsoloRecipeModifiers.inputOutputBonus(parallels)
                .eutMultiplier(parallels).build());
    }

    @Override
    public void addDisplayText(List<Component> lines) {
        super.addDisplayText(lines);
        if (!isFormed()) return;
        var mode = Component.translatable(shortestProcessing
                ? "gtcsolo.large_scale_chemical_reactor.mode.fast" : "gtcsolo.large_scale_chemical_reactor.mode.bulk");
        lines.add(ComponentPanelWidget.withButton(
                Component.translatable("gtcsolo.large_scale_chemical_reactor.mode", mode), MODE_BUTTON));
    }

    @Override
    public void handleDisplayClick(String action, ClickData click) {
        var level = getLevel();
        if (!MODE_BUTTON.equals(action) || click.isRemote || level == null || level.isClientSide() || !isFormed())
            return;
        shortestProcessing = !shortestProcessing;
        recipeLogic.markLastRecipeDirty();
        recipeLogic.updateTickSubscription();
        markDirty();
    }
}
