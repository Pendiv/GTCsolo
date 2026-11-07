package DIV.gtcsolo.machine;

import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import DIV.gtcsolo.registry.BiochemicalMachines;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.utils.GTUtil;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class BiochemicalProcessMachine extends WorkableElectricMultiblockMachine {
    private BlockPattern controllerPattern;
    private int sections;

    public BiochemicalProcessMachine(IMachineBlockEntity holder) { super(holder); }

    @Override public BlockPattern getPattern() {
        if (controllerPattern == null) controllerPattern = BiochemicalMachines.pattern(getDefinition());
        return controllerPattern;
    }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        sections = getPattern().getFormedRepetitionCount()[1];
    }

    @Override public void onStructureInvalid() {
        sections = 0;
        super.onStructureInvalid();
    }

    public int sections() { return sections; }

    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new ProcessLogic(this); }

    public static ModifierFunction modify(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof BiochemicalProcessMachine process) || !process.isFormed()) return ModifierFunction.NULL;
        return source -> process.modifyProcess(source);
    }

    private GTRecipe modifyProcess(GTRecipe source) {
        int load = source.data.getInt(BiochemicalProcessRules.LOAD);
        int minimum = source.data.getInt(BiochemicalProcessRules.MINIMUM_TICKS);
        int capacity = BiochemicalProcessRules.limit(sections, load);
        long baseEU = RecipeHelper.getInputEUt(source), voltage = getOverclockVoltage();
        if (capacity < 1 || baseEU <= 0 || baseEU > voltage || minimum < 1 || minimum > source.duration) return null;
        int requested = getParallelHatch().map(h -> Math.max(1, h.getCurrentParallel())).orElse(1);
        int maxStages = Math.max(0, GTUtil.getOCTierByVoltage(voltage) - GTUtil.getTierByVoltage(baseEU));
        GTRecipe best = null;
        double bestRate = 0;
        long eut = baseEU;
        for (int stages = 0; stages <= maxStages && eut <= voltage; stages++) {
            int duration = BiochemicalProcessRules.duration(source.duration, minimum, stages);
            int limit = (int) Math.min(Math.min(requested, capacity), voltage / eut);
            limit = Math.min(limit, MegaMixerRecipeModifiers.safeMultiplier(source));
            GTRecipe capacityRecipe = source.copy();
            capacityRecipe.tickInputs.remove(EURecipeCapability.CAP);
            int parallel = MegaMixerRecipeModifiers.availableParallels(this, capacityRecipe, limit);
            double rate = parallel / (double) duration;
            if (parallel > 0 && rate > bestRate) {
                GTRecipe result = ModifierFunction.builder().modifyAllContents(ContentModifier.multiplier(parallel))
                        .eutMultiplier((double) eut / baseEU * parallel).parallels(parallel).addOCs(stages).build().apply(source);
                result.duration = duration;
                result.data = source.data.copy();
                result.data.putInt(BiochemicalProcessRules.LOAD, Math.multiplyExact(load, parallel));
                best = result;
                bestRate = rate;
            }
            if (duration == minimum || eut > Long.MAX_VALUE / 4) break;
            eut *= 4;
        }
        return best;
    }

    @Override public void addDisplayText(List<Component> lines) {
        super.addDisplayText(lines);
        if (isFormed()) lines.add(getRecipeType() == BiochemicalMachines.MEMBRANE_SEPARATION
                ? Component.translatable("gtcsolo.biochemical.membrane_area", sections)
                : Component.translatable("gtcsolo.biochemical.capacity", sections, BiochemicalProcessRules.capacity(sections)));
    }

    public static final class ProcessLogic extends RecipeLogic {
        private long lastTick = Long.MIN_VALUE;
        public ProcessLogic(BiochemicalProcessMachine machine) { super(machine); }
        @Override public void serverTick() {
            var level = getMachine().getLevel();
            if (level == null || level.isClientSide()) return;
            long now = level.getGameTime();
            if (lastTick == now) return;
            lastTick = now;
            super.serverTick();
        }
    }
}
